package chiselv

import scala.util.Random
import chisel3._
import chiseltest._
import org.scalatest._

import flatspec._
import matchers._

class RegisterBankSpec extends AnyFlatSpec with ChiselScalatestTester with should.Matchers {

  it should "have x0 equal to 0" in {
    test(new RegisterBank) { c =>
      c.io.rs1_addr.poke(0.U)
      c.io.rs1.expect(0.U)
    }
  }
  it should "not write to x0" in {
    test(new RegisterBank) { c =>
      c.io.writeEnable.poke(true)
      c.io.regwr_addr.poke(0.U)
      c.io.regwr_data.poke(123.U)
      c.clock.step()
      c.io.rs1_addr.peekInt() should be(0)
    }
  }
  it should "not write if not enabled" in {
    test(new RegisterBank) { c =>
      c.io.rs1_addr.poke(1)
      c.io.regwr_addr.poke(1)
      c.io.regwr_data.poke(123.U)
      c.clock.step()
      c.io.rs1.expect(0.U)
    }
  }
  it should "write all other registers and read values as expected" in {
    test(new RegisterBank).withAnnotations(Seq(WriteVcdAnnotation)) { c =>
      val one = BigInt(1)
      val max = (one << 32) - one
      val cases = Array[BigInt](1, 2, 4, 123, 0, 0x7fffffffL, max) ++ Seq.fill(10)(
        BigInt(Random.nextLong(max.toLong))
      )
      Random.shuffle(1 to 31).foreach { i =>
        cases.foreach { v =>
          c.io.writeEnable.poke(true)
          c.clock.step()
          c.io.regwr_addr.poke(i)
          c.io.regwr_data.poke(v.U)
          c.io.rs2_addr.poke(i)
          c.clock.step()
          c.io.writeEnable.poke(false)
          c.clock.step()
          c.io.rs2.expect(v)
        }
      }
      c.clock.step(10)
    }
  }
}
