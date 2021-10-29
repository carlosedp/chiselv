package chiselv

import chisel3._
import chiseltest._
import org.scalatest._

import java.io.{File, PrintWriter}

import flatspec._
import matchers._

class MemorySpec extends AnyFlatSpec with ChiselScalatestTester with should.Matchers {
  behavior of "DataMemory"

  it should "write and read from address" in {
    test(new DualPortRAM(32, 1 * 1024)) { c =>
      c.io.dualPort.writeAddr.poke(0.U)
      c.io.dualPort.readAddr.poke(0.U)
      c.io.dualPort.writeMask.poke("b1111".U)
      c.clock.step(2)
      c.io.dualPort.writeEnable.poke(true.B)
      c.io.dualPort.writeData.poke(1234.U)
      c.clock.step(2)
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

  behavior of "InstructionMemory"
  it should "load from file and read multiple instructions" in {
    val filename = "MemorySpecTestFile.hex"
    // Create memory test file with 32bit address space
    new PrintWriter(new File(filename)) { write("00010203\r\n08090A0B\r\nDEADBEEF\r\n07060504\r\n"); close }

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
    new File(filename).delete()
  }
}
