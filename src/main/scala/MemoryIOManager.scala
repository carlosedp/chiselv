import chisel3._

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
 * 0x3000_2000 - 0x3000_2FFF: PWM0
 * 0x3000_3000 - 0x3000_FFFF: Reserved
 * 0x3001_0000 - 0x3001_0FFF: UART1
 * 0x3001_1000 - 0x3001_1FFF: GPIO1
 * 0x3001_2000 - 0x3001_2FFF: PWM1
 * 0x3001_3000 - 0x3FFF_FFFF: Reserved
 * 0x4000_0000 - 0x4FFF_FFFF: Reserved
 * 0x5000_0000 - 0x7000_0000: Reserved
 * 0x8000_0000 - 0x8000_FFFF: On-chip memory RAM (64KB)
 * 0x8001_0000 - 0xFFFF_FFFF: Reserved
 */

class SerialIOPort() extends Bundle {
  val tx = Output(UInt(1.W))
  val rx = Input(UInt(1.W))
}

class MemoryIOManager(bitWidth: Int = 32, clockFreq: Long, sizeBytes: Long = 1024) extends Module {
  val io = IO(new Bundle {
    val MemoryIOPort = new DualPort(bitWidth, scala.math.pow(2, bitWidth).toLong)
    // val UART0Port    = new SerialIOPort()
    // val GPIO0Port    = new SerialIOPort()
  })

  val dataOut      = WireInit(0.U(bitWidth.W))
  val readAddress  = io.MemoryIOPort.readAddr
  val writeAddress = io.MemoryIOPort.writeAddr

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
      dataOut := 0.U
    }
    // Has PWM0
    when(readAddress(11, 0) === 0x20L.U) { // (0x0000_1020)
      dataOut := 0.U
    }
  }

  /* --- UART0 --- */
  when(readAddress(31, 12) === 0x3000_0L.U || writeAddress(31, 12) === 0x3000_0L.U) {
    dataOut := 0.U
  }

  /* --- GPIO0 --- */
  when(readAddress(31, 12) === 0x3000_1L.U || writeAddress(31, 12) === 0x3000_1L.U) {
    dataOut := 0.U
  }

  /* --- PWM0 --- */
  when(readAddress(31, 12) === 0x3000_2L.U || writeAddress(31, 12) === 0x3000_2L.U) {
    dataOut := 0.U
  }

  /* --- Data Memory --- */
  val memory = Module(new DualPortRAM(bitWidth, sizeBytes))
  dontTouch(memory.io.dualPort)
  val addressOffset = 0x8000_0000L.U
  memory.io.dualPort.writeEnable := false.B
  memory.io.dualPort.writeData   := DontCare
  memory.io.dualPort.readAddr    := 0.U
  memory.io.dualPort.writeAddr   := 0.U

  memory.io.dualPort.readAddr  := readAddress - addressOffset
  memory.io.dualPort.writeAddr := writeAddress - addressOffset
  when(readAddress(31, 16) === 0x8000L.U || writeAddress(31, 16) === 0x8000L.U) {

    when(io.MemoryIOPort.writeEnable) {
      memory.io.dualPort.writeEnable := io.MemoryIOPort.writeEnable
      memory.io.dualPort.writeData   := io.MemoryIOPort.writeData
    }
    dataOut := memory.io.dualPort.readData
  }

  io.MemoryIOPort.readData := dataOut
}
