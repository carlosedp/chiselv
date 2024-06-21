package chiselv

import chisel3._
import chisel3.util.{is, switch}
import chiselv.Instruction._

class ALUPort(bitWidth: Int = 32) extends Bundle {
  val inst = Input(Instruction())
  val a    = Input(UInt(bitWidth.W))
  val b    = Input(UInt(bitWidth.W))
  val x    = Output(UInt(bitWidth.W))
}
class ALU(bitWidth: Int = 32) extends Module {
  val io  = IO(new ALUPort(bitWidth))
  val out = WireDefault(0.U(bitWidth.W))

  // For RV32I the shift amount is 5 bits, for RV64I is 6 bits
  val shamt = (if (bitWidth == 32) 5 else if (bitWidth == 64) 6 else 0) - 1

  switch(io.inst) {
    // Arithmetic
    is(ADD, ADDI)(out := io.a + io.b)
    is(SUB)(out       := io.a - io.b)
    // Shifts
    is(SRA, SRAI)(out := (io.a.asSInt >> io.b(shamt, 0)).asUInt) // Signed
    is(SRL, SRLI)(out := io.a >> io.b(shamt, 0))
    is(SLL, SLLI)(out := io.a << io.b(shamt, 0))
    // Logical
    is(AND, ANDI)(out := io.a & io.b)
    is(OR, ORI)(out   := io.a | io.b)
    is(XOR, XORI)(out := io.a ^ io.b)
    // Compare
    is(SLT, SLTI)(out   := Mux(io.a.asSInt < io.b.asSInt, 1.U, 0.U)) // Signed
    is(SLTU, SLTIU)(out := Mux(io.a < io.b, 1.U, 0.U))
    // Auxiliary
    is(EQ)(out   := Mux(io.a === io.b, 1.U, 0.U))
    is(NEQ)(out  := Mux(io.a =/= io.b, 1.U, 0.U))
    is(GTE)(out  := Mux(io.a.asSInt >= io.b.asSInt, 1.U, 0.U))
    is(GTEU)(out := Mux(io.a >= io.b, 1.U, 0.U))
  }

  io.x := out
}
