package chiselv

import chisel3._
import chiseltest._
import org.scalatest._

import flatspec._
import matchers._

class ProgramCounterSpec extends AnyFlatSpec with ChiselScalatestTester with should.Matchers {
  behavior of "ProgramCounter"

  it should "initialize to 0" in {
    test(new ProgramCounter()) { c =>
      c.io.pcPort.dataOut.expect(0.U)
    }
  }
  it should "walk 4 bytes" in {
    test(new ProgramCounter()) { c =>
      c.io.pcPort.countEnable.poke(true.B)
      c.io.pcPort.dataOut.peek().litValue should be(0)
      c.clock.step()
      c.io.pcPort.dataOut.peek().litValue should be(4)
    }
  }
  it should "jump to 0xbaddcafe (write)" in {
    test(new ProgramCounter()) { c =>
      c.io.pcPort.writeEnable.poke(true.B)
      c.io.pcPort.dataIn.poke("h_baddcafe".U)
      c.clock.step()
      c.io.pcPort.dataOut.peek().litValue should be(BigInt("baddcafe", 16))
    }
  }
  it should "add 32 to PC ending up in 40" in {
    test(new ProgramCounter()) { c =>
      c.io.pcPort.writeEnable.poke(true.B)
      c.io.pcPort.dataIn.poke(8.U)
      c.clock.step()
      c.io.pcPort.dataOut.peek().litValue should be(8)
      c.io.pcPort.writeAdd.poke(true.B)
      c.io.pcPort.dataIn.poke(32.U)
      c.clock.step()
      c.io.pcPort.dataOut.peek().litValue should be(40)
    }
  }
}
