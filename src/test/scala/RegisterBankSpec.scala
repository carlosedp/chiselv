package chiselv

import chisel3._
import chiseltest._
import org.scalatest._

import flatspec._
import matchers._

class RegisterBankSpec extends AnyFlatSpec with ChiselScalatestTester with should.Matchers {
  behavior of "RegisterBank"

  it should "have x0 equal to 0" in {
    test(new RegisterBank()) { c =>
      c.io.regPort.rs1_addr.poke(0.U)
      c.io.regPort.rs1.expect(0.S)
    }
  }
  it should "not write to x0" in {
    test(new RegisterBank()) { c =>
      c.io.regPort.writeEnable.poke(true)
      c.io.regPort.regwr_addr.poke(0.U)
      c.io.regPort.regwr_data.poke(123.S)
      c.clock.step()
      c.io.regPort.rs1_addr.peekInt() should be(0)
    }
  }
  it should "not write if not enabled" in {
    test(new RegisterBank()) { c =>
      c.io.regPort.rs1_addr.poke(1)
      c.io.regPort.regwr_addr.poke(1)
      c.io.regPort.regwr_data.poke(123.S)
      c.clock.step()
      c.io.regPort.rs1.expect(0.S)
    }
  }
  it should "write to other registers as expected" in {
    test(new RegisterBank()).withAnnotations(Seq(WriteVcdAnnotation)) { c =>
      for (i <- 1 until 32) {
        c.io.regPort.writeEnable.poke(true)
        c.clock.step()
        c.io.regPort.regwr_addr.poke(i)
        c.io.regPort.regwr_data.poke(i.S)
        c.io.regPort.rs2_addr.poke(i)
        c.clock.step()
        c.io.regPort.writeEnable.poke(false)
        c.io.regPort.rs2.expect(i.S)
      }
      c.clock.step(10)
    }
  }
}
