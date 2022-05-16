package chiselv

import chisel3._
import chiseltest._
import com.carlosedp.scalautils.riscvassembler._
import org.scalatest._

import Instruction._
import flatspec._
import matchers._

class DecoderSpec extends AnyFlatSpec with ChiselScalatestTester with should.Matchers {
  behavior of "Decoder - Aritmetic"

  it should "Decode an ADD instruction (type R)" in {
    test(new Decoder) { c =>
      c.io.DecoderPort.op.poke(makeBin("add x1, x2, x3"))
      c.clock.step()
      validateResult(c, ADD, 1, 2, 3, 0, true, false)
    }
  }
  it should "Decode an ADDI instruction (type I)" in {
    test(new Decoder) { c =>
      c.io.DecoderPort.op.poke(makeBin("addi x1, x2, 5"))
      c.clock.step()
      validateResult(c, ADDI, 1, 2, 0, 5, true, false, true)
    }
  }
  it should "Decode an SUB instruction (type R)" in {
    test(new Decoder) { c =>
      c.io.DecoderPort.op.poke(makeBin("sub x1, x2, x3"))
      c.clock.step()
      validateResult(c, SUB, 1, 2, 3, 0, true, false)
    }
  }
  it should "Decode an LUI instruction (type U)" in {
    test(new Decoder) { c =>
      c.io.DecoderPort.op.poke(makeBin("lui x23, -1431658496"))
      c.clock.step()
      validateResult(c, LUI, 23, 0, 0, -1431658496, false, false, true)
    }
  }
  it should "Decode an AUIPC instruction (type U)" in {
    test(new Decoder) { c =>
      c.io.DecoderPort.op.poke(makeBin("auipc x23, -1431658496"))
      c.clock.step()
      validateResult(c, AUIPC, 23, 0, 0, -1431658496, false, false, true)
    }
  }

  behavior of "Decoder - Shifts"

  it should "Decode an SLL instruction (type R)" in {
    test(new Decoder) { c =>
      c.io.DecoderPort.op.poke(makeBin("sll x1, x2, x3"))
      c.clock.step()
      validateResult(c, SLL, 1, 2, 3, 0, true, false)
    }
  }
  it should "Decode an SLLI instruction (type I)" in {
    test(new Decoder) { c =>
      c.io.DecoderPort.op.poke(makeBin("slli x1, x2, 5"))
      c.clock.step()
      validateResult(c, SLLI, 1, 2, 0, 5, true, false, true)
    }
  }
  it should "Decode an SRL instruction (type R)" in {
    test(new Decoder) { c =>
      c.io.DecoderPort.op.poke(makeBin("srl x1, x2, x3"))
      c.clock.step()
      validateResult(c, SRL, 1, 2, 3, 0, true, false)
    }
  }
  it should "Decode an SRLI instruction (type I)" in {
    test(new Decoder) { c =>
      c.io.DecoderPort.op.poke(makeBin("srli x1, x2, 5"))
      c.clock.step()
      validateResult(c, SRLI, 1, 2, 0, 5, true, false, true)
    }
  }
  it should "Decode an SRA instruction (type R)" in {
    test(new Decoder) { c =>
      c.io.DecoderPort.op.poke(makeBin("sra x1, x2, x3"))
      c.clock.step()
      validateResult(c, SRA, 1, 2, 3, 0, true, false)
    }
  }
  it should "Decode an SRAI instruction (type I)" in {
    test(new Decoder) { c =>
      c.io.DecoderPort.op.poke(makeBin("srai x1, x2, 5"))
      c.clock.step()
      validateResult(c, SRLI, 1, 2, 0, 5, true, false, true)
    }
  }

  behavior of "Decoder - Logical"

