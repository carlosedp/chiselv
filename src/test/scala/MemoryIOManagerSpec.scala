package chiselv

import chisel3._
import chiseltest._
import org.scalatest._

import flatspec._
import matchers._

class MemoryIOManagerSpec extends AnyFlatSpec with ChiselScalatestTester with should.Matchers {
  behavior of "MemoryIOManager"

  def defaultDut() =
    test(new MemoryIOManager(32, 50000000, 1024)).withAnnotations(
      Seq(
        WriteVcdAnnotation
      )
    )

  it should "read dummy value from Syscon 0x0" in {
    defaultDut() { c =>
      c.io.MemoryIOPort.readRequest.poke(true.B)
      c.io.MemoryIOPort.readAddr.poke(0x0000_1000.U)
      c.clock.step()
      c.io.MemoryIOPort.readData.expect(0xbaad_cafeL.U)
    }
  }
  it should "read clock speed from Syscon" in {
    defaultDut() { c =>
      c.io.MemoryIOPort.readRequest.poke(true.B)
      c.io.MemoryIOPort.readAddr.poke(0x0000_1008.U)
      c.clock.step()
      c.io.MemoryIOPort.readData.expect(50000000.U)
    }
  }
  it should "check if UART0 is available in Syscon" in {
    defaultDut() { c =>
      c.io.MemoryIOPort.readRequest.poke(true.B)
      c.io.MemoryIOPort.readAddr.poke(0x0000_1010.U)
      c.clock.step()
      c.io.MemoryIOPort.readData.expect(1.U)
    }
  }
  it should "check if GPIO0 is available in Syscon" in {
    defaultDut() { c =>
      c.io.MemoryIOPort.readRequest.poke(true.B)
      c.io.MemoryIOPort.readAddr.poke(0x0000_1018.U)
      c.clock.step()
      c.io.MemoryIOPort.readData.expect(1.U)
    }
  }
  it should "check if Timer0 is available in Syscon" in {
    defaultDut() { c =>
      c.io.MemoryIOPort.readRequest.poke(true.B)
      c.io.MemoryIOPort.readAddr.poke(0x0000_1024.U)
      c.clock.step()
      c.io.MemoryIOPort.readData.expect(1.U)
    }
  }
}
