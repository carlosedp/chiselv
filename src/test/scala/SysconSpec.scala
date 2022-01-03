package chiselv

import chisel3._
import chiseltest._
import org.scalatest._

import flatspec._
import matchers._

class SysconSpec extends AnyFlatSpec with ChiselScalatestTester with should.Matchers {
  behavior of "Syscon"

  def defaultDut() =
    test(new Syscon(32, 50000000, 8, 0L, 64 * 1024, 64 * 1024)).withAnnotations(
      Seq(
        WriteVcdAnnotation
      )
    )

  it should "read dummy value from Syscon 0x0" in {
    defaultDut() { c =>
      c.io.SysconPort.Address.poke(0x0000_1000.U)
      c.clock.step()
      c.io.SysconPort.DataOut.expect(0xbaad_cafeL.U)
    }
  }
  it should "read clock speed from Syscon" in {
    defaultDut() { c =>
      c.io.SysconPort.Address.poke(0x0000_1008.U)
      c.clock.step()
      c.io.SysconPort.DataOut.expect(50000000.U)
    }
  }
  it should "check if UART0 is available in Syscon" in {
    defaultDut() { c =>
      c.io.SysconPort.Address.poke(0x0000_1010.U)
      c.clock.step()
      c.io.SysconPort.DataOut.expect(1.U)
    }
  }
  it should "check if GPIO0 is available in Syscon" in {
    defaultDut() { c =>
      c.io.SysconPort.Address.poke(0x0000_1018.U)
      c.clock.step()
      c.io.SysconPort.DataOut.expect(1.U)
    }
  }
  it should "check num of GPIO0s in Syscon" in {
    defaultDut() { c =>
      c.io.SysconPort.Address.poke(0x0000_1028.U)
      c.clock.step()
      c.io.SysconPort.DataOut.expect(8.U)
    }
  }
  it should "check if Timer0 is available in Syscon" in {
    defaultDut() { c =>
      c.io.SysconPort.Address.poke(0x0000_1024.U)
      c.clock.step()
      c.io.SysconPort.DataOut.expect(1.U)
    }
  }
  it should "check RAM size in Syscon" in {
    defaultDut() { c =>
      c.io.SysconPort.Address.poke(0x0000_1034.U)
      c.clock.step()
      c.io.SysconPort.DataOut.expect((64 * 1024).U)
    }
  }
}
