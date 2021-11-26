package chiselv

import chisel3._
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
      c.io.dualPort.writeEnable.poke(true.B)
      c.io.dualPort.writeAddress.poke(1.U)
      c.io.dualPort.writeData.poke(1234.U)
      c.clock.step(1)
      c.io.dualPort.readAddress.poke(1.U)
      c.clock.step(2)
      c.io.dualPort.readData.expect(1234.U)
    }
  }
  it should "not allow write to address" in {
    test(new DualPortRAM(32, 1 * 1024)) { c =>
      c.io.dualPort.writeAddress.poke("h00000010".U)
      c.io.dualPort.readAddress.poke("h00000010".U)
      c.clock.step()
      c.io.dualPort.writeData.poke(1234.U)
      c.clock.step()
      c.io.dualPort.readData.expect(0.U)
    }
  }

  it should "write data and follow with a read in same address" in {
    test(new DualPortRAM(32, 64 * 1024)) { c =>
      val addressOffset = 0x0000100L
      val addresses     = Seq(0x0L, 0x0010L, 0x0d00L, 0x1000L, 0x2000L, 0x8000L, 0xe000L)
      val values        = Seq(0.U, 1.U, 0x0000_cafeL.U, 0xbaad_cafeL.U, 0xffff_ffffL.U)
      addresses.foreach { address =>
        values.foreach { value =>
          c.io.dualPort.writeEnable.poke(true.B)
          c.io.dualPort.writeAddress.poke((addressOffset + address).U)
          c.io.dualPort.readAddress.poke((addressOffset + address).U)
          c.io.dualPort.writeData.poke(value)
          c.clock.step(1)
          c.io.dualPort.readAddress.poke((addressOffset + address).U)
          c.clock.step(1)
          c.io.dualPort.readData.expect(value)
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
    os.write(file, "00010203\r\n08090A0B\r\nDEADBEEF\r\n07060504\r\n")
    test(new InstructionMemory(32, 16 * 1024, filename)).withAnnotations(Seq(WriteVcdAnnotation)) { c =>
      c.io.memPort.readAddr.poke(0.U)
      c.clock.step()
      c.io.memPort.readData.expect("h00010203".U)
      c.io.memPort.readAddr.poke(4.U)
      c.clock.step()
      c.io.memPort.readData.expect("h08090A0B".U)
      c.io.memPort.readAddr.poke(8.U)
      c.clock.step()
      c.io.memPort.readData.expect("hDEADBEEF".U)
      c.io.memPort.readAddr.poke(12.U)
      c.clock.step()
      c.clock.step()
      c.io.memPort.readData.expect("h07060504".U)
      c.io.memPort.readAddr.poke(1.U)
      c.clock.step()
    }
    os.remove.all(file)
  }
}