  it should "Decode an AND instruction (type R)" in {
    test(new Decoder) { c =>
      c.io.DecoderPort.op.poke(makeBin("and x1, x2, x3"))
      c.clock.step()
      validateResult(c, AND, 1, 2, 3, 0, true, false)
    }
  }
  it should "Decode an ANDI instruction (type I)" in {
    test(new Decoder) { c =>
      c.io.DecoderPort.op.poke(makeBin("andi x7, x24, -1"))
      c.clock.step()
      validateResult(c, ANDI, 7, 24, 0, -1, true, false, true)
    }
  }
  it should "Decode an OR instruction (type R)" in {
    test(new Decoder) { c =>
      c.io.DecoderPort.op.poke(makeBin("or x1, x2, x3"))
      c.clock.step()
      validateResult(c, OR, 1, 2, 3, 0, true, false)
    }
  }
  it should "Decode an ORI instruction (type I)" in {
    test(new Decoder) { c =>
      c.io.DecoderPort.op.poke(makeBin("ori x7, x24, -1"))
      c.clock.step()
      validateResult(c, ORI, 7, 24, 0, -1, true, false, true)
    }
  }
  it should "Decode an XOR instruction (type R)" in {
    test(new Decoder) { c =>
      c.io.DecoderPort.op.poke(makeBin("xor x1, x2, x3"))
      c.clock.step()
      validateResult(c, XOR, 1, 2, 3, 0, true, false)
    }
  }
  it should "Decode an XORI instruction (type I)" in {
    test(new Decoder) { c =>
      c.io.DecoderPort.op.poke(makeBin("xori x7, x24, -1"))
      c.clock.step()
      validateResult(c, XORI, 7, 24, 0, -1, true, false, true)
    }
  }

  behavior of "Decoder - Compare"

  it should "Decode an SLT instruction (type R)" in {
    test(new Decoder) { c =>
      c.io.DecoderPort.op.poke(makeBin("slt x1, x2, x3"))
      c.clock.step()
      validateResult(c, SLT, 1, 2, 3, 0, true, false)
    }
  }
  it should "Decode an SLTI instruction (type I)" in {
    test(new Decoder) { c =>
      c.io.DecoderPort.op.poke(makeBin("slti x7, x24, -1"))
      c.clock.step()
      validateResult(c, SLTI, 7, 24, 0, -1, true, false, true)
    }
  }
  it should "Decode an SLTU instruction (type R)" in {
    test(new Decoder) { c =>
      c.io.DecoderPort.op.poke(makeBin("sltu x1, x2, x3"))
      c.clock.step()
      validateResult(c, SLTU, 1, 2, 3, 0, true, false)
    }
  }
  it should "Decode an SLTIU instruction (type I)" in {
    test(new Decoder) { c =>
      c.io.DecoderPort.op.poke(makeBin("sltiu x7, x24, -1"))
      c.clock.step()
      validateResult(c, SLTIU, 7, 24, 0, -1, true, false, true)
    }
  }

  behavior of "Decoder - Branches"

  it should "Decode an BEQ instruction (type B)" in {
    test(new Decoder) { c =>
      c.io.DecoderPort.op.poke(makeBin("beq x21, x10, -1366"))
      c.clock.step()
      validateResult(c, BEQ, 0, 21, 10, -1366, false, true, true)
    }
  }
  it should "Decode an BNE instruction (type B)" in {
    test(new Decoder) { c =>
      c.io.DecoderPort.op.poke(makeBin("bne x21, x10, -1366"))
      c.clock.step()
      validateResult(c, BNE, 0, 21, 10, -1366, false, true, true)
    }
  }
  it should "Decode an BLT instruction (type B)" in {
    test(new Decoder) { c =>
      c.io.DecoderPort.op.poke(makeBin("blt x21, x10, -1366"))
      c.clock.step()
      validateResult(c, BLT, 0, 21, 10, -1366, false, true, true)
    }
  }
  it should "Decode an BGE instruction (type B)" in {
    test(new Decoder) { c =>
      c.io.DecoderPort.op.poke(makeBin("bge x21, x10, -1366"))
      c.clock.step()
      validateResult(c, BGE, 0, 21, 10, -1366, false, true, true)
    }
  }
  it should "Decode an BLTU instruction (type B)" in {
    test(new Decoder) { c =>
      c.io.DecoderPort.op.poke(makeBin("bltu x21, x10, -1366"))
      c.clock.step()
      validateResult(c, BLTU, 0, 21, 10, -1366, false, true, true)
    }
  }
  it should "Decode an BGEU instruction (type B)" in {
    test(new Decoder) { c =>
      c.io.DecoderPort.op.poke(makeBin("bgeu x21, x10, -1366"))
      c.clock.step()
      validateResult(c, BGEU, 0, 21, 10, -1366, false, true, true)
    }
  }

  behavior of "Decoder - Jumps"

