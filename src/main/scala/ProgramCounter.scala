import chisel3._

class PCPort(bitWidth: Int = 32) extends Bundle {
  val dataIn      = Output(UInt(bitWidth.W))
  val dataOut     = Input(UInt(bitWidth.W))
  val writeEnable = Output(Bool())
  val writeAdd    = Output(Bool()) // 1 => Add dataIn to PC, 0 => Set dataIn to PC
  val countEnable = Output(Bool()) // 1 => COUNT UP, 0 => STOPPED
}

class ProgramCounter(regWidth: Int = 32) extends Module {
  val io = IO(new Bundle {
    val pcPort = Flipped(new PCPort(regWidth))
  })
  val pc = RegInit(0.U(regWidth.W))

  when(io.pcPort.writeEnable) {
    pc := Mux(io.pcPort.writeAdd, pc + io.pcPort.dataIn - 4.U, io.pcPort.dataIn)
  }.elsewhen(io.pcPort.countEnable)(pc := pc + 4.U)

  io.pcPort.dataOut := pc
}
