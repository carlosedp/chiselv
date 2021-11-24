package chiselv

import chisel3._
import chisel3.experimental.ChiselEnum

object Instruction extends ChiselEnum {
  val ERR_INST,
  // RV32I
  ADD, ADDI, SUB, LUI, AUIPC,                  // Arithmetic
  SLL, SLLI, SRL, SRLI, SRA, SRAI,             // Shifts
  XOR, XORI, OR, ORI, AND, ANDI,               // Logical
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
  val INST_I = Value((1 << 0).U)
  val INST_S = Value((1 << 1).U)
  val INST_B = Value((1 << 2).U)
  val INST_U = Value((1 << 3).U)
  val INST_J = Value((1 << 4).U)
  val INST_Z = Value((1 << 5).U)
  val INST_R = Value((1 << 6).U)
  val IN_ERR = Value((1 << 7).U)
}
