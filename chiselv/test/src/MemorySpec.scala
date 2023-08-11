package chiselv

import chiseltest._
import org.scalatest._

import flatspec._
import matchers._

class MemorySpec extends AnyFlatSpec with ChiselScalatestTester with should.Matchers {
  behavior of "DataMemory"

  it should "write and read from address" in {
    test(new DualPortRAM(32, 1 * 1024)).withAnnotations(
      Seq(
        WriteVcdAnnotation
      )
    ) { c =>
      c.io.dualPort.writeEnable.poke(true)
      c.io.dualPort.writeAddress.poke(1)
      c.io.dualPort.writeData.poke(1234)
      c.clock.step(1)
      c.io.dualPort.readAddress.poke(1)
      c.clock.step(2)
      c.io.dualPort.readData.peekInt() should be(1234)
    }
  }
  it should "not allow write to address" in {
    test(new DualPortRAM(32, 1 * 1024)) { c =>
      c.io.dualPort.writeAddress.poke(0x00000010)
      c.io.dualPort.readAddress.poke(0x00000010)
      c.clock.step()
      c.io.dualPort.writeData.poke(1234)
      c.clock.step()
      c.io.dualPort.readData.peekInt() should be(0)
    }
  }

  it should "write data and follow with a read in same address" in {
    test(new DualPortRAM(32, 64 * 1024)) { c =>
      val addressOffset = 0x0000100L
      val addresses     = Seq(0x0L, 0x0010L, 0x0d00L, 0x1000L, 0x2000L, 0x8000L, 0xe000L)
      val values        = Seq(0, 1, 0x0000_cafeL, 0xbaad_cafeL, 0xffff_ffffL)
      addresses.foreach { address =>
        values.foreach { value =>
          c.io.dualPort.writeEnable.poke(true)
          c.io.dualPort.writeAddress.poke(addressOffset + address)
          c.io.dualPort.readAddress.poke(addressOffset + address)
          c.io.dualPort.writeData.poke(value)
          c.clock.step(1)
          c.io.dualPort.readAddress.poke(addressOffset + address)
          c.clock.step(1)
          c.io.dualPort.readData.peekInt() should be(value)
        }
      }
      c.clock.step(10)
    }
  }

  behavior of "InstructionMemory"
  it should "load from file and read multiple instructions" in {
    val filename = "MemorySpecTestFile.hex"
    // Create memory test file with 32bit address space
    val file = os.pwd / "MemorySpecTestFile.hex"
    os.remove(file)
    os.write(file, "00010203\r\n08090A0B\r\nDEADBEEF\r\n07060504\r\n")
    test(new InstructionMemory(32, 16 * 1024, filename)).withAnnotations(Seq(WriteVcdAnnotation)) { c =>
      c.io.memPort.readAddr.poke(0)
      c.clock.step()
      c.io.memPort.readData.peekInt() should be(0x00010203L)
      c.io.memPort.readAddr.poke(4)
      c.clock.step()
      c.io.memPort.readData.peekInt() should be(0x08090a0bL)
      c.io.memPort.readAddr.poke(8)
      c.clock.step()
      c.io.memPort.readData.peekInt() should be(0xdeadbeefL)
      c.io.memPort.readAddr.poke(12)
      c.clock.step()
      c.clock.step()
      c.io.memPort.readData.peekInt() should be(0x07060504L)
      c.io.memPort.readAddr.poke(1)
      c.clock.step()
    }
    os.remove.all(file)
  }
}
