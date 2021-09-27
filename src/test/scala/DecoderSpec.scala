import Instruction._
import chisel3._
import chiseltest._
import org.scalatest._

import flatspec._
import matchers._

class DecoderSpec extends AnyFlatSpec with ChiselScalatestTester with should.Matchers {
  "Decoder" should "Decode an ADD instruction (type R)" in {
    test(new Decoder()) { c =>
      //  Template: "b0000000??????????000?????0110011"
      c.io.DecoderPort.op.poke("b00000000000100001000000010110011".U)
      c.clock.step()
      c.io.DecoderPort.inst.expect(ADD)
      c.io.DecoderPort.rd.peek().litValue() should be(1)
      c.io.DecoderPort.rs1.peek().litValue() should be(1)
      c.io.DecoderPort.rs2.peek().litValue() should be(1)
      c.io.DecoderPort.imm.peek().litValue() should be(0)
      c.io.DecoderPort.toALU.expect(true.B)
      c.io.DecoderPort.branch.expect(false.B)
    }
  }
  it should "Decode an ANDI instruction (type I)" in {
    test(new Decoder()) { c =>
      //  Template: b?????????????????111?????0010011
      c.io.DecoderPort.op.poke("b11110000111111000111001110010011".U)
      c.clock.step()
      c.io.DecoderPort.inst.expect(ANDI)
      c.io.DecoderPort.rd.peek().litValue() should be(7)
      c.io.DecoderPort.rs1.peek().litValue() should be(24)
      c.io.DecoderPort.rs2.peek().litValue() should be(0)
      c.io.DecoderPort.imm.peek().litValue() should be(3855)
      c.io.DecoderPort.toALU.expect(true.B)
      c.io.DecoderPort.branch.expect(false.B)
    }
  }
  it should "Decode an SB instruction (type S)" in {
    test(new Decoder()) { c =>
      //  Template: b?????????????????000?????0100011
      c.io.DecoderPort.op.poke("b11111010101010101000011110100011".U)
      c.clock.step()
      c.io.DecoderPort.inst.expect(SB)
      c.io.DecoderPort.rd.peek().litValue() should be(0)
      c.io.DecoderPort.rs1.peek().litValue() should be(21)
      c.io.DecoderPort.rs2.peek().litValue() should be(10)
      c.io.DecoderPort.imm.peek().litValue() should be(4015) // 0xfaf
      c.io.DecoderPort.toALU.expect(false.B)
      c.io.DecoderPort.branch.expect(false.B)
    }
  }
  it should "Decode an BEQ instruction (type B)" in {
    test(new Decoder()) { c =>
      //  Template: b?????????????????000?????1100011
      c.io.DecoderPort.op.poke("b11111110101010101000111111100011".U)
      c.clock.step()
      c.io.DecoderPort.inst.expect(BEQ)
      c.io.DecoderPort.rd.peek().litValue() should be(0)
      c.io.DecoderPort.rs1.peek().litValue() should be(21)
      c.io.DecoderPort.rs2.peek().litValue() should be(10)
      c.io.DecoderPort.imm.peek().litValue() should be(8190)
      c.io.DecoderPort.toALU.expect(false.B)
      c.io.DecoderPort.branch.expect(true.B)
    }
  }
  it should "Decode an LUI instruction (type U)" in {
    test(new Decoder()) { c =>
      //  Template: b?????????????????????????0110111
      c.io.DecoderPort.op.poke("b10101010101010101010101110110111".U)
      c.clock.step()
      c.io.DecoderPort.inst.expect(LUI)
      c.io.DecoderPort.rd.peek().litValue() should be(23)
      c.io.DecoderPort.rs1.peek().litValue() should be(0)
      c.io.DecoderPort.rs2.peek().litValue() should be(0)
      c.io.DecoderPort.imm.peek().litValue() should be(2863308800L)
      c.io.DecoderPort.toALU.expect(false.B)
      c.io.DecoderPort.branch.expect(false.B)
    }
  }
  it should "Decode an JAL instruction (type J)" in {
    test(new Decoder()) { c =>
      //  Template: b?????????????????????????1101111
      c.io.DecoderPort.op.poke("b10101010101010101010101011101111".U)
      c.clock.step()
      c.io.DecoderPort.inst.expect(JAL)
      c.io.DecoderPort.rd.peek().litValue() should be(21)
      c.io.DecoderPort.rs1.peek().litValue() should be(0)
      c.io.DecoderPort.rs2.peek().litValue() should be(0)
      c.io.DecoderPort.imm.peek().litValue() should be(1745578L)
      c.io.DecoderPort.toALU.expect(false.B)
      c.io.DecoderPort.branch.expect(false.B)
    }
  }
}
