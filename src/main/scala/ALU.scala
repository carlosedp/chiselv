import chisel3._
import chisel3.util._

import Instructions._

class ALU(width: Int = 32) extends Module {
  val io = IO(new Bundle {
    val op = Input(Instructions())
    val a  = Input(UInt(width.W))
    val b  = Input(UInt(width.W))
    val x  = Output(UInt(width.W))
  })

  val op = io.op
  val a  = io.a
  val b  = io.b

  val out   = WireDefault(UInt(width.W), 0.U)
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

  io.x := out
}
