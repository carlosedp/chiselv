package chiselv

import chisel3._
import chiseltest._
import com.carlosedp.riscvassembler.ObjectUtils.NumericManipulation
import org.scalatest._

import Instruction._
import flatspec._
import matchers._

class ALUSpec extends AnyFlatSpec with ChiselScalatestTester with should.Matchers {
  val one        = BigInt(1)
  val max        = (one << 32) - one
  val min_signed = one << 32 - 1
  val max_signed = (one << 32 - 1) - one
  val cases =
    Array[BigInt](1, 2, 4, 123, -1, -2, -4, 0, 0x7fffffffL, 0x80000000L, max, min_signed, max_signed) ++ Seq.fill(10)(
      BigInt(scala.util.Random.nextInt()),
    )

  behavior of "ALU"

  it should "ADD" in {
    test(new ALU) { c =>
      testCycle(c, ADD)
    }
  }
  it should "SUB" in {
    test(new ALU) { c =>
      testCycle(c, SUB)
    }
  }
  it should "AND" in {
    test(new ALU) { c =>
      testCycle(c, AND)
    }
  }
  it should "OR" in {
    test(new ALU) { c =>
      testCycle(c, OR)
    }
  }
  it should "XOR" in {
    test(new ALU) { c =>
      testCycle(c, XOR)
    }
  }
  it should "SRA" in {
    test(new ALU) { c =>
      testCycle(c, SRA)
    }
  }
  it should "SRL" in {
    test(new ALU) { c =>
      testCycle(c, SRL)
    }
  }
  it should "SLL" in {
    test(new ALU) { c =>
      testCycle(c, SLL)
    }
  }
  it should "SLT" in {
    test(new ALU) { c =>
      testCycle(c, SLT)
    }
  }
  it should "SLTU" in {
    test(new ALU) { c =>
      testCycle(c, SLTU)
    }
  }
  it should "EQ" in {
    test(new ALU) { c =>
      testCycle(c, EQ)
    }
  }
  it should "NEQ" in {
    test(new ALU) { c =>
      testCycle(c, NEQ)
    }
  }
  it should "GT" in {
    test(new ALU) { c =>
      testCycle(c, GTE)
    }
  }
  it should "GTU" in {
    test(new ALU) { c =>
      testCycle(c, GTEU)
    }
  }
  // --------------------- Test Helpers ---------------------
  def aluHelper(
    a:  BigInt,
    b:  BigInt,
    op: Type,
  ): BigInt =
    op match {
      case ADD  => (a + b).to32Bit
      case SUB  => a - b
      case AND  => a & b
      case OR   => a | b
      case XOR  => a ^ b
      case SRA  => a.toInt >> (b.toInt & 0x1f)
      case SRL  => a.toInt >>> b.toInt
      case SLL  => a.toInt << b.toInt
      case SLT  => if (a.toInt < b.toInt) 1 else 0
      case SLTU => if (a.to32Bit < b.to32Bit) 1 else 0
      case EQ   => if (a.to32Bit == b.to32Bit) 1 else 0
      case NEQ  => if (a.to32Bit != b.to32Bit) 1 else 0
      case GTE  => if (a.toInt >= b.toInt) 1 else 0
      case GTEU => if (a.to32Bit >= b.to32Bit) 1 else 0
      case _    => 0 // Never happens
    }

  def testDut(
    i:   BigInt,
    j:   BigInt,
    out: BigInt,
    op:  Type,
    dut: ALU,
  ) = {
    // print(s"Inputs: $i $op $j | Test result should be ${aluHelper(i, j, op)} | ")
    dut.io.ALUPort.inst.poke(op)
    dut.io.ALUPort.a.poke(i.to32Bit)
    dut.io.ALUPort.b.poke(j.to32Bit)
    dut.clock.step()
    dut.io.ALUPort.x.peekInt() should be(out)
  }
  def testCycle(
    dut: ALU,
    op:  Type,
  ) =
    cases.foreach { i =>
      cases.foreach { j =>
        testDut(i, j, aluHelper(i, j, op).to32Bit, op, dut)
      }
    }

  def toUInt(
    i: BigInt,
  ) = i.to32Bit.asUInt(32.W)
}
