package chiselv

import chisel3._
import chisel3.util._

/* Memory Map
 *
 * 0x0000_0000 - 0x0000_00FF: Reserved
 * 0x0000_0100 - 0x0000_0FFF: Debug
 * 0x0000_1000 - 0x0000_1FFF: Syscon
 * 0x0000_2000 - 0x0000_FFFF: Reserved
 * 0x0003_0000 - 0x0003_FFFF: ROM (64KB)
 * 0x0001_0000 - 0x2FFF_FFFF: Reserved
 * 0x3000_0000 - 0x3000_0FFF: UART0
 *                 0x00 (TX Write)
 *                 0x04 (RX Read)
 *                 0x0C (Status Read) [txFull|rxFull|txEmpty|rxEmpty]
 *                 0x10 (Clock Divisor Read/Write)
 * 0x3000_1000 - 0x3000_1FFF: GPIO0
 *                 0x00 (direction - 0: input, 1: output)
 *                 0x04 (value     - 1: high, 0: low)
 * 0x3000_2000 - 0x3000_2FFF: PWM0
 * 0x3000_3000 - 0x3000_3FFF: Timer0
 *                 0x00 (32 bit value in miliseconds)
 * 0x3000_4000 - 0x3FFF_FFFF: Reserved
 * 0x4000_0000 - 0x4FFF_FFFF: Reserved
 * 0x5000_0000 - 0x7000_0000: Reserved
 * 0x8000_0000 - 0x8FFF_FFFF: On-chip memory RAM
 * 0x9000_0000 - 0x9FFF_FFFF: Reserved
 */

class MemoryIOManager(bitWidth: Int = 32, clockFreq: Long, sizeBytes: Long = 1024) extends Module {
  val io = IO(new Bundle {
    val MemoryIOPort = new MemoryPortDual(bitWidth, scala.math.pow(2, bitWidth).toLong)
    val GPIO0Port    = Flipped(new GPIOPort(bitWidth))
    val Timer0Port   = Flipped(new TimerPort(bitWidth))
  })

  val dataOut      = WireDefault(0.U(bitWidth.W))
  val readAddress  = io.MemoryIOPort.readAddr
  val writeAddress = io.MemoryIOPort.writeAddr

  // Initialize IO
  io.GPIO0Port.dataIn         := DontCare
  io.GPIO0Port.writeValue     := false.B
  io.GPIO0Port.writeDirection := false.B

  io.Timer0Port.dataIn      := DontCare
  io.Timer0Port.writeEnable := false.B

  /* --- Syscon --- */
  when(readAddress(31, 12) === 0x0000_1L.U) {
    // Dummy output - (0x0000_1000)
    when(readAddress(11, 0) === 0x0L.U)(dataOut := 0xbaad_cafeL.U)
    // Clock frequency - (0x0000_1008)
    when(readAddress(11, 0) === 0x8L.U)(dataOut := clockFreq.asUInt(bitWidth.W))
    // Has UART0 - (0x0000_1010)
    when(readAddress(11, 0) === 0x10L.U)(dataOut := 0.U)
    // Has GPIO0 - (0x0000_1018)
    when(readAddress(11, 0) === 0x18L.U)(dataOut := 1.U)
    // Has PWM0 - (0x0000_1020)
    when(readAddress(11, 0) === 0x20L.U)(dataOut := 0.U)
    // Has Timer0 - (0x0000_1024)
    when(readAddress(11, 0) === 0x24L.U)(dataOut := 1.U)
  }

  /* --- UART0 --- */
  when(readAddress(31, 12) === 0x3000_0L.U || writeAddress(31, 12) === 0x3000_0L.U) {
    dataOut := 0.U
  }

  // GPIO0
  when(readAddress(31, 12) === 0x3000_1L.U || writeAddress(31, 12) === 0x3000_1L.U) {
    // Reads
    when(readAddress(7, 0) === 0x00.U) { // Direction
      dataOut := io.GPIO0Port.directionOut
    }
      .elsewhen(readAddress(7, 0) === 0x04.U) { // Value
        dataOut := io.GPIO0Port.valueOut
      }
      .otherwise(dataOut := 0.U)

    // Writes
    when(writeAddress(7, 0) === 0x00.U) { // Direction
      io.GPIO0Port.writeDirection := io.MemoryIOPort.writeEnable
    }
      .elsewhen(writeAddress(7, 0) === 0x04.U) { // Value
        io.GPIO0Port.writeValue := io.MemoryIOPort.writeEnable
      }
    io.GPIO0Port.dataIn := io.MemoryIOPort.writeData
  }