  it should "Decode an JAL instruction (type J)" in {
    test(new Decoder) { c =>
      c.io.DecoderPort.op.poke(makeBin("jal x21, -699052"))
      c.clock.step()
      validateResult(c, JAL, 21, 0, 0, -699052, false, false, true, true)
    }
  }
  it should "Decode an JALR instruction (type I)" in {
    test(new Decoder) { c =>
      c.io.DecoderPort.op.poke(makeBin("jalr x21, x1, 1234"))
      c.clock.step()
      validateResult(c, JALR, 21, 1, 0, 1234, false, false, true, true)
    }
  }

  behavior of "Decoder - Loads/Stores"

  it should "Decode an LB instruction (type I)" in {
    test(new Decoder) { c =>
      c.io.DecoderPort.op.poke(makeBin("lb x1, 16(x2)"))
      c.clock.step()
      validateResult(c, LB, 1, 2, 0, 16, false, false, true, false, true, false)
    }
  }
  it should "Decode an LH instruction (type I)" in {
    test(new Decoder) { c =>
      c.io.DecoderPort.op.poke(makeBin("lh x1, 16(x2)"))
      c.clock.step()
      validateResult(c, LH, 1, 2, 0, 16, false, false, true, false, true, false)
    }
  }
  it should "Decode an LBU instruction (type I)" in {
    test(new Decoder) { c =>
      c.io.DecoderPort.op.poke(makeBin("lbu x1, 16(x2)"))
      c.clock.step()
      validateResult(c, LBU, 1, 2, 0, 16, false, false, true, false, true, false)
    }
  }
  it should "Decode an LHU instruction (type I)" in {
    test(new Decoder) { c =>
      c.io.DecoderPort.op.poke(makeBin("lhu x1, 16(x2)"))
      c.clock.step()
      validateResult(c, LHU, 1, 2, 0, 16, false, false, true, false, true, false)
    }
  }
  it should "Decode an LW instruction (type I)" in {
    test(new Decoder) { c =>
      c.io.DecoderPort.op.poke(makeBin("lw x1, 16(x2)"))
      c.clock.step()
      validateResult(c, LW, 1, 2, 0, 16, false, false, true, false, true, false)
    }
  }
  it should "Decode an SB instruction (type S)" in {
    test(new Decoder) { c =>
      c.io.DecoderPort.op.poke(makeBin("sb x10, -81(x21)"))
      c.clock.step()
      validateResult(c, SB, 0, 21, 10, -81, false, false, true, false, false, true)
    }
  }
  it should "Decode an SH instruction (type S)" in {
    test(new Decoder) { c =>
      c.io.DecoderPort.op.poke(makeBin("sh x10, -81(x21)"))
      c.clock.step()
      validateResult(c, SH, 0, 21, 10, -81, false, false, true, false, false, true)
    }
  }
  it should "Decode an SW instruction (type S)" in {
    test(new Decoder) { c =>
      c.io.DecoderPort.op.poke(makeBin("sw x10, -81(x21)"))
      c.clock.step()
      validateResult(c, SW, 0, 21, 10, -81, false, false, true, false, false, true)
    }
  }

  // --------------------- Test Helpers ---------------------

  def makeBin(in: String): UInt = {
    val bin = RISCVAssembler.binOutput(in)
    ("b" + bin).U
  }

  def validateResult(
    c:       Decoder,
    inst:    Instruction.Type,
    rd:      Int,
    rs1:     Int,
    rs2:     Int,
    imm:     Int,
    toALU:   Boolean = false,
    branch:  Boolean = false,
    use_imm: Boolean = false,
    jump:    Boolean = false,
    load:    Boolean = false,
    store:   Boolean = false
  ) = {
    c.io.DecoderPort.inst.expect(inst)
    c.io.DecoderPort.rd.peekInt() should be(rd)
    c.io.DecoderPort.rs1.peekInt() should be(rs1)
    c.io.DecoderPort.rs2.peekInt() should be(rs2)
    c.io.DecoderPort.imm.peekInt() should be(imm)
    c.io.DecoderPort.toALU.peekBoolean() should be(toALU)
    c.io.DecoderPort.branch.peekBoolean() should be(branch)
    c.io.DecoderPort.use_imm.peekBoolean() should be(use_imm)
    c.io.DecoderPort.jump.peekBoolean() should be(jump)
    c.io.DecoderPort.is_load.peekBoolean() should be(load)
    c.io.DecoderPort.is_store.peekBoolean() should be(store)
  }
}
