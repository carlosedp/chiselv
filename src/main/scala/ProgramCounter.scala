import chisel3._

class PCPort(bitWidth: Int = 32) extends Bundle {
  val dataIn      = Input(UInt(bitWidth.W))
  val dataOut     = Output(UInt(bitWidth.W))
  val writeEnable = Input(Bool())
  val writeAdd    = Input(Bool()) // 1 => Add dataIn to PC, 0 => Set dataIn to PC
  val countEnable = Input(Bool()) // 1 => COUNT UP, 0 => STOPPED
}

class ProgramCounter(regWidth: Int = 32, entryPoint: Long = 0) extends Module {
  val io = IO(new Bundle {
    val pcPort = new PCPort(regWidth)
  })
  val pc = RegInit(entryPoint.U(regWidth.W))

  when(io.pcPort.writeEnable) {
    pc := Mux(io.pcPort.writeAdd, pc + io.pcPort.dataIn - 4.U, io.pcPort.dataIn)
  }.elsewhen(io.pcPort.countEnable)(pc := pc + 4.U)

  io.pcPort.dataOut := pc
}
