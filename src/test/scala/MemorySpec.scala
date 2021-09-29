import chisel3._
import chiseltest._
import org.scalatest._
import treadle.ClockInfoAnnotation
import treadle.executable.ClockInfo

import flatspec._
import matchers._

class MemorySpec extends AnyFlatSpec with ChiselScalatestTester with should.Matchers {
  val filename = "MemorySpecTestFile.hex"
  // Create memory test file with 32bit address space
  reflect.io.File(filename).writeAll("00010203\r\n08090A0B\r\nDEADBEEF\r\n07060504\r\n")

  "Memory" should "write and read from address" in {
    test(new DualPortRAM(32, 1 * 1024)) { c =>
      c.io.dualPort.writeAddr.poke(0.U)
      c.io.dualPort.readAddr.poke(0.U)
      c.clock.step()
      c.io.dualPort.writeEnable.poke(true.B)
      c.io.dualPort.writeData.poke(1234.U)
      c.clock.step()
      c.io.dualPort.readData.expect(1234.U)
    }
  }
  it should "not allow write to address" in {
    test(new DualPortRAM(32, 1 * 1024)) { c =>
      c.io.dualPort.writeAddr.poke("h00000010".U)
      c.io.dualPort.readAddr.poke("h00000010".U)
      c.clock.step()
      c.io.dualPort.writeData.poke(1234.U)
      c.clock.step()
      c.io.dualPort.readData.expect(0.U)
    }
  }
  it should "load from file and read multiple addresses" in {
    test(new DualPortRAM(32, 1 * 1024, filename)).withAnnotations(
      Seq(VerilatorBackendAnnotation, WriteFstAnnotation, ClockInfoAnnotation(Seq(ClockInfo(period = 2))))
    ) { c =>
      c.io.dualPort.readAddr.poke(0.U)
      c.io.dualPort.writeAddr.poke(0.U)
      c.clock.step()
      c.io.dualPort.readData.expect("h00010203".U)
      c.io.dualPort.readAddr.poke(1.U)
      c.clock.step()
      c.io.dualPort.readData.expect("h08090A0B".U)
      c.io.dualPort.readAddr.poke(2.U)
      c.clock.step()
      c.io.dualPort.readData.expect("hDEADBEEF".U)
      c.io.dualPort.readAddr.poke(3.U)
      c.clock.step()
      c.io.dualPort.readData.expect("h07060504".U)
      c.io.dualPort.readAddr.poke(1.U)
      c.clock.step()
      c.io.dualPort.writeAddr.poke(1.U)
      c.io.dualPort.writeEnable.poke(true.B)
      c.io.dualPort.writeData.poke(1234.U)
      c.io.dualPort.readAddr.poke(1.U)
      c.clock.step()
      c.io.dualPort.readData.expect(1234.U)
    }
    reflect.io.File(filename).delete()
  }
}
