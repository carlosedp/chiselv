package chiselv

import chisel3._

class PCPort(
  bitWidth: Int = 32,
) extends Bundle {
  val dataIn      = Input(UInt(bitWidth.W))
  val PC          = Output(UInt(bitWidth.W))
  val PC4         = Output(UInt(bitWidth.W))
  val writeEnable = Input(Bool())
  val writeAdd    = Input(Bool()) // 1 => Add dataIn to PC, 0 => Set dataIn to PC
}

class ProgramCounter(
  regWidth:   Int = 32,
  entryPoint: Long = 0,
) extends Module {
  val io = IO(new Bundle {
    val pcPort = new PCPort(regWidth)
  })
  val pc = RegInit(entryPoint.U(regWidth.W))

  when(io.pcPort.writeEnable) {
    pc := Mux(io.pcPort.writeAdd, (pc.asSInt + io.pcPort.dataIn.asSInt).asUInt, io.pcPort.dataIn)
  }
  io.pcPort.PC4 := pc + 4.U
  io.pcPort.PC  := pc
}
