import Instruction._
import InstructionPattern._
import InstructionType._
import chisel3._
import chisel3.util._

class DecoderPort(bitWidth: Int = 32) extends Bundle {
  val op     = Output(UInt(bitWidth.W)) // Op is the 32 bit instruction read received for decoding
  val inst   = Input(Instruction())     // Instruction is the decoded instruction
  val rd     = Input(UInt(5.W))         // Rd is the 5 bit destiny register
  val rs1    = Input(UInt(5.W))         // Rs1 is the 5 bit source register 1
  val rs2    = Input(UInt(5.W))         // Rs2 is the 5 bit source register 2
  val imm    = Input(UInt(bitWidth.W))  // Imm is the 32 bit immediate
  val toALU  = Input(Bool())            // ToALU is a flag to indicate if the instruction is to be executed in the ALU
  val branch = Input(Bool())            // Branch is a flag to indicate if the instruction should jump and link. Update PC
}

class Decoder(bitWidth: Int = 32) extends Module {
  val io = IO(new Bundle {
    val DecoderPort = Flipped(new DecoderPort(bitWidth))
  })

  val signals =
    ListLookup(
      io.DecoderPort.op,
      // format: off
      List(                 IN_ERR, ERR_INST, false.B,  false.B), // Default values
      Array(
        /*                  inst_type,  inst   to_alu   branch   */
        // Arithmetic
        ADD_PAT     -> List(INST_R,      ADD,  true.B,  false.B),
        ADDI_PAT    -> List(INST_I,     ADDI,  true.B,  false.B),
        SUB_PAT     -> List(INST_R,      SUB,  true.B,  false.B),
        LUI_PAT     -> List(INST_U,      LUI, false.B,  false.B),
        AUIPC_PAT   -> List(INST_U,    AUIPC, false.B,  false.B),
        // Shifts
        SLL_PAT     -> List(INST_R,      SLL,  true.B,  false.B),
        SLLI_PAT    -> List(INST_I,     SLLI,  true.B,  false.B),
        SRL_PAT     -> List(INST_R,      SRL,  true.B,  false.B),
        SRLI_PAT    -> List(INST_I,     SRLI,  true.B,  false.B),
        SRA_PAT     -> List(INST_R,      SRA,  true.B,  false.B),
        SRAI_PAT    -> List(INST_I,     SRAI,  true.B,  false.B),
        // Logical
        XOR_PAT     -> List(INST_R,      XOR, true.B,   false.B),
        XORI_PAT    -> List(INST_I,     XORI, true.B,   false.B),
        OR_PAT      -> List(INST_R,       OR, true.B,   false.B),
        ORI_PAT     -> List(INST_I,      ORI, true.B,   false.B),
        AND_PAT     -> List(INST_R,      AND, true.B,   false.B),
        ANDI_PAT    -> List(INST_I,     ANDI, true.B,   false.B),
        // Compare
        SLT_PAT     -> List(INST_R,     SLT,  true.B,   false.B),
        SLTI_PAT    -> List(INST_I,    SLTI,  true.B,   false.B),
        SLTU_PAT    -> List(INST_R,    SLTU,  true.B,   false.B),
        SLTIU_PAT   -> List(INST_I,   SLTIU,  true.B,   false.B),
        // Branches
        BEQ_PAT     -> List(INST_B,     BEQ, false.B,    true.B),
        BNE_PAT     -> List(INST_B,     BNE, false.B,    true.B),
        BLT_PAT     -> List(INST_B,     BLT, false.B,    true.B),
        BGE_PAT     -> List(INST_B,     BGE, false.B,    true.B),
        BLTU_PAT    -> List(INST_B,    BLTU, false.B,    true.B),
        BGEU_PAT    -> List(INST_B,    BGEU, false.B,    true.B),
        // Jump & link
        JAL_PAT     -> List(INST_J,     JAL, false.B,   false.B),
        JALR_PAT    -> List(INST_I,    JALR, false.B,   false.B),
        // Sync
        FENCE_PAT   -> List(INST_I,   FENCE, false.B,   false.B),
        FENCEI_PAT  -> List(INST_I,  FENCEI, false.B,   false.B),
        // Environment
        ECALL_PAT   -> List(INST_I,   ECALL, false.B,   false.B),
        EBREAK_PAT  -> List(INST_I,  EBREAK, false.B,   false.B),
        // CSR
        CSRRW_PAT   -> List(INST_I,   CSRRW, false.B,   false.B),
        CSRRS_PAT   -> List(INST_I,   CSRRS, false.B,   false.B),
        CSRRC_PAT   -> List(INST_I,   CSRRC, false.B,   false.B),
        CSRRWI_PAT  -> List(INST_I,  CSRRWI, false.B,   false.B),
        CSRRSI_PAT  -> List(INST_I,  CSRRSI, false.B,   false.B),
        CSRRCI_PAT  -> List(INST_I,  CSRRCI, false.B,   false.B),
        // Loads
        LB_PAT      -> List(INST_I,      LB, false.B,   false.B),
        LH_PAT      -> List (INST_I,     LH, false.B,   false.B),
        LBU_PAT     -> List (INST_I,    LBU, false.B,   false.B),
        LHU_PAT     -> List (INST_I,    LHU, false.B,   false.B),
        LW_PAT      -> List (INST_I,     LW, false.B,   false.B),
        // Stores
        SB_PAT      -> List (INST_S,     SB, false.B,   false.B),
        SH_PAT      -> List (INST_S,     SH, false.B,   false.B),
        SW_PAT      -> List (INST_S,     SW, false.B,   false.B),
      )
    ) // format: on

  io.DecoderPort.rd := 0.U
  io.DecoderPort.rs1 := 0.U
  io.DecoderPort.rs2 := 0.U
  io.DecoderPort.imm := 0.U
  io.DecoderPort.inst := signals(1)
  io.DecoderPort.toALU := signals(2)
  io.DecoderPort.branch := signals(3)
  switch(signals(0)) {
    is(INST_R) {
      io.DecoderPort.rd := io.DecoderPort.op(11, 7)
      io.DecoderPort.rs1 := io.DecoderPort.op(19, 15)
      io.DecoderPort.rs2 := io.DecoderPort.op(24, 20)
    }
    is(INST_I) {
      io.DecoderPort.rd := io.DecoderPort.op(11, 7)
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
      io.DecoderPort.rd := io.DecoderPort.op(11, 7)
      io.DecoderPort.imm := ImmGenerator(INST_U, io.DecoderPort.op)
    }
    is(INST_J) {
      io.DecoderPort.rd := io.DecoderPort.op(11, 7)
      io.DecoderPort.imm := ImmGenerator(INST_J, io.DecoderPort.op)
    }
  }

  /**
   * ImmGenerator generates the immadiate value depending on the instruction type
   *
   * @param regType is the instruction type from `Constants.scala`
   * @return the imm value
   */
  def ImmGenerator(regType: InstructionType.Type, inst: UInt): UInt = regType match {
    case INST_R => 0.U
    case INST_I => inst(31, 20)
    case INST_S => Cat(inst(31, 25), inst(11, 7))
    case INST_B =>
      Cat(
        inst(31),
        inst(7),
        inst(30, 25),
        inst(11, 8),
        0.U(1.W)
      )
    case INST_U => Cat(inst(31, 12), 0.U(12.W))
    case INST_J =>
      Cat(
        inst(31),
        inst(19, 12),
        inst(20),
        inst(30, 25),
        inst(24, 21),
        0.U(1.W)
      )
    case INST_Z => inst(19, 15).zext.asUInt()
    case _      => 0.U
  }
}
