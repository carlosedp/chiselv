import Instruction._
import chisel3._
import chisel3.util._

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

  val op = io.ALUPort.inst
  val a  = io.ALUPort.a
  val b  = io.ALUPort.b

  val out   = WireDefault(UInt(bitWidth.W), 0.U)
  val shamt = b(4, 0).asUInt() // For RV32I the shift amount is 5 bits wide
  // val shamt = b(5, 0).asUInt() // For RV64I the shift amount is 6 bits wide

  switch(op) {
    // Arithmetic
    is(ADD)(out  := a + b)
    is(ADDI)(out := a + b)
    is(SUB)(out  := a - b)
    // Shifts
    is(SRA)(out  := (a.asSInt >> shamt).asUInt) // Signed
    is(SRAI)(out := (a.asSInt >> shamt).asUInt) // Signed
    is(SRL)(out  := a >> shamt)
    is(SRLI)(out := a >> shamt)
    is(SLL)(out  := a << shamt)
    is(SLLI)(out := a << shamt)
    // Logical
    is(AND)(out  := a & b)
    is(ANDI)(out := a & b)
    is(OR)(out   := a | b)
    is(ORI)(out  := a | b)
    is(XOR)(out  := a ^ b)
    is(XORI)(out := a ^ b)
    // Compare
    is(SLT)(out   := (Mux((a.asSInt < b.asSInt), 1.U, 0.U))) // Signed
    is(SLTI)(out  := (Mux((a.asSInt < b.asSInt), 1.U, 0.U))) // Signed
    is(SLTU)(out  := Mux(a < b, 1.U, 0.U))
    is(SLTIU)(out := Mux(a < b, 1.U, 0.U))
    // Auxiliary
    is(EQ)(out   := Mux(a === b, 1.U, 0.U))
    is(NEQ)(out  := Mux(a =/= b, 1.U, 0.U))
    is(GTE)(out  := Mux(a >= b, 1.U, 0.U))
    is(GTEU)(out := (Mux((a.asSInt >= b.asSInt), 1.U, 0.U))) // Signed
  }

  io.ALUPort.x := out
}
