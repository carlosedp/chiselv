import chisel3._
import chisel3.util._

import Instruction._

class ALUPort(bitWidth: Int = 32) extends Bundle {
  val op = Output(Instruction())
  val a  = Output(UInt(bitWidth.W))
  val b  = Output(UInt(bitWidth.W))
  val x  = Input(UInt(bitWidth.W))
}
class ALU(bitWidth: Int = 32) extends Module {
  val io = IO(new Bundle {
    val ALUPort = Flipped(new ALUPort(bitWidth))
  })

  val op = io.ALUPort.op
  val a  = io.ALUPort.a
  val b  = io.ALUPort.b

  val out   = WireDefault(UInt(bitWidth.W), 0.U)
  val shamt = b(4, 0).asUInt() // For RV32I the shift amount is 5 bits wide
  // val shamt = b(5, 0).asUInt() // For RV64I the shift amount is 6 bits wide

  switch(op) {
    // Arithmetic
    is(ADD)(out := a + b)
    is(SUB)(out := a - b)
    // Shifts
    is(SRA)(out := (a.asSInt >> shamt).asUInt) // Signed
    is(SRL)(out := a >> shamt)
    is(SLL)(out := a << shamt)
    // Logical
    is(AND)(out := a & b)
    is(OR)(out := a | b)
    is(XOR)(out := a ^ b)
    // Compare
    is(SLT)(out := (a.asSInt < b.asSInt).asUInt) // Signed
    is(SLTU)(out := a < b)
  }

  io.ALUPort.x := out
}
