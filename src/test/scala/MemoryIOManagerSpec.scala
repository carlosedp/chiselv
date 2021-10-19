import chisel3._
import chiseltest._
import org.scalatest._

import flatspec._
import matchers._

// Extend the Control module to add the observer for sub-module signals
class MemoryIOManagerWrapper(bitWidth: Int = 32, clockFreq: Long, sizeBytes: Long = 1)
  extends MemoryIOManager(bitWidth, clockFreq, sizeBytes)
  with Observer {
  val memPort = observe(memory.io.dualPort)
}

class MemoryIOManagerSpec extends AnyFlatSpec with ChiselScalatestTester with should.Matchers {
  behavior of "MemoryIOManager"

  it should "read dummy value from Syscon 0x0" in {
    test(new MemoryIOManager(32, 50000000)).withAnnotations(
      Seq(
        VerilatorBackendAnnotation
      )
    ) { c =>
      c.io.MemoryIOPort.readAddr.poke(0x0000_1000.U)
      c.clock.step()
      c.io.MemoryIOPort.readData.expect(0xbaad_cafeL.U)
    }
  }
  it should "read clock speed from Syscon" in {
    test(new MemoryIOManager(32, 50000000)).withAnnotations(
      Seq(
        VerilatorBackendAnnotation
      )
    ) { c =>
      c.io.MemoryIOPort.readAddr.poke(0x0000_1008.U)
      c.clock.step()
      c.io.MemoryIOPort.readData.expect(50000000.U)
    }
  }
  it should "check if UART0 is available in Syscon" in {
    test(new MemoryIOManager(32, 50000000)).withAnnotations(
      Seq(
        VerilatorBackendAnnotation
      )
    ) { c =>
      c.io.MemoryIOPort.readAddr.poke(0x0000_1010.U)
      c.clock.step()
      c.io.MemoryIOPort.readData.expect(0.U)
    }
  }
  it should "check if GPIO0 is available in Syscon" in {
    test(new MemoryIOManager(32, 50000000)).withAnnotations(
      Seq(
        VerilatorBackendAnnotation
      )
    ) { c =>
      c.io.MemoryIOPort.readAddr.poke(0x0000_1018.U)
      c.clock.step()
      c.io.MemoryIOPort.readData.expect(1.U)
    }
  }
  it should "write data and follow with a read in same address" in {
    test(new MemoryIOManager(32, 50000000, 64 * 1024)).withAnnotations(
      Seq(
        VerilatorBackendAnnotation
      )
    ) { c =>
      val addressOffset = 0x8000_0000L
      val addresses     = Seq(0x0L, 0x0010L, 0x0d00L, 0x1000L, 0x2000L, 0x8000L, 0xe000L)
      val values        = Seq(0.U, 1.U, 0x0000_cafeL.U, 0xbaad_cafeL.U, 0xffff_ffffL.U)
      addresses.foreach { address =>
        values.foreach { value =>
          c.io.MemoryIOPort.writeEnable.poke(true.B)
          c.io.MemoryIOPort.writeAddr.poke((addressOffset + address).U)
          c.io.MemoryIOPort.writeData.poke(value)
          c.clock.step()
          c.io.MemoryIOPort.readAddr.poke((addressOffset + address).U)
          c.clock.step()
          c.io.MemoryIOPort.readData.expect(value)
        }
      }
      c.clock.step(10)
    }
  }
}
