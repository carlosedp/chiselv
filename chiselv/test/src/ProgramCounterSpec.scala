package chiselv

import chiseltest._
import org.scalatest._

import flatspec._
import matchers._

class ProgramCounterSpec extends AnyFlatSpec with ChiselScalatestTester with should.Matchers {
  behavior of "ProgramCounter"

  it should "initialize to 0" in {
    test(new ProgramCounter) { c =>
      c.io.pcPort.PC.peekInt() should be(0)
    }
  }
  it should "initialize to 0x00400000" in {
    test(new ProgramCounter(entryPoint = 0x00400000)) { c =>
      c.io.pcPort.PC.peekInt() should be(0x00400000L)
    }
  }
  it should "walk 4 bytes" in {
    test(new ProgramCounter) { c =>
      c.io.pcPort.PC.peekInt() should be(0)
      c.io.pcPort.PC4.peekInt() should be(4)
      c.clock.step()
      c.io.pcPort.writeEnable.poke(true)
      c.io.pcPort.dataIn.poke(c.io.pcPort.PC4.peek())
      c.clock.step()
      c.io.pcPort.PC.peekInt() should be(4)
    }
  }
  it should "jump to 0xbaddcafe (write)" in {
    test(new ProgramCounter) { c =>
      c.io.pcPort.writeEnable.poke(true)
      c.io.pcPort.dataIn.poke(0xbaddcafeL)
      c.clock.step()
      c.io.pcPort.PC.peekInt() should be(0xbaddcafeL)
    }
  }
  it should "add 32 to PC ending up in 40" in {
    test(new ProgramCounter) { c =>
      c.io.pcPort.writeEnable.poke(true)
      c.io.pcPort.dataIn.poke(8)
      c.clock.step()
      c.io.pcPort.PC.peekInt() should be(8)
      c.io.pcPort.writeAdd.poke(true)
      c.io.pcPort.dataIn.poke(32)
      c.clock.step()
      c.io.pcPort.PC.peekInt() should be(40)
    }
  }
}
