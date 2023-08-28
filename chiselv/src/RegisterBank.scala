package chiselv

import chisel3._
import chisel3.util.log2Ceil

class RegisterBankPort(bitWidth: Int = 32) extends Bundle {
  val rs1         = Output(UInt(bitWidth.W))
  val rs2         = Output(UInt(bitWidth.W))
  val rs1_addr    = Input(UInt(log2Ceil(bitWidth).W))
  val rs2_addr    = Input(UInt(log2Ceil(bitWidth).W))
  val regwr_addr  = Input(UInt(log2Ceil(bitWidth).W))
  val regwr_data  = Input(UInt(bitWidth.W))
  val writeEnable = Input(Bool())
  val stall       = Input(Bool()) // >1 => Stall, 0 => Run
}

class RegisterBank(numRegs: Int = 32, regWidth: Int = 32) extends Module {
  val io = IO(new Bundle {
    val regPort = new RegisterBankPort(regWidth)
  })

  val regs = RegInit(VecInit(Seq.fill(numRegs)(0.U(regWidth.W))))
  regs(0) := 0.U // Register x0 is always 0

  io.regPort.rs1 := regs(io.regPort.rs1_addr)
  io.regPort.rs2 := regs(io.regPort.rs2_addr)
  when(io.regPort.writeEnable && io.regPort.regwr_addr =/= 0.U && !io.regPort.stall) {
    regs(io.regPort.regwr_addr) := io.regPort.regwr_data
  }
}
