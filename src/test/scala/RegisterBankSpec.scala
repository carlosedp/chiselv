import chisel3._
import chiseltest._
import org.scalatest._
import flatspec._
import matchers.should._

class RegisterBankSpec extends AnyFlatSpec with ChiselScalatestTester with Matchers {

  "RegisterBank" should "have x0 equal to 0" in {
    test(new RegisterBank()) { c =>
      c.io.address.poke(0.U)
      c.io.dataOut.expect(0.U)
    }
  }
  it should "not write to x0" in {
    test(new RegisterBank()) { c =>
      c.io.writeEnable.poke(true.B)
      c.io.address.poke(0.U)
      c.io.dataIn.poke(123.U)
      c.clock.step()
      c.io.address.poke(0.U)
      c.io.dataOut.expect(0.U)
    }
  }
  it should "not write if not enabled" in {
    test(new RegisterBank()) { c =>
      c.io.address.poke(1.U)
      c.io.dataIn.poke(123.U)
      c.clock.step()
      c.io.address.poke(1.U)
      c.io.dataOut.expect(0.U)
    }
  }
  it should "write to other registers as expected" in {
    test(new RegisterBank()) { c =>
      for (i <- 1 until 31) {
        c.io.writeEnable.poke(true.B)
        c.io.address.poke(i.U)
        c.io.dataIn.poke(i.U)
        c.clock.step()
        c.io.writeEnable.poke(false.B)
        c.io.address.poke(i.U)
        c.io.dataOut.expect(i.U)
      }
    }
  }
}
