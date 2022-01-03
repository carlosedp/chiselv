package chiselv

import chisel3._
import chiseltest._
import com.carlosedp.scalautils.riscvassembler._
import org.scalatest._

import Instruction._
import flatspec._
import matchers._

class DecoderSpec extends AnyFlatSpec with ChiselScalatestTester with should.Matchers {
  behavior of "Decoder"

  it should "Decode an ADD instruction (type R)" in {
    test(new Decoder()) { c =>
      //  Template: "b0000000??????????000?????0110011"
      c.io.DecoderPort.op.poke(makeBin("add x1, x2, x3"))
      c.clock.step()
      validateResult(c, ADD, 1, 2, 3, 0, true.B, false.B)
    }
  }
  it should "Decode an ANDI instruction (type I)" in {
    test(new Decoder()) { c =>
      // ?????????????????000?????0010011
      c.io.DecoderPort.op.poke(makeBin("andi x7, x24, -1"))
      c.clock.step()
      validateResult(c, ANDI, 7, 24, 0, -1, true.B, false.B)
    }
  }
  it should "Decode an SB instruction (type S)" in {
    test(new Decoder()) { c =>
      //  Template: b?????????????????000?????0100011
      c.io.DecoderPort.op.poke(makeBin("sb x10, -81(x21)"))
      c.clock.step()
      validateResult(c, SB, 0, 21, 10, -81, false.B, false.B)
    }
  }
  it should "Decode an BEQ instruction (type B)" in {
    test(new Decoder()) { c =>
      c.io.DecoderPort.op.poke(makeBin("beq x21, x10, -1366"))
      c.clock.step()
      validateResult(c, BEQ, 0, 21, 10, -1366, false.B, true.B)
    }
  }
  it should "Decode an LUI instruction (type U)" in {
    test(new Decoder()) { c =>
      //  Template: b?????????????????????????0110111
      c.io.DecoderPort.op.poke(makeBin("lui x23, -1431658496"))
      c.clock.step()
      c.io.DecoderPort.inst.expect(LUI)
      c.io.DecoderPort.rd.peek().litValue should be(23)
      c.io.DecoderPort.rs1.peek().litValue should be(0)
      c.io.DecoderPort.rs2.peek().litValue should be(0)
      c.io.DecoderPort.imm.peek().litValue should be(-1431658496L)
      c.io.DecoderPort.toALU.expect(false.B)
      c.io.DecoderPort.branch.expect(false.B)
    }
  }
  it should "Decode an JAL instruction (type J)" in {
    test(new Decoder()) { c =>
      //  Template: b?????????????????????????1101111
      c.io.DecoderPort.op.poke(makeBin("jal x21, -699052"))
      c.clock.step()
      validateResult(c, JAL, 21, 0, 0, -699052, false.B, false.B)
    }
  }

  // --------------------- Test Helpers ---------------------

  def makeBin(in: String): UInt = {
    val bin = RISCVAssembler.binOutput(in)
    ("b" + bin).U
  }

  def validateResult(
    c:      Decoder,
    inst:   Instruction.Type,
    rd:     Int,
    rs1:    Int,
    rs2:    Int,
    imm:    Int,
    toALU:  Bool,
    branch: Bool
  ) = {
    c.io.DecoderPort.inst.expect(inst)
    c.io.DecoderPort.rd.peek().litValue should be(rd)
    c.io.DecoderPort.rs1.peek().litValue should be(rs1)
    c.io.DecoderPort.rs2.peek().litValue should be(rs2)
    c.io.DecoderPort.imm.peek().litValue should be(imm)
    c.io.DecoderPort.toALU.expect(toALU)
    c.io.DecoderPort.branch.expect(branch)
  }

}
