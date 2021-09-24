import chisel3._

class ProgramCounter(regWidth: Int = 32) extends Module {
  val io = IO(new Bundle {
    val dataIn      = Input(UInt(regWidth.W))
    val dataOut     = Output(UInt(regWidth.W))
    val writeEnable = Input(Bool())
    val writeAdd    = Input(Bool()) // 1 => Add dataIn to PC, 0 => Set dataIn to PC
    val countEnable = Input(Bool()) // 1 => COUNT UP, 0 => STOPPED
  })
  val pc = RegInit(0.U(regWidth.W))

  when(io.writeEnable) {
    pc := Mux(io.writeAdd, pc + io.dataIn - 4.U, io.dataIn)
  }.elsewhen(io.countEnable)(pc := pc + 4.U)

  io.dataOut := pc
}
