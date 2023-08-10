package chiselv

import scala.util.Random
import chisel3._
import chiseltest._
import org.scalatest._

import flatspec._
import matchers._

class RegisterBankSpec extends AnyFlatSpec with ChiselScalatestTester with should.Matchers {
  behavior of "RegisterBank"

  it should "have x0 equal to 0" in {
    test(new RegisterBank) { c =>
      c.io.regPort.rs1_addr.poke(0.U)
      c.io.regPort.rs1.expect(0.S)
    }
  }
  it should "not write to x0" in {
    test(new RegisterBank) { c =>
      c.io.regPort.writeEnable.poke(true)
      c.io.regPort.regwr_addr.poke(0.U)
      c.io.regPort.regwr_data.poke(123.S)
      c.clock.step()
      c.io.regPort.rs1_addr.peekInt() should be(0)
    }
  }
  it should "not write if not enabled" in {
    test(new RegisterBank) { c =>
      c.io.regPort.rs1_addr.poke(1)
      c.io.regPort.regwr_addr.poke(1)
      c.io.regPort.regwr_data.poke(123.S)
      c.clock.step()
      c.io.regPort.rs1.expect(0.S)
    }
  }
  it should "write all other registers and read values as expected" in {
    test(new RegisterBank).withAnnotations(Seq(WriteVcdAnnotation)) { c =>
      val min_signed = 1 << 32 - 1
      val max_signed = (1 << 32 - 1) - 1
      val cases = Array[BigInt](1, 2, 4, 123, -1, -2, -4, 0, 0x7fffffffL, min_signed, max_signed) ++ Seq.fill(10)(
        BigInt(Random.between(min_signed, max_signed)),
      )
      Random.shuffle(1 to 31).foreach { i =>
        cases.foreach { v =>
          c.io.regPort.writeEnable.poke(true)
          c.clock.step()
          c.io.regPort.regwr_addr.poke(i)
          c.io.regPort.regwr_data.poke(v.S)
          c.io.regPort.rs2_addr.poke(i)
          c.clock.step()
          c.io.regPort.writeEnable.poke(false)
          c.clock.step()
          c.io.regPort.rs2.expect(v)
        }
      }
      c.clock.step(10)
    }
  }
}
