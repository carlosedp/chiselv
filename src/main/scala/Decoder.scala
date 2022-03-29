package chiselv

import chisel3._
import chisel3.util.{BitPat, Cat, Fill, ListLookup, is, switch}

import Instruction._
import InstructionType._

class DecoderPort(bitWidth: Int = 32) extends Bundle {
  val op       = Input(UInt(bitWidth.W))  // Op is the 32 bit instruction read received for decoding
  val inst     = Output(Instruction())    // Instruction is the decoded instruction
  val rd       = Output(UInt(5.W))        // Rd is the 5 bit destiny register
  val rs1      = Output(UInt(5.W))        // Rs1 is the 5 bit source register 1
  val rs2      = Output(UInt(5.W))        // Rs2 is the 5 bit source register 2
  val imm      = Output(SInt(bitWidth.W)) // Imm is the 32 bit immediate
  val toALU    = Output(Bool())           // ToALU is a flag to indicate if the instruction is to be executed in the ALU
  val branch   = Output(Bool())           // Branch is a flag to indicate if the instruction should jump and link. Update PC
  val use_imm  = Output(Bool())           // Use_imm is a flag to indicate if the instruction has an immediate
  val jump     = Output(Bool())           // jump is a flag to indicate if the instruction has is a jump
  val is_load  = Output(Bool())           // is_load is a flag to indicate if the instruction has a load from memory
  val is_store = Output(Bool())           // is_store is a flag to indicate if the instruction has a store to memory
}

class Decoder(bitWidth: Int = 32) extends Module {
  val io = IO(new Bundle {
    val DecoderPort = new DecoderPort(bitWidth)
  })

