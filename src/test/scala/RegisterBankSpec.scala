import chisel3._
import chiseltest._
import org.scalatest._

import flatspec._
import matchers._

class RegisterBankSpec extends AnyFlatSpec with ChiselScalatestTester with should.Matchers {
  "RegisterBank" should "have x0 equal to 0" in {
    test(new RegisterBank()) { c =>
      c.io.regPort.address.poke(0.U)
      c.io.regPort.dataOut.expect(0.U)
    }
  }
  it should "not write to x0" in {
    test(new RegisterBank()) { c =>
      c.io.regPort.writeEnable.poke(true.B)
      c.io.regPort.address.poke(0.U)
      c.io.regPort.dataIn.poke(123.U)
      c.clock.step()
      c.io.regPort.dataOut.peek().litValue() should be(0)
    }
  }
  it should "not write if not enabled" in {
    test(new RegisterBank()) { c =>
      c.io.regPort.address.poke(1.U)
      c.io.regPort.dataIn.poke(123.U)
      c.clock.step()
      c.io.regPort.dataOut.expect(0.U)
    }
  }
  it should "write to other registers as expected" in {
    test(new RegisterBank()).withAnnotations(Seq(WriteVcdAnnotation)) { c =>
      for (i <- 1 until 31) {
        c.io.regPort.writeEnable.poke(true.B)
        c.clock.step()
        c.io.regPort.address.poke(i.U)
        c.io.regPort.dataIn.poke(i.U)
        c.clock.step()
        c.io.regPort.writeEnable.poke(false.B)
        c.io.regPort.dataOut.expect(i.U)
      }
      c.clock.step(10)
    }
  }
}
