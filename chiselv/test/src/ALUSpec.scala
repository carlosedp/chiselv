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
      BigInt(scala.util.Random.nextInt())
    )

  behavior of "ALU"

  it should "ADD" in {
    testCycle(ADD)
  }
  it should "SUB" in {
    testCycle(SUB)
  }
  it should "AND" in {
    testCycle(AND)
  }
  it should "OR" in {
    testCycle(OR)
  }
  it should "XOR" in {
    testCycle(XOR)
  }
  it should "SRA" in {
    testCycle(SRA)
  }
  it should "SRL" in {
    testCycle(SRL)
  }
  it should "SLL" in {
    testCycle(SLL)
  }
  it should "SLT" in {
    testCycle(SLT)
  }
  it should "SLTU" in {
    testCycle(SLTU)
  }
  it should "EQ" in {
    testCycle(EQ)
  }
  it should "NEQ" in {
    testCycle(NEQ)
  }
  it should "GT" in {
    testCycle(GTE)
  }
  it should "GTU" in {
    testCycle(GTEU)
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
      op: Type
    ) =
    test(new ALU) { c =>
      cases.foreach { i =>
        cases.foreach { j =>
          testDut(i, j, aluHelper(i, j, op).to32Bit, op, c)
        }
      }
    }

  def toUInt(
      i: BigInt
    ) = i.to32Bit.asUInt(32.W)
}