  val signals =
    ListLookup(
      io.DecoderPort.op,
      // format: off
      List(                                                  IN_ERR, ERR_INST, false.B,  false.B, false.B,  false.B,  false.B,  false.B), // Default values
      Array(
        /*                                                   inst_type,  inst   to_alu   branch   use_imm   jump      is_load   is_store*/
        // Arithmetic
        BitPat("b0000000??????????000?????0110011")  -> List(INST_R,      ADD,  true.B,  false.B, false.B,  false.B,  false.B,  false.B),
        BitPat("b?????????????????000?????0010011")  -> List(INST_I,     ADDI,  true.B,  false.B, true.B,   false.B,  false.B,  false.B),
        BitPat("b0100000??????????000?????0110011")  -> List(INST_R,      SUB,  true.B,  false.B, false.B,  false.B,  false.B,  false.B),
        BitPat("b?????????????????????????0110111")  -> List(INST_U,      LUI, false.B,  false.B, true.B,   false.B,  false.B,  false.B),
        BitPat("b?????????????????????????0010111")  -> List(INST_U,    AUIPC, false.B,  false.B, true.B,   false.B,  false.B,  false.B),
        // Shifts
        BitPat("b0000000??????????001?????0110011")  -> List(INST_R,      SLL,  true.B,  false.B, false.B,  false.B,  false.B,  false.B),
        BitPat("b0000000??????????001?????0010011")  -> List(INST_I,     SLLI,  true.B,  false.B, true.B,   false.B,  false.B,  false.B),
        BitPat("b0100000??????????101?????0110011")  -> List(INST_R,      SRL,  true.B,  false.B, false.B,  false.B,  false.B,  false.B),
        BitPat("b0000000??????????101?????0010011")  -> List(INST_I,     SRLI,  true.B,  false.B, true.B,   false.B,  false.B,  false.B),
        BitPat("b0100000??????????101?????0110011")  -> List(INST_R,      SRA,  true.B,  false.B, false.B,  false.B,  false.B,  false.B),
        BitPat("b0100000??????????101?????0010011")  -> List(INST_I,     SRAI,  true.B,  false.B, true.B,   false.B,  false.B,  false.B),
        // Logical
        BitPat("b0000000??????????100?????0110011")  -> List(INST_R,      XOR, true.B,   false.B, false.B,  false.B,  false.B,  false.B),
        BitPat("b?????????????????100?????0010011")  -> List(INST_I,     XORI, true.B,   false.B, true.B,   false.B,  false.B,  false.B),
        BitPat("b0000000??????????110?????0110011")  -> List(INST_R,       OR, true.B,   false.B, false.B,  false.B,  false.B,  false.B),
        BitPat("b?????????????????110?????0010011")  -> List(INST_I,      ORI, true.B,   false.B, true.B,   false.B,  false.B,  false.B),
        BitPat("b0000000??????????111?????0110011")  -> List(INST_R,      AND, true.B,   false.B, false.B,  false.B,  false.B,  false.B),
        BitPat("b?????????????????111?????0010011")  -> List(INST_I,     ANDI, true.B,   false.B, true.B,   false.B,  false.B,  false.B),
        // Compare
        BitPat("b0000000??????????010?????0110011")  -> List(INST_R,     SLT,  true.B,   false.B, false.B,  false.B,  false.B,  false.B),
        BitPat("b?????????????????010?????0010011")  -> List(INST_I,    SLTI,  true.B,   false.B, true.B,   false.B,  false.B,  false.B),
        BitPat("b0000000??????????011?????0110011")  -> List(INST_R,    SLTU,  true.B,   false.B, false.B,  false.B,  false.B,  false.B),
        BitPat("b?????????????????011?????0010011")  -> List(INST_I,   SLTIU,  true.B,   false.B, true.B,   false.B,  false.B,  false.B),
        // Branches
        BitPat("b?????????????????000?????1100011")  -> List(INST_B,     BEQ, false.B,    true.B, true.B,   false.B,  false.B,  false.B),
        BitPat("b?????????????????001?????1100011")  -> List(INST_B,     BNE, false.B,    true.B, true.B,   false.B,  false.B,  false.B),
        BitPat("b?????????????????100?????1100011")  -> List(INST_B,     BLT, false.B,    true.B, true.B,   false.B,  false.B,  false.B),
        BitPat("b?????????????????101?????1100011")  -> List(INST_B,     BGE, false.B,    true.B, true.B,   false.B,  false.B,  false.B),
        BitPat("b?????????????????110?????1100011")  -> List(INST_B,    BLTU, false.B,    true.B, true.B,   false.B,  false.B,  false.B),
        BitPat("b?????????????????111?????1100011")  -> List(INST_B,    BGEU, false.B,    true.B, true.B,   false.B,  false.B,  false.B),
        // Jump & link
        BitPat("b?????????????????????????1101111")  -> List(INST_J,     JAL, false.B,   false.B, true.B,    true.B,  false.B,  false.B),
        BitPat("b?????????????????000?????1100111")  -> List(INST_I,    JALR, false.B,   false.B, true.B,    true.B,  false.B,  false.B),
        // Sync
        BitPat("b0000????????00000000000000001111")  -> List(INST_I,   FENCE, false.B,   false.B, false.B,   false.B, false.B,  false.B),
        BitPat("b00000000000000000000001000001111")  -> List(INST_I,  FENCEI, false.B,   false.B,  true.B,   false.B, false.B,  false.B),
        // Environment
        BitPat("b00000000000000000000000001110011")  -> List(INST_I,   ECALL, false.B,   false.B, false.B,   false.B, false.B,  false.B),
        BitPat("b00000000000100000000000001110011")  -> List(INST_I,  EBREAK, false.B,   false.B, false.B,   false.B, false.B,  false.B),
        // CSR
        BitPat("b?????????????????001?????1110011")  -> List(INST_I,   CSRRW, false.B,   false.B, false.B,   false.B, false.B,  false.B),
        BitPat("b?????????????????010?????1110011")  -> List(INST_I,   CSRRS, false.B,   false.B, false.B,   false.B, false.B,  false.B),
        BitPat("b?????????????????011?????1110011")  -> List(INST_I,   CSRRC, false.B,   false.B, false.B,   false.B, false.B,  false.B),
        BitPat("b?????????????????101?????1110011")  -> List(INST_I,  CSRRWI, false.B,   false.B, true.B,    false.B, false.B,  false.B),
        BitPat("b?????????????????110?????1110011")  -> List(INST_I,  CSRRSI, false.B,   false.B, true.B,    false.B, false.B,  false.B),
        BitPat("b?????????????????111?????1110011")  -> List(INST_I,  CSRRCI, false.B,   false.B, true.B,    false.B, false.B,  false.B),
        // Loads
        BitPat("b?????????????????000?????0000011")  -> List(INST_I,      LB, false.B,   false.B, true.B,    false.B,  true.B,  false.B),
        BitPat("b?????????????????001?????0000011")  -> List(INST_I,      LH, false.B,   false.B, true.B,    false.B,  true.B,  false.B),
        BitPat("b?????????????????100?????0000011")  -> List(INST_I,     LBU, false.B,   false.B, true.B,    false.B,  true.B,  false.B),
        BitPat("b?????????????????101?????0000011")  -> List(INST_I,     LHU, false.B,   false.B, true.B,    false.B,  true.B,  false.B),
        BitPat("b?????????????????010?????0000011")  -> List(INST_I,      LW, false.B,   false.B, true.B,    false.B,  true.B,  false.B),
        // Stores
        BitPat("b?????????????????000?????0100011")  -> List(INST_S,      SB, false.B,   false.B, true.B,    false.B, false.B,   true.B),
        BitPat("b?????????????????001?????0100011")  -> List(INST_S,      SH, false.B,   false.B, true.B,    false.B, false.B,   true.B),
        BitPat("b?????????????????010?????0100011")  -> List(INST_S,      SW, false.B,   false.B, true.B,    false.B, false.B,   true.B),
      )
    ) // format: on

