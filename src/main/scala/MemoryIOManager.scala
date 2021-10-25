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
 * 0x3000_1000 - 0x3000_1FFF: GPIO0
 *                 0x00 (direction - 1: input, 0: output)
 *                 0x04 (value     - 1: high, 0: low)
 * 0x3000_2000 - 0x3000_2FFF: PWM0
 * 0x3000_3000 - 0x3000_3FFF: Timer0
 *                 0x00 (32 bit value in miliseconds)
 * 0x3000_4000 - 0x3FFF_FFFF: Reserved
 * 0x4000_0000 - 0x4FFF_FFFF: Reserved
 * 0x5000_0000 - 0x7000_0000: Reserved
 * 0x8000_0000 - 0x8000_FFFF: On-chip memory RAM (64KB)
 * 0x8001_0000 - 0xFFFF_FFFF: Reserved
 */

class MemoryIOManager(bitWidth: Int = 32, clockFreq: Long, sizeBytes: Long = 1024) extends Module {
  val io = IO(new Bundle {
    val MemoryIOPort = new MemoryPortDual(bitWidth, scala.math.pow(2, bitWidth).toLong)
    val GPIO0Port    = Flipped(new GPIOPort(bitWidth))
    val Timer0Port   = Flipped(new TimerPort(bitWidth))
  })

  val dataOut      = WireInit(0.U(bitWidth.W))
  val stall        = WireDefault(false.B)
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
    // Dummy output
    when(readAddress(11, 0) === 0x0L.U) { // (0x0000_1000)
      dataOut := 0xbaad_cafeL.U
    }
    // Clock frequency
    when(readAddress(11, 0) === 0x8L.U) { // (0x0000_1008)
      dataOut := clockFreq.asUInt(bitWidth.W)
    }
    // Has UART0
    when(readAddress(11, 0) === 0x10L.U) { // (0x0000_1010)
      dataOut := 0.U
    }
    // Has GPIO0
    when(readAddress(11, 0) === 0x18L.U) { // (0x0000_1018)
      dataOut := 1.U
    }
    // Has PWM0
    when(readAddress(11, 0) === 0x20L.U) { // (0x0000_1020)
      dataOut := 0.U
    }
    // Has Timer0
    when(readAddress(11, 0) === 0x24L.U) { // (0x0000_1024)
      dataOut := 1.U
    }
  }

  /* --- UART0 --- */
  when(readAddress(31, 12) === 0x3000_0L.U || writeAddress(31, 12) === 0x3000_0L.U) {
    dataOut := 0.U
  }

  // GPIO0
  when(readAddress(31, 12) === 0x3000_1L.U || writeAddress(31, 12) === 0x3000_1L.U) {
    // Direction
    when(readAddress(7, 0) === 0x00.U) {
      io.GPIO0Port.writeDirection := io.MemoryIOPort.writeEnable
    }
      // Value
      .elsewhen(readAddress(7, 0) === 0x04.U) {
        io.GPIO0Port.writeValue := io.MemoryIOPort.writeEnable
      }
    io.GPIO0Port.dataIn := io.MemoryIOPort.writeData
    dataOut             := io.GPIO0Port.dataOut
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
  when(readAddress(31, 16) === 0x8000L.U || writeAddress(31, 16) === 0x8000L.U) {
    // Set the data write mask
    val writeMask = WireDefault(0.U(4.W))
    val dataIn    = WireDefault(0.U(bitWidth.W))

    switch(io.MemoryIOPort.dataSize) {
      is(3.U) { // Write word
        dataIn    := io.MemoryIOPort.writeData
        writeMask := "b1111".U
      }
      is(2.U) { // Write halfword
        switch(memory.io.dualPort.writeAddr(1).asUInt()) {
          is(1.U) { // Write half word 1
            dataIn    := Cat(io.MemoryIOPort.writeData(15, 0).asUInt, Fill(16, 0.U))
            writeMask := "b1100".U
          }
          is(0.U) { // Write half word 0
            dataIn    := Cat(Fill(16, 0.U), io.MemoryIOPort.writeData(15, 0).asUInt)
            writeMask := "b0011".U
          }
        }
      }
      is(1.U) { // Write byte
        switch(memory.io.dualPort.writeAddr(1, 0)) {
          is(3.U) { // Write byte 3
            dataIn    := Cat(io.MemoryIOPort.writeData(7, 0).asUInt, Fill(24, 0.U))
            writeMask := "b1000".U
          }
          is(2.U) { // Write byte 2
            dataIn    := Cat(Fill(8, 0.U), io.MemoryIOPort.writeData(7, 0).asUInt, Fill(16, 0.U))
            writeMask := "b0100".U
          }
          is(1.U) { // Write byte 2
            dataIn    := Cat(Fill(16, 0.U), io.MemoryIOPort.writeData(7, 0).asUInt, Fill(8, 0.U))
            writeMask := "b0010".U
          }
          is(0.U) { // Write byte 0
            dataIn    := Cat(Fill(24, 0.U), io.MemoryIOPort.writeData(7, 0).asUInt)
            writeMask := "b0001".U
          }
        }
      }
    }
    when(io.MemoryIOPort.writeEnable) {
      memory.io.dualPort.writeEnable := io.MemoryIOPort.writeEnable
      memory.io.dualPort.writeMask   := writeMask
      memory.io.dualPort.writeData   := dataIn
    }
    dataOut := memory.io.dualPort.readData
  }

  io.MemoryIOPort.readData := dataOut
  io.MemoryIOPort.stall    := stall
}
