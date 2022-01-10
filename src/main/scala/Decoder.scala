package chiselv

import chisel3._
import chisel3.util._
import chisel3.util.experimental.decode
import chiselv.Instruction._
import chiselv.InstructionType._

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

  class DecType extends Bundle {
    val inst_type = InstructionType()
    val inst      = Instruction()
    val to_alu    = Bool()
    val branch    = Bool()
    val use_imm   = Bool()
    val jump      = Bool()
    val is_load   = Bool()
    val is_store  = Bool()
  }

  val signals = decode.decodeAs(
    (new DecType),
    io.DecoderPort.op,
    decode.TruthTable(
        // format: off
        Array(
        /*                                                inst_type,   ##    inst        ##  to_alu    ##  branch    ##  use_imm   ##   jump     ##  is_load   ## is_store */
        // Arithmetic
        BitPat("b0000000??????????000?????0110011")  -> BitPat(INST_R) ## BitPat(ADD)    ## BitPat.Y() ## BitPat.N() ## BitPat.N() ## BitPat.N() ## BitPat.N() ## BitPat.N(),
        BitPat("b?????????????????000?????0010011")  -> BitPat(INST_I) ## BitPat(ADDI)   ## BitPat.Y() ## BitPat.N() ## BitPat.Y() ## BitPat.N() ## BitPat.N() ## BitPat.N(),
        BitPat("b0100000??????????000?????0110011")  -> BitPat(INST_R) ## BitPat(SUB)    ## BitPat.Y() ## BitPat.N() ## BitPat.N() ## BitPat.N() ## BitPat.N() ## BitPat.N(),
        BitPat("b?????????????????????????0110111")  -> BitPat(INST_U) ## BitPat(LUI)    ## BitPat.N() ## BitPat.N() ## BitPat.Y() ## BitPat.N() ## BitPat.N() ## BitPat.N(),
        BitPat("b?????????????????????????0010111")  -> BitPat(INST_U) ## BitPat(AUIPC)  ## BitPat.N() ## BitPat.N() ## BitPat.Y() ## BitPat.N() ## BitPat.N() ## BitPat.N(),
        // Shifts
        BitPat("b0000000??????????001?????0110011")  -> BitPat(INST_R) ## BitPat(SLL)    ## BitPat.Y() ## BitPat.N() ## BitPat.N() ## BitPat.N() ## BitPat.N() ## BitPat.N(),
        BitPat("b0000000??????????001?????0010011")  -> BitPat(INST_I) ## BitPat(SLLI)   ## BitPat.Y() ## BitPat.N() ## BitPat.Y() ## BitPat.N() ## BitPat.N() ## BitPat.N(),
        BitPat("b0000000??????????101?????0110011")  -> BitPat(INST_R) ## BitPat(SRL)    ## BitPat.Y() ## BitPat.N() ## BitPat.N() ## BitPat.N() ## BitPat.N() ## BitPat.N(),
        BitPat("b0000000??????????101?????0010011")  -> BitPat(INST_I) ## BitPat(SRLI)   ## BitPat.Y() ## BitPat.N() ## BitPat.Y() ## BitPat.N() ## BitPat.N() ## BitPat.N(),
        BitPat("b0100000??????????101?????0110011")  -> BitPat(INST_R) ## BitPat(SRA)    ## BitPat.Y() ## BitPat.N() ## BitPat.N() ## BitPat.N() ## BitPat.N() ## BitPat.N(),
        BitPat("b0100000??????????101?????0010011")  -> BitPat(INST_I) ## BitPat(SRAI)   ## BitPat.Y() ## BitPat.N() ## BitPat.Y() ## BitPat.N() ## BitPat.N() ## BitPat.N(),
        // Logical
        BitPat("b0000000??????????100?????0110011")  -> BitPat(INST_R) ## BitPat(XOR)    ## BitPat.Y() ## BitPat.N() ## BitPat.N() ## BitPat.N() ## BitPat.N() ## BitPat.N(),
        BitPat("b?????????????????100?????0010011")  -> BitPat(INST_I) ## BitPat(XORI)   ## BitPat.Y() ## BitPat.N() ## BitPat.Y() ## BitPat.N() ## BitPat.N() ## BitPat.N(),
        BitPat("b0000000??????????110?????0110011")  -> BitPat(INST_R) ## BitPat(OR)     ## BitPat.Y() ## BitPat.N() ## BitPat.N() ## BitPat.N() ## BitPat.N() ## BitPat.N(),
        BitPat("b?????????????????110?????0010011")  -> BitPat(INST_I) ## BitPat(ORI)    ## BitPat.Y() ## BitPat.N() ## BitPat.Y() ## BitPat.N() ## BitPat.N() ## BitPat.N(),
        BitPat("b0000000??????????111?????0110011")  -> BitPat(INST_R) ## BitPat(AND)    ## BitPat.Y() ## BitPat.N() ## BitPat.N() ## BitPat.N() ## BitPat.N() ## BitPat.N(),
        BitPat("b?????????????????111?????0010011")  -> BitPat(INST_I) ## BitPat(ANDI)   ## BitPat.Y() ## BitPat.N() ## BitPat.Y() ## BitPat.N() ## BitPat.N() ## BitPat.N(),
        // Compare
        BitPat("b0000000??????????010?????0110011")  -> BitPat(INST_R) ## BitPat(SLT)    ## BitPat.Y() ## BitPat.N() ## BitPat.N() ## BitPat.N() ## BitPat.N() ## BitPat.N(),
        BitPat("b?????????????????010?????0010011")  -> BitPat(INST_I) ## BitPat(SLTI)   ## BitPat.Y() ## BitPat.N() ## BitPat.Y() ## BitPat.N() ## BitPat.N() ## BitPat.N(),
        BitPat("b0000000??????????011?????0110011")  -> BitPat(INST_R) ## BitPat(SLTU)   ## BitPat.Y() ## BitPat.N() ## BitPat.N() ## BitPat.N() ## BitPat.N() ## BitPat.N(),
        BitPat("b?????????????????011?????0010011")  -> BitPat(INST_I) ## BitPat(SLTIU)  ## BitPat.Y() ## BitPat.N() ## BitPat.Y() ## BitPat.N() ## BitPat.N() ## BitPat.N(),
        // Branches
        BitPat("b?????????????????000?????1100011")  -> BitPat(INST_B) ## BitPat(BEQ)    ## BitPat.N() ## BitPat.Y() ## BitPat.Y() ## BitPat.N() ## BitPat.N() ## BitPat.N(),
        BitPat("b?????????????????001?????1100011")  -> BitPat(INST_B) ## BitPat(BNE)    ## BitPat.N() ## BitPat.Y() ## BitPat.Y() ## BitPat.N() ## BitPat.N() ## BitPat.N(),
        BitPat("b?????????????????100?????1100011")  -> BitPat(INST_B) ## BitPat(BLT)    ## BitPat.N() ## BitPat.Y() ## BitPat.Y() ## BitPat.N() ## BitPat.N() ## BitPat.N(),
        BitPat("b?????????????????101?????1100011")  -> BitPat(INST_B) ## BitPat(BGE)    ## BitPat.N() ## BitPat.Y() ## BitPat.Y() ## BitPat.N() ## BitPat.N() ## BitPat.N(),
        BitPat("b?????????????????110?????1100011")  -> BitPat(INST_B) ## BitPat(BLTU)   ## BitPat.N() ## BitPat.Y() ## BitPat.Y() ## BitPat.N() ## BitPat.N() ## BitPat.N(),
        BitPat("b?????????????????111?????1100011")  -> BitPat(INST_B) ## BitPat(BGEU)   ## BitPat.N() ## BitPat.Y() ## BitPat.Y() ## BitPat.N() ## BitPat.N() ## BitPat.N(),
        // Jump & link
        BitPat("b?????????????????????????1101111")  -> BitPat(INST_J) ## BitPat(JAL)    ## BitPat.N() ## BitPat.N() ## BitPat.Y() ## BitPat.Y() ## BitPat.N() ## BitPat.N(),
        BitPat("b?????????????????000?????1100111")  -> BitPat(INST_I) ## BitPat(JALR)   ## BitPat.N() ## BitPat.N() ## BitPat.Y() ## BitPat.Y() ## BitPat.N() ## BitPat.N(),
        // Sync
        BitPat("b0000????????00000000000000001111")  -> BitPat(INST_I) ## BitPat(FENCE)  ## BitPat.N() ## BitPat.N() ## BitPat.N() ## BitPat.N() ## BitPat.N() ## BitPat.N(),
        BitPat("b00000000000000000000001000001111")  -> BitPat(INST_I) ## BitPat(FENCEI) ## BitPat.N() ## BitPat.N() ##  BitPat.Y()## BitPat.N() ## BitPat.N() ## BitPat.N(),
        // Environment
        BitPat("b00000000000000000000000001110011")  -> BitPat(INST_I) ## BitPat(ECALL)  ## BitPat.N() ## BitPat.N() ## BitPat.N() ## BitPat.N() ## BitPat.N() ## BitPat.N(),
        BitPat("b00000000000100000000000001110011")  -> BitPat(INST_I) ## BitPat(EBREAK) ## BitPat.N() ## BitPat.N() ## BitPat.N() ## BitPat.N() ## BitPat.N() ## BitPat.N(),
        // CSR
        BitPat("b?????????????????001?????1110011")  -> BitPat(INST_I) ## BitPat(CSRRW)  ## BitPat.N() ## BitPat.N() ## BitPat.N() ## BitPat.N() ## BitPat.N() ## BitPat.N(),
        BitPat("b?????????????????010?????1110011")  -> BitPat(INST_I) ## BitPat(CSRRS)  ## BitPat.N() ## BitPat.N() ## BitPat.N() ## BitPat.N() ## BitPat.N() ## BitPat.N(),
        BitPat("b?????????????????011?????1110011")  -> BitPat(INST_I) ## BitPat(CSRRC)  ## BitPat.N() ## BitPat.N() ## BitPat.N() ## BitPat.N() ## BitPat.N() ## BitPat.N(),
        BitPat("b?????????????????101?????1110011")  -> BitPat(INST_I) ## BitPat(CSRRWI) ## BitPat.N() ## BitPat.N() ## BitPat.Y() ## BitPat.N() ## BitPat.N() ## BitPat.N(),
        BitPat("b?????????????????110?????1110011")  -> BitPat(INST_I) ## BitPat(CSRRSI) ## BitPat.N() ## BitPat.N() ## BitPat.Y() ## BitPat.N() ## BitPat.N() ## BitPat.N(),
        BitPat("b?????????????????111?????1110011")  -> BitPat(INST_I) ## BitPat(CSRRCI) ## BitPat.N() ## BitPat.N() ## BitPat.Y() ## BitPat.N() ## BitPat.N() ## BitPat.N(),
        // Loads
        BitPat("b?????????????????000?????0000011")  -> BitPat(INST_I) ## BitPat(LB)     ## BitPat.N() ## BitPat.N() ## BitPat.Y() ## BitPat.N() ## BitPat.Y() ## BitPat.N(),
        BitPat("b?????????????????001?????0000011")  -> BitPat(INST_I) ## BitPat(LH)     ## BitPat.N() ## BitPat.N() ## BitPat.Y() ## BitPat.N() ## BitPat.Y() ## BitPat.N(),
        BitPat("b?????????????????100?????0000011")  -> BitPat(INST_I) ## BitPat(LBU)    ## BitPat.N() ## BitPat.N() ## BitPat.Y() ## BitPat.N() ## BitPat.Y() ## BitPat.N(),
        BitPat("b?????????????????101?????0000011")  -> BitPat(INST_I) ## BitPat(LHU)    ## BitPat.N() ## BitPat.N() ## BitPat.Y() ## BitPat.N() ## BitPat.Y() ## BitPat.N(),
        BitPat("b?????????????????010?????0000011")  -> BitPat(INST_I) ## BitPat(LW)     ## BitPat.N() ## BitPat.N() ## BitPat.Y() ## BitPat.N() ## BitPat.Y() ## BitPat.N(),
        // Stores
        BitPat("b?????????????????000?????0100011")  -> BitPat(INST_S) ## BitPat(SB)     ## BitPat.N() ## BitPat.N() ## BitPat.Y() ## BitPat.N() ## BitPat.N() ## BitPat.Y(),
        BitPat("b?????????????????001?????0100011")  -> BitPat(INST_S) ## BitPat(SH)     ## BitPat.N() ## BitPat.N() ## BitPat.Y() ## BitPat.N() ## BitPat.N() ## BitPat.Y(),
        BitPat("b?????????????????010?????0100011")  -> BitPat(INST_S) ## BitPat(SW)     ## BitPat.N() ## BitPat.N() ## BitPat.Y() ## BitPat.N() ## BitPat.N() ## BitPat.Y()
      ),                                                BitPat(IN_ERR) ## BitPat(ERR)    ## BitPat.dontCare(6) // Default values
      // format: on
    )
  )
  // .asTypeOf(new DecType)

  io.DecoderPort.rd  := io.DecoderPort.op(11, 7)
  io.DecoderPort.rs1 := io.DecoderPort.op(19, 15)
  io.DecoderPort.rs2 := io.DecoderPort.op(24, 20)
  io.DecoderPort.imm := Mux1H(
    signals.inst_type,
    Seq(
      ImmGenerator(INST_I, io.DecoderPort.op),
      ImmGenerator(INST_S, io.DecoderPort.op),
      ImmGenerator(INST_B, io.DecoderPort.op),
      ImmGenerator(INST_U, io.DecoderPort.op),
      ImmGenerator(INST_J, io.DecoderPort.op),
      ImmGenerator(INST_Z, io.DecoderPort.op),
      0.S, // padding for InstructionType
      0.S  // padding for InstructionType
    )
  )

  io.DecoderPort.inst     := signals.inst
  io.DecoderPort.toALU    := signals.to_alu
  io.DecoderPort.branch   := signals.branch
  io.DecoderPort.use_imm  := signals.use_imm
  io.DecoderPort.jump     := signals.jump
  io.DecoderPort.is_load  := signals.is_load
  io.DecoderPort.is_store := signals.is_store

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
