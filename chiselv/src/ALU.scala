package chiselv

import chisel3._
import chisel3.util.{MuxCase, is, switch}
import chiselv.Instruction._

class ALUPort(bitWidth: Int = 32) extends Bundle {
  val inst = Input(Instruction())
  val a    = Input(UInt(bitWidth.W))
  val b    = Input(UInt(bitWidth.W))
  val x    = Output(UInt(bitWidth.W))
}
class ALU(bitWidth: Int = 32) extends Module {
  val io = IO(new ALUPort(bitWidth))

  val a   = io.a
  val b   = io.b
  val out = WireDefault(0.U(bitWidth.W))

  // For RV32I the shift amount is 5 bits, for RV64I is 6 bits
  val shamt = if (bitWidth == 32) b(4, 0).asUInt else b(5, 0).asUInt

  // Use the correct ALU operation on Immediate instructions
  val op = MuxCase(
    io.inst,
    Seq(
      (io.inst === ADDI)  -> ADD,
      (io.inst === SRAI)  -> SRA,
      (io.inst === SRLI)  -> SRL,
      (io.inst === SLLI)  -> SLL,
      (io.inst === ANDI)  -> AND,
      (io.inst === ORI)   -> OR,
      (io.inst === XORI)  -> XOR,
      (io.inst === SLTI)  -> SLT,
      (io.inst === SLTIU) -> SLTU,
    ),
  )

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
    is(OR)(out  := a | b)
    is(XOR)(out := a ^ b)
    // Compare
    is(SLT)(out  := Mux(a.asSInt < b.asSInt, 1.U, 0.U)) // Signed
    is(SLTU)(out := Mux(a < b, 1.U, 0.U))
    // Auxiliary
    is(EQ)(out   := Mux(a === b, 1.U, 0.U))
    is(NEQ)(out  := Mux(a =/= b, 1.U, 0.U))
    is(GTE)(out  := Mux(a.asSInt >= b.asSInt, 1.U, 0.U))
    is(GTEU)(out := Mux(a >= b, 1.U, 0.U))
  }

  io.x := out
}
