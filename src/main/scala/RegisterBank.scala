import chisel3._
import chisel3.util._

class RegisterBankPort(bitWidth: Int = 32) extends Bundle {
  val dataIn      = Input(UInt(bitWidth.W))
  val dataOut     = Output(UInt(bitWidth.W))
  val address     = Input(UInt(log2Ceil(bitWidth + 1).W))
  val writeEnable = Input(Bool())
}

class RegisterBank(numRegs: Int = 32, regWidth: Int = 32) extends Module {
  val io = IO(new Bundle {
    val regPort = new RegisterBankPort(regWidth)
  })

  val regs = Mem(numRegs, UInt(regWidth.W))
  regs.write(0.U, 0.U) // Register x0 is always 0

  io.regPort.dataOut := regs.read(io.regPort.address)
  when(io.regPort.writeEnable && io.regPort.address =/= 0.U) {
    regs.write(io.regPort.address, io.regPort.dataIn)
  }
}
