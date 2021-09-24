import chisel3.experimental.ChiselEnum

object Instructions extends ChiselEnum {
  val
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
  SB, SH, SW                                   // Stores
  = Value
}
