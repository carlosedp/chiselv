package chiselv

import chisel3.experimental.ChiselEnum

object Instruction extends ChiselEnum {
  val ERR_INST,
  // RV32I
  ADD, ADDI, SUB, LUI, AUIPC,                  // Arithmetic
  SLL, SLLI, SRL, SRLI, SRA, SRAI,             // Shifts
  AND, ANDI, OR, ORI, XOR, XORI,               // Logical
  SLT, SLTI, SLTU, SLTIU,                      // Compare
  BEQ, BNE, BLT, BGE, BLTU, BGEU,              // Branches
  JAL, JALR,                                   // Jump & Link
  FENCE, FENCEI,                               // Sync
  ECALL, EBREAK,                               // Environment
  CSRRW, CSRRS, CSRRC, CSRRWI, CSRRSI, CSRRCI, // CSR
  LB, LH, LBU, LHU, LW,                        // Loads
  SB, SH, SW,                                  // Stores
  EQ, NEQ, GTE, GTEU                           // Not instructions but auxiliaries
  = Value
}

object InstructionType extends ChiselEnum {
  val IN_ERR, INST_R, INST_I, INST_S, INST_B, INST_U, INST_J, INST_Z = Value
}
