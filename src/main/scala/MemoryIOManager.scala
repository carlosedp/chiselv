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
 *                 0x04 (value     - 0: low, 1: high)
 * 0x3000_2000 - 0x3000_2FFF: PWM0
 * 0x3000_3000 - 0x3000_3FFF: Timer0
 *                 0x00 (32 bit value in miliseconds)
 * 0x3000_4000 - 0x3FFF_FFFF: Reserved
 * 0x4000_0000 - 0x4FFF_FFFF: Reserved
 * 0x5000_0000 - 0x7000_0000: Reserved
 * 0x8000_0000 - 0x8FFF_FFFF: On-chip memory RAM
 * 0x9000_0000 - 0x9FFF_FFFF: Reserved
 */

class MMIOPort(val bitWidth: Int, val addressSize: Long) extends Bundle {
  val writeRequest = Input(Bool())
  val readRequest  = Input(Bool())
  val readAddr     = Input(UInt(log2Ceil(addressSize).W))
  val readData     = Output(UInt(bitWidth.W))
  val writeAddr    = Input(UInt(log2Ceil(addressSize).W))
  val writeData    = Input(UInt(bitWidth.W))
  val writeMask    = Input(UInt((bitWidth / 8).W))
  val dataSize     = Input(UInt(2.W))
}

class MemoryIOManager(bitWidth: Int = 32, sizeBytes: Long = 1024) extends Module {
  val io = IO(new Bundle {
    val MemoryIOPort = new MMIOPort(bitWidth, scala.math.pow(2, bitWidth).toLong)
    val GPIO0Port    = Flipped(new GPIOPort(bitWidth))
    val Timer0Port   = Flipped(new TimerPort(bitWidth))
    val UART0Port    = Flipped(new UARTPort())
    val DataMemPort  = Flipped(new MemoryPortDual(bitWidth, sizeBytes))
    val SysconPort   = Flipped(new SysconPort(bitWidth))
    val stall        = Output(Bool())
  })

  val dataOut      = WireDefault(0.U(bitWidth.W))
  val readAddress  = io.MemoryIOPort.readAddr
  val writeAddress = io.MemoryIOPort.writeAddr

  // Initialize IO
  io.GPIO0Port.dataIn         := 0.U
  io.GPIO0Port.writeValue     := false.B
  io.GPIO0Port.writeDirection := false.B

  io.Timer0Port.dataIn      := 0.U
  io.Timer0Port.writeEnable := false.B

  io.UART0Port.txQueue.valid      := false.B
  io.UART0Port.txQueue.bits       := 0.U
  io.UART0Port.rxQueue.ready      := false.B
  io.UART0Port.clockDivisor.bits  := 0.U
  io.UART0Port.clockDivisor.valid := false.B

  io.DataMemPort.writeEnable  := false.B
  io.DataMemPort.writeData    := 0.U
  io.DataMemPort.readAddress  := 0.U
  io.DataMemPort.writeAddress := 0.U
  io.DataMemPort.dataSize     := 0.U
  io.DataMemPort.writeMask    := 0.U

  io.SysconPort.Address := 0.U

  // Stall Management
  val stallLatency = WireDefault(0.U(4.W))
  val stallEnable  = WireDefault(false.B)
  val DACK         = RegInit(0.U(2.W))
  DACK := Mux(
    DACK > 0.U,
    DACK - 1.U,
    Mux((io.MemoryIOPort.readRequest || io.MemoryIOPort.writeRequest), stallLatency, 0.U)
  )
  io.stall := ((io.MemoryIOPort.readRequest || io.MemoryIOPort.writeRequest) && DACK =/= 1.U && stallEnable)

  /* --- Syscon --- */
  when(readAddress(31, 12) === 0x0000_1L.U && io.MemoryIOPort.readRequest) {
    io.SysconPort.Address := readAddress(11, 0)
    dataOut               := io.SysconPort.DataOut
  }

  /* --- UART0 --- */
  when(readAddress(31, 12) === 0x3000_0L.U || writeAddress(31, 12) === 0x3000_0L.U) {
    // Reads
    when(io.MemoryIOPort.readRequest) {
      when(readAddress(7, 0) === 0x04.U) {
        /* RX */
        when(io.UART0Port.rxQueue.valid) {
          io.UART0Port.rxQueue.ready := true.B
          dataOut                    := io.UART0Port.rxQueue.bits
        }
      }
        /* Status */
        .elsewhen(readAddress(7, 0) === 0x0c.U) {
          dataOut := Cat(io.UART0Port.txFull, io.UART0Port.rxFull, io.UART0Port.txEmpty, io.UART0Port.rxEmpty)
        }
        /* Invalid */
        .otherwise(dataOut := 0.U)
    }
    // Writes (TX)
    when(io.MemoryIOPort.writeRequest) {
      when(writeAddress(7, 0) === 0x00.U) {
        /* TX */
        io.UART0Port.txQueue.valid := true.B
        io.UART0Port.txQueue.bits  := io.MemoryIOPort.writeData(7, 0)
      }
        /* clock divisor */
        .elsewhen(writeAddress(7, 0) === 0x10.U) {
          io.UART0Port.clockDivisor.valid := true.B
          io.UART0Port.clockDivisor.bits  := io.MemoryIOPort.writeData(7, 0)
        }
    }
  }