  io.DecoderPort.rd       := 0.U
  io.DecoderPort.rs1      := 0.U
  io.DecoderPort.rs2      := 0.U
  io.DecoderPort.imm      := 0.S
  io.DecoderPort.inst     := signals(1)
  io.DecoderPort.toALU    := signals(2)
  io.DecoderPort.branch   := signals(3)
  io.DecoderPort.use_imm  := signals(4)
  io.DecoderPort.jump     := signals(5)
  io.DecoderPort.is_load  := signals(6)
  io.DecoderPort.is_store := signals(7)

  switch(signals(0)) {
    is(INST_R) {
      io.DecoderPort.rd  := io.DecoderPort.op(11, 7)
      io.DecoderPort.rs1 := io.DecoderPort.op(19, 15)
      io.DecoderPort.rs2 := io.DecoderPort.op(24, 20)
    }
    is(INST_I) {
      io.DecoderPort.rd  := io.DecoderPort.op(11, 7)
      io.DecoderPort.rs1 := io.DecoderPort.op(19, 15)
      io.DecoderPort.imm := ImmGenerator(INST_I, io.DecoderPort.op)
    }
    is(INST_S) {
      io.DecoderPort.rs1 := io.DecoderPort.op(19, 15)
      io.DecoderPort.rs2 := io.DecoderPort.op(24, 20)
      io.DecoderPort.imm := ImmGenerator(INST_S, io.DecoderPort.op)
    }
    is(INST_B) {
      io.DecoderPort.rs1 := io.DecoderPort.op(19, 15)
      io.DecoderPort.rs2 := io.DecoderPort.op(24, 20)
      io.DecoderPort.imm := ImmGenerator(INST_B, io.DecoderPort.op)
    }
    is(INST_U) {
      io.DecoderPort.rd  := io.DecoderPort.op(11, 7)
      io.DecoderPort.imm := ImmGenerator(INST_U, io.DecoderPort.op)
    }
    is(INST_J) {
      io.DecoderPort.rd  := io.DecoderPort.op(11, 7)
      io.DecoderPort.imm := ImmGenerator(INST_J, io.DecoderPort.op)
    }
  }

  /** ImmGenerator generates the immadiate value depending on the instruction type
    *
    * @param regType
    *   is the instruction type from `Constants.scala`
    * @return
    *   the imm value
    */
  def ImmGenerator(regType: InstructionType.Type, inst: UInt): SInt = regType match {
    case INST_R => 0.S
    case INST_I => Cat(Fill(20, inst(31)), inst(31, 20)).asSInt()
    case INST_S => Cat(Fill(20, inst(31)), inst(31, 25), inst(11, 7)).asSInt()
    case INST_B => Cat(Fill(19, inst(31)), inst(31), inst(7), inst(30, 25), inst(11, 8), 0.U(1.W)).asSInt()
    case INST_U => Cat(inst(31, 12), Fill(12, 0.U)).asSInt()
    case INST_J =>
      Cat(Fill(11, inst(31)), inst(31), inst(19, 12), inst(20), inst(30, 25), inst(24, 21), 0.U(1.W)).asSInt()
    case INST_Z => Cat(Fill(27, 0.U), inst(19, 15)).asSInt() // for csri
    case _      => 0.S
  }
}