  /* --- PWM0 --- */
  when(readAddress(31, 12) === 0x3000_2L.U || writeAddress(31, 12) === 0x3000_2L.U) {
    dataOut := 0.U
  }

  /* --- Timer0 --- */
  when(readAddress(31, 12) === 0x3000_3L.U || writeAddress(31, 12) === 0x3000_3L.U) {
    io.Timer0Port.dataIn      := io.MemoryIOPort.writeData
    io.Timer0Port.writeEnable := io.MemoryIOPort.writeEnable
    dataOut                   := io.Timer0Port.dataOut
  }

  /* --- Data Memory --- */
  val memory = Module(new DualPortRAM(bitWidth, sizeBytes))

  // Initialize IO
  memory.io.dualPort.writeEnable := false.B
  memory.io.dualPort.writeData   := DontCare
  memory.io.dualPort.readAddr    := DontCare
  memory.io.dualPort.writeAddr   := DontCare
  memory.io.dualPort.dataSize    := DontCare
  memory.io.dualPort.writeMask   := io.MemoryIOPort.writeMask

  // Remove address offset
  memory.io.dualPort.readAddr  := Cat(Fill(16, 0.U), readAddress(15, 0))
  memory.io.dualPort.writeAddr := Cat(Fill(16, 0.U), readAddress(15, 0))

  when(readAddress(31, 28) === 0x8.U || writeAddress(31, 28) === 0x8.U) {
    // Set the data write mask and the data position within the word
    val writeMask   = WireDefault(0.U(4.W))
    val dataToWrite = WireDefault(0.U(bitWidth.W))

    when(io.MemoryIOPort.writeEnable) {
      switch(io.MemoryIOPort.dataSize) {
        is(3.U) { // Write word
          dataToWrite := io.MemoryIOPort.writeData
          writeMask   := "b1111".U
        }
        is(2.U) { // Write halfword
          switch(memory.io.dualPort.writeAddr(1).asUInt()) {
            is(1.U) { // Write half word 1
              dataToWrite := Cat(io.MemoryIOPort.writeData(15, 0).asUInt, Fill(16, 0.U))
              writeMask   := "b1100".U
            }
            is(0.U) { // Write half word 0
              dataToWrite := Cat(Fill(16, 0.U), io.MemoryIOPort.writeData(15, 0).asUInt)
              writeMask   := "b0011".U
            }
          }
        }
        is(1.U) { // Write byte
          switch(memory.io.dualPort.writeAddr(1, 0)) {
            is(3.U) { // Write byte 3
              dataToWrite := Cat(io.MemoryIOPort.writeData(7, 0).asUInt, Fill(24, 0.U))
              writeMask   := "b1000".U
            }
            is(2.U) { // Write byte 2
              dataToWrite := Cat(Fill(8, 0.U), io.MemoryIOPort.writeData(7, 0).asUInt, Fill(16, 0.U))
              writeMask   := "b0100".U
            }
            is(1.U) { // Write byte 2
              dataToWrite := Cat(Fill(16, 0.U), io.MemoryIOPort.writeData(7, 0).asUInt, Fill(8, 0.U))
              writeMask   := "b0010".U
            }
            is(0.U) { // Write byte 0
              dataToWrite := Cat(Fill(24, 0.U), io.MemoryIOPort.writeData(7, 0).asUInt)
              writeMask   := "b0001".U
            }
          }
        }
      }
      // Read modify write the data
      val dataIn = Cat(
        Mux(writeMask(3), dataToWrite(3 * 8 + 7, 3 * 8), io.MemoryIOPort.readData(3 * 8 + 7, 3 * 8)),
        Mux(writeMask(2), dataToWrite(2 * 8 + 7, 2 * 8), io.MemoryIOPort.readData(2 * 8 + 7, 2 * 8)),
        Mux(writeMask(1), dataToWrite(1 * 8 + 7, 1 * 8), io.MemoryIOPort.readData(1 * 8 + 7, 1 * 8)),
        Mux(writeMask(0), dataToWrite(0 * 8 + 7, 0 * 8), io.MemoryIOPort.readData(0 * 8 + 7, 0 * 8)),
      )

      memory.io.dualPort.writeEnable := io.MemoryIOPort.writeEnable
      memory.io.dualPort.writeMask   := writeMask
      memory.io.dualPort.writeData   := dataIn
    }
    dataOut := memory.io.dualPort.readData
  }
  io.MemoryIOPort.readData := dataOut
}
