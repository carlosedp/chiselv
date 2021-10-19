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

class MemoryIOManager(bitWidth: Int = 32, clockFreq: Long, sizeBytes: Long = 1024) extends Module {
  val io = IO(new Bundle {
    val MemoryIOPort = new MemoryPortDual(bitWidth, scala.math.pow(2, bitWidth).toLong)
    val GPIO0Port    = Flipped(new GPIOPort(1))
  })

  val dataOut      = WireInit(0.U(bitWidth.W))
  val readAddress  = io.MemoryIOPort.readAddr
  val writeAddress = io.MemoryIOPort.writeAddr

  // Initialize IO
  io.GPIO0Port.dataIn      := DontCare
  io.GPIO0Port.dir         := DontCare
  io.GPIO0Port.writeEnable := DontCare

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
  }

  /* --- UART0 --- */
  when(readAddress(31, 12) === 0x3000_0L.U || writeAddress(31, 12) === 0x3000_0L.U) {
    dataOut := 0.U
  }

  // GPIO0
  when(readAddress(31, 12) === 0x3000_1L.U || writeAddress(31, 12) === 0x3000_1L.U) {
    io.GPIO0Port.writeEnable := io.MemoryIOPort.writeEnable
    io.GPIO0Port.dataIn      := io.MemoryIOPort.writeData
    io.GPIO0Port.dir         := 1.U // FIX THIS
    io.MemoryIOPort.readData := io.GPIO0Port.dataOut
  }

  /* --- PWM0 --- */
  when(readAddress(31, 12) === 0x3000_2L.U || writeAddress(31, 12) === 0x3000_2L.U) {
    dataOut := 0.U
  }

  /* --- Data Memory --- */
  val memory = Module(new DualPortRAM(bitWidth, sizeBytes))
  // Initialize IO
  memory.io.dualPort.writeEnable := false.B
  memory.io.dualPort.writeData   := DontCare
  memory.io.dualPort.readAddr    := DontCare
  memory.io.dualPort.writeAddr   := DontCare
  memory.io.dualPort.writeMask   := DontCare

  memory.io.dualPort.readAddr  := Cat(Fill(16, 0.U), readAddress(15, 0))
  memory.io.dualPort.writeAddr := Cat(Fill(16, 0.U), readAddress(15, 0))
  when(readAddress(31, 16) === 0x8000L.U || writeAddress(31, 16) === 0x8000L.U) {

    when(io.MemoryIOPort.writeEnable) {
      memory.io.dualPort.writeEnable := io.MemoryIOPort.writeEnable
      memory.io.dualPort.writeData   := io.MemoryIOPort.writeData
    }
    dataOut := memory.io.dualPort.readData
  }

  io.MemoryIOPort.readData := dataOut
}