  // GPIO0
  when(readAddress(31, 12) === 0x3000_1L.U || writeAddress(31, 12) === 0x3000_1L.U) {
    // Reads
    when(io.MemoryIOPort.readRequest) {
      // -- Direction
      when(readAddress(7, 0) === 0x00.U) {
        dataOut := io.GPIO0Port.directionOut
      }
        // -- Value
        .elsewhen(readAddress(7, 0) === 0x04.U) {
          dataOut := io.GPIO0Port.valueOut
        }
        .otherwise(dataOut := 0.U)
    }
    // Writes
    when(io.MemoryIOPort.writeRequest) {
      // -- Direction
      when(writeAddress(7, 0) === 0x00.U) {
        io.GPIO0Port.writeDirection := io.MemoryIOPort.writeRequest
      }
        // -- Value
        .elsewhen(writeAddress(7, 0) === 0x04.U) {
          io.GPIO0Port.writeValue := io.MemoryIOPort.writeRequest
        }
      io.GPIO0Port.dataIn := io.MemoryIOPort.writeData
    }

  }

  /* --- PWM0 --- */
  when(readAddress(31, 12) === 0x3000_2L.U || writeAddress(31, 12) === 0x3000_2L.U) {
    dataOut := 0.U
  }

  /* --- Timer0 --- */
  when(readAddress(31, 12) === 0x3000_3L.U || writeAddress(31, 12) === 0x3000_3L.U) {
    when(io.MemoryIOPort.writeRequest) {
      io.Timer0Port.writeEnable := io.MemoryIOPort.writeRequest
      io.Timer0Port.dataIn      := io.MemoryIOPort.writeData
    }
    when(io.MemoryIOPort.readRequest) {
      dataOut := io.Timer0Port.dataOut
    }
  }

  /* --- Data Memory --- */
  when(readAddress(31, 28) === 0x8.U || writeAddress(31, 28) === 0x8.U) {
    // Stall core for 1 cycle
    stallLatency               := 1.U
    stallEnable                := true.B
    io.DataMemPort.readAddress := Cat(Fill(4, 0.U), readAddress(27, 0))

    switch(io.MemoryIOPort.dataSize) {
      is(3.U)(dataOut := io.DataMemPort.readData) // Read word
      is(2.U) { // Read halfword
        switch(io.DataMemPort.readAddress(1).asUInt()) {
          is(1.U)(dataOut := Cat(Fill(16, 0.U), io.DataMemPort.readData(31, 16).asUInt)) // Read half word 1
          is(0.U)(dataOut := Cat(Fill(16, 0.U), io.DataMemPort.readData(15, 0).asUInt))  // Read half word 0
        }
      }
      is(1.U) { // Write byte
        switch(io.DataMemPort.readAddress(1, 0)) {
          is(3.U)(dataOut := Cat(Fill(24, 0.U), io.DataMemPort.readData(31, 24).asUInt)) // Read byte 3
          is(2.U)(dataOut := Cat(Fill(24, 0.U), io.DataMemPort.readData(23, 16).asUInt)) // Read byte 2
          is(1.U)(dataOut := Cat(Fill(24, 0.U), io.DataMemPort.readData(15, 8).asUInt))  // Read byte 1
          is(0.U)(dataOut := Cat(Fill(24, 0.U), io.DataMemPort.readData(7, 0).asUInt))   // Read byte 0
        }
      }
    }

    when(io.MemoryIOPort.writeRequest) {
      when(!io.stall) {
        val dataToWrite = WireDefault(0.U(bitWidth.W))
        val writeMask   = WireDefault(0.U(4.W))

        io.DataMemPort.writeAddress := Cat(Fill(4, 0.U), writeAddress(27, 0))
        io.DataMemPort.writeEnable  := io.MemoryIOPort.writeRequest

        switch(io.MemoryIOPort.dataSize) {
          is(3.U) { // Write word
            dataToWrite := io.MemoryIOPort.writeData
            writeMask   := "b1111".U
          }
          is(2.U) { // Write halfword
            switch(io.DataMemPort.writeAddress(1).asUInt()) {
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
            switch(io.DataMemPort.writeAddress(1, 0)) {
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

        val dataIn = Cat(
          Mux(writeMask(3), dataToWrite(3 * 8 + 7, 3 * 8), io.DataMemPort.readData(3 * 8 + 7, 3 * 8)),
          Mux(writeMask(2), dataToWrite(2 * 8 + 7, 2 * 8), io.DataMemPort.readData(2 * 8 + 7, 2 * 8)),
          Mux(writeMask(1), dataToWrite(1 * 8 + 7, 1 * 8), io.DataMemPort.readData(1 * 8 + 7, 1 * 8)),
          Mux(writeMask(0), dataToWrite(0 * 8 + 7, 0 * 8), io.DataMemPort.readData(0 * 8 + 7, 0 * 8))
        )
        io.DataMemPort.writeData := dataIn
        dataOut                  := dataIn
      }
    }
  }

  io.MemoryIOPort.readData := dataOut
}
