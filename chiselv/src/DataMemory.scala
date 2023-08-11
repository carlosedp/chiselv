package chiselv

import chisel3._
import chisel3.util.experimental.loadMemoryFromFileInline
import chisel3.util.log2Ceil

class MemoryPortDual(val bitWidth: Int, val addressSize: Long) extends Bundle {
  val readAddress  = Input(UInt(log2Ceil(addressSize).W))
  val readData     = Output(UInt(bitWidth.W))
  val writeAddress = Input(UInt(log2Ceil(addressSize).W))
  val writeData    = Input(UInt(bitWidth.W))
  val writeMask    = Input(UInt((bitWidth / 8).W))
  val dataSize     = Input(UInt(2.W))
  val writeEnable  = Input(Bool())
}
class DualPortRAM(
    bitWidth:   Int = 32,
    sizeBytes:  Long = 1,
    memoryFile: String = "",
    debugMsg:   Boolean = false,
  ) extends Module {
  val words = sizeBytes / bitWidth
  val io = IO(new Bundle {
    val dualPort = new MemoryPortDual(bitWidth, sizeBytes)
  })

  if (debugMsg) {
    println(s"Dual-port Memory Parameters:")
    println(s"  Words: $words")
    println(s"  Size: " + words * bitWidth + " bytes")
    println(s"  Bit width: $bitWidth bit")
    println(s"  Addr Width: " + io.dualPort.readAddress.getWidth + " bit")
  }

  val mem = SyncReadMem(words, UInt(bitWidth.W))

  // Divide memory address by 4 to get the word due to pc+4 addressing
  val readAddress  = io.dualPort.readAddress >> 2
  val writeAddress = io.dualPort.writeAddress >> 2

  if (memoryFile.trim().nonEmpty) {
    if (debugMsg) println(s"  Load memory file: " + memoryFile)
    loadMemoryFromFileInline(mem, memoryFile)
  }

  io.dualPort.readData := mem.read(readAddress)

  when(io.dualPort.writeEnable === true.B) {
    mem.write(writeAddress, io.dualPort.writeData)
  }
}
