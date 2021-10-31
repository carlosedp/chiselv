package chiselv

import chisel3._
import chiseltest._
import org.scalatest._

import Instruction._
import flatspec._
import matchers._

class DecoderSpec extends AnyFlatSpec with ChiselScalatestTester with should.Matchers {
  behavior of "Decoder"

  it should "Decode an ADD instruction (type R)" in {
    test(new Decoder()) { c =>
      //  Template: "b0000000??????????000?????0110011"
      val genInst = InstructionHelper(ADD, 1, 2, 3, 0)
      c.io.DecoderPort.op.poke(genInst.U)
      c.clock.step()
      validateResult(c, ADD, 1, 2, 3, 0, true.B, false.B)
    }
  }
  it should "Decode an ANDI instruction (type I)" in {
    test(new Decoder()) { c =>
      // ?????????????????000?????0010011
      val genInst = InstructionHelper(ANDI, 7, 24, 0, -1)
      c.io.DecoderPort.op.poke(genInst.U)
      c.clock.step()
      validateResult(c, ANDI, 7, 24, 0, -1, true.B, false.B)
    }
  }
  it should "Decode an SB instruction (type S)" in {
    test(new Decoder()) { c =>
      //  Template: b?????????????????000?????0100011
      c.io.DecoderPort.op.poke("b11111010101010101000011110100011".U)
      c.clock.step()
      validateResult(c, SB, 0, 21, 10, -81, false.B, false.B)
    }
  }
  it should "Decode an BEQ instruction (type B)" in {
    test(new Decoder()) { c =>
      //  Template: b?????????????????000?????1100011
      // 11111111111111111111 1 010101 0101 0
      c.io.DecoderPort.op.poke("b10101010101010101000010111100011".U)
      c.clock.step()
      validateResult(c, BEQ, 0, 21, 10, -1366, false.B, true.B)
    }
  }
  it should "Decode an LUI instruction (type U)" in {
    test(new Decoder()) { c =>
      //  Template: b?????????????????????????0110111
      c.io.DecoderPort.op.poke("b10101010101010101010101110110111".U)
      c.clock.step()
      c.io.DecoderPort.inst.expect(LUI)
      c.io.DecoderPort.rd.peek().litValue should be(23)
      c.io.DecoderPort.rs1.peek().litValue should be(0)
      c.io.DecoderPort.rs2.peek().litValue should be(0)
      c.io.DecoderPort.imm.peek().litValue should be(2863308800L)
      c.io.DecoderPort.toALU.expect(false.B)
      c.io.DecoderPort.branch.expect(false.B)
    }
  }
  it should "Decode an JAL instruction (type J)" in {
    test(new Decoder()) { c =>
      //  Template: b?????????????????????????1101111
      // 11111111111 1 01010101 0 1010101010 0
      c.io.DecoderPort.op.poke("b11010101010001010101101011101111".U)
      c.clock.step()
      validateResult(c, JAL, 21, 0, 0, -699052, false.B, false.B)
    }
  }
  // --------------------- Test Helpers ---------------------
  def validateResult(
    c: Decoder,
    inst: Instruction.Type,
    rd: Int,
    rs1: Int,
    rs2: Int,
    imm: Int,
    toALU: Bool,
    branch: Bool,
  ) = {
    c.io.DecoderPort.inst.expect(inst)
    c.io.DecoderPort.rd.peek().litValue should be(rd)
    c.io.DecoderPort.rs1.peek().litValue should be(rs1)
    c.io.DecoderPort.rs2.peek().litValue should be(rs2)
    c.io.DecoderPort.imm.peek().litValue.toLong.toBinaryString should be(imm.toBinaryString)
    c.io.DecoderPort.toALU.expect(toALU)
    c.io.DecoderPort.branch.expect(branch)
  }

  def InstructionHelper(inst: Instruction.Type, rd: Int, rs1: Int, rs2: Int, imm: Long): String =
    inst match {
      case ADD =>
        String.format(
          "b0000000%05d%05d000%05d0110011",
          rs2.toBinaryString.toInt,
          rs1.toBinaryString.toInt,
          rd.toBinaryString.toInt,
        )
      case ANDI =>
        String.format(
          "b%12s%05d111%05d0010011",
          imm.toBinaryString.takeRight(12),
          rs1.toBinaryString.toInt,
          rd.toBinaryString.toInt,
        )
    }
}
