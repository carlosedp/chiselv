package chiselv

import chiseltest._
import org.scalatest._

import flatspec._
import matchers._

class SysconSpec extends AnyFlatSpec with ChiselScalatestTester with should.Matchers {
  behavior of "Syscon"

  def defaultDut() =
    test(new Syscon(32, 50000000, 8, 0L, 64 * 1024, 64 * 1024)).withAnnotations(
      Seq(
        WriteVcdAnnotation,
      ),
    )

  it should "read dummy value from Syscon 0x0" in {
    defaultDut() { c =>
      c.io.SysconPort.Address.poke(0x00)
      c.clock.step()
      c.io.SysconPort.DataOut.peekInt() should be(0xbaad_cafeL)
    }
  }
  it should "read clock speed from Syscon" in {
    defaultDut() { c =>
      c.io.SysconPort.Address.poke(0x08)
      c.clock.step()
      c.io.SysconPort.DataOut.peekInt() should be(50000000)
    }
  }
  it should "check if UART0 is available in Syscon" in {
    defaultDut() { c =>
      c.io.SysconPort.Address.poke(0x10)
      c.clock.step()
      c.io.SysconPort.DataOut.peekInt() should be(1)
    }
  }
  it should "check if GPIO0 is available in Syscon" in {
    defaultDut() { c =>
      c.io.SysconPort.Address.poke(0x18)
      c.clock.step()
      c.io.SysconPort.DataOut.peekInt() should be(1)
    }
  }
  it should "check num of GPIO0s in Syscon" in {
    defaultDut() { c =>
      c.io.SysconPort.Address.poke(0x28)
      c.clock.step()
      c.io.SysconPort.DataOut.peekInt() should be(8)
    }
  }
  it should "check if Timer0 is available in Syscon" in {
    defaultDut() { c =>
      c.io.SysconPort.Address.poke(0x24)
      c.clock.step()
      c.io.SysconPort.DataOut.peekInt() should be(1)
    }
  }
  it should "check RAM size in Syscon" in {
    defaultDut() { c =>
      c.io.SysconPort.Address.poke(0x34)
      c.clock.step()
      c.io.SysconPort.DataOut.peekInt() should be(64 * 1024)
    }
  }
}
