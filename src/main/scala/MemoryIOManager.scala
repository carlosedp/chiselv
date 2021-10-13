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
 * 0x4000_0000 - 0x4FFF_FFFF: Off-chip memory (256MB)
 * 0x5000_0000 - 0x7000_0000: Reserved
 * 0x8000_0000 - 0x8000_FFFF: On-chip memory ROM (64KB)
 * 0x8001_0000 - 0xFFFF_FFFF: Reserved
 */

class MemoryIOPort(bitWidth: Int = 32) extends Bundle {
  val address     = Input(UInt(bitWidth.W))
  val dataIn      = Input(UInt(bitWidth.W))
  val dataOut     = Output(UInt(bitWidth.W))
  val op          = Input(UInt(1.W)) // 0: read, 1: write
  val writeEnable = Input(Bool())
}

class SerialIOPort() extends Bundle {
  val tx = Output(UInt(1.W))
  val rx = Input(UInt(1.W))
}

class MemoryIOManager(bitWidth: Int = 32, clockFreq: Long) extends Module {
  val io = IO(new Bundle {
    val MemoryIOPort = new MemoryIOPort(bitWidth)
    // val UART0Port    = new SerialIOPort()
    // val GPIO0Port    = new SerialIOPort()
  })

  val op      = io.MemoryIOPort.op
  val dataOut = Reg(UInt(bitWidth.W))
  val address = io.MemoryIOPort.address

  /* --- Syscon --- */
  when(address(31, 12) === 0x0000_1L.U) {
    when(op === 0.U) { // Read
      // Dummy output
      when(address(11, 0) === 0x0L.U) { // (0x0000_1000)
        dataOut := 0xbaad_cafeL.U
      }
      // Clock frequency
      when(address(11, 0) === 0x8L.U) { // (0x0000_1008)
        dataOut := clockFreq.asUInt(bitWidth.W)
      }
      // Has UART0
      when(address(11, 0) === 0x10L.U) { // (0x0000_1010)
        dataOut := 0.U
      }
      // Has GPIO0
      when(address(11, 0) === 0x18L.U) { // (0x0000_1018)
        dataOut := 0.U
      }
      // Has PWM0
      when(address(11, 0) === 0x20L.U) { // (0x0000_1020)
        dataOut := 0.U
      }
    }
  }
  /* --- UART0 --- */
  when(address(31, 12) === 0x3000_0L.U) {}
  /* --- GPIO0 --- */
  when(address(31, 12) === 0x3000_1L.U) {}
  /* --- PWM0 --- */
  when(address(31, 12) === 0x3000_2L.U) {}

  /* --- Data Memory --- */
  val memory = Module(new DualPortRAM(bitWidth, 64 * 1024))
  dontTouch(memory.io.dualPort)
  val addressOffset = 0x8000_0000L.U
  memory.io.dualPort.writeEnable := false.B
  memory.io.dualPort.writeData   := DontCare
  memory.io.dualPort.readAddr    := 0.U
  memory.io.dualPort.writeAddr   := 0.U

  when(address(31, 16) === 0x8000L.U) {
    memory.io.dualPort.readAddr := address - addressOffset
    when(op === 1.U) { // Write
      memory.io.dualPort.writeEnable := true.B
      memory.io.dualPort.writeAddr   := address - addressOffset
      memory.io.dualPort.writeData   := io.MemoryIOPort.dataIn
    }
    dataOut := memory.io.dualPort.readData
  }

  io.MemoryIOPort.dataOut := dataOut
}
