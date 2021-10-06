import chisel3._
import chisel3.experimental.{ChiselAnnotation, annotate}
import chisel3.util.experimental.loadMemoryFromFileInline
import chisel3.util.log2Ceil

class DualPort(val bitWidth: Int, val words: Int) extends Bundle {
  val readAddr    = Input(UInt(log2Ceil(words).W))
  val readData    = Output(UInt(bitWidth.W))
  val writeAddr   = Input(UInt(log2Ceil(words).W))
  val writeData   = Input(UInt(bitWidth.W))
  val writeEnable = Input(Bool())
}

class DualPortRAM(
  bitWidth: Int = 32,
  sizeBytes: Int = 1,
  memoryFile: String = "",
) extends Module {
  val words = sizeBytes / bitWidth
  val io = IO(new Bundle() {
    val dualPort = new DualPort(bitWidth, words)
  })
  println(s"Dual-port Memory Parameters:")
  println(s"  Words: $words")
  println(s"  Size: " + words * bitWidth + " bytes")
  println(s"  Bit width: $bitWidth bit")
  println(s"  Addr Width: " + io.dualPort.readAddr.getWidth + " bit")

  // This is required to have readmem outside `ifndef SYNTHESIS` and be synthesized by FPGA tools
  annotate(new ChiselAnnotation { override def toFirrtl = firrtl.annotations.MemorySynthInit })

  val mem = SyncReadMem(words, UInt(bitWidth.W))
  if (memoryFile.trim().nonEmpty) {
    println(s"  Load memory file: " + memoryFile)
    loadMemoryFromFileInline(mem, memoryFile)
  }

  io.dualPort.readData := mem.read(io.dualPort.readAddr)
  when(io.dualPort.writeEnable === true.B) {
    mem.write(io.dualPort.writeAddr, io.dualPort.writeData)
  }
}
