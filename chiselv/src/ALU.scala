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
  val io = IO(new Bundle {
    val ALUPort = new ALUPort(bitWidth)
  })

  val a   = io.ALUPort.a
  val b   = io.ALUPort.b
  val out = WireDefault(0.U(bitWidth.W))

  // For RV32I the shift amount is 5 bits, for RV64I is 6 bits
  val shamt = if (bitWidth == 32) b(4, 0).asUInt else b(5, 0).asUInt

  // Use the correct ALU operation on Immediate instructions
  val op = MuxCase(
    io.ALUPort.inst,
    Seq(
      (io.ALUPort.inst === ADDI)  -> ADD,
      (io.ALUPort.inst === SRAI)  -> SRA,
      (io.ALUPort.inst === SRLI)  -> SRL,
      (io.ALUPort.inst === SLLI)  -> SLL,
      (io.ALUPort.inst === ANDI)  -> AND,
      (io.ALUPort.inst === ORI)   -> OR,
      (io.ALUPort.inst === XORI)  -> XOR,
      (io.ALUPort.inst === SLTI)  -> SLT,
      (io.ALUPort.inst === SLTIU) -> SLTU,
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

  io.ALUPort.x := out
}
