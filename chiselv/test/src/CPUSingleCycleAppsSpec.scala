package chiselv

import chiseltest._
import chiseltest.experimental._
import org.scalatest._

import flatspec._
import matchers._

// Extend the Control module to add the observer for sub-module signals
class CPUSingleCycleWrapperApps(
  memoryFile: String,
) extends SOC(
    cpuFrequency          = 25000000,
    entryPoint            = 0,
    bitWidth              = 32,
    instructionMemorySize = 1 * 1024,
    dataMemorySize        = 1 * 1024,
    memoryFile            = memoryFile,
    numGPIO               = 0,
  ) {
  val registers    = expose(core.registerBank.regs)
  val pc           = expose(core.PC.pc)
  val memWriteAddr = expose(core.memoryIOManager.io.MemoryIOPort.writeAddr)
  val memWriteData = expose(core.memoryIOManager.io.MemoryIOPort.writeData)
  val memReadAddr  = expose(core.memoryIOManager.io.MemoryIOPort.readAddr)
  val memReadData  = expose(core.memoryIOManager.io.MemoryIOPort.readData)
}

class CPUSingleCycleAppsSpec extends AnyFlatSpec with ChiselScalatestTester with should.Matchers {
  behavior of "CPUSingleCycleApps"

  val writeLatency = 2
  val readLatency  = 1

  def defaultDut(memoryfile: String) =
    test(new CPUSingleCycleWrapperApps(memoryFile = memoryfile))
      .withAnnotations(
        Seq(
          WriteVcdAnnotation,
        ),
      )

  it should "load instructions from file to write to all registers with ADDI" in {
    val filename = "./gcc/test/test_addi.mem"
    defaultDut(filename) { c =>
      c.clock.setTimeout(0)
      val results = List.fill(8)(List(0, 1000, 3000, 2000)).flatten
      for ((i, r) <- 0 until 31 zip results) {
        c.registers(i).peekInt() should be(r)
        c.clock.step(1)
      }
      c.clock.step(20)
    }
  }

  it should "load program and end with 25 (0x19) in mem address 100 (0x64)" in {
    val filename = "./gcc/test/test_book.mem"
    defaultDut(filename) { c =>
      c.clock.setTimeout(0)
      c.clock.step(1) // addi
      c.registers(2).peekInt() should be(5)
      c.clock.step(1) // addi
      c.registers(3).peekInt() should be(12)
      c.clock.step(1) // addi
      c.registers(7).peekInt() should be(3)
      c.clock.step(1) // lui
      c.registers(6).peekInt() should be(0x80000000)
      c.clock.step(1) // or
      c.registers(4).peekInt() should be(7)
      c.clock.step(1) // and
      c.registers(5).peekInt() should be(4)
      c.clock.step(1) // add
      c.registers(5).peekInt() should be(11)
      c.clock.step(1) // beq (skip)
      c.clock.step(1) // slt
      c.registers(4).peekInt() should be(0)
      c.clock.step(1) // beq (skip next addi)
      c.clock.step(1) // slt
      c.registers(4).peekInt() should be(1)
      c.clock.step(1) // add
      c.registers(7).peekInt() should be(12)
      c.clock.step(1) // sub
      c.registers(7).peekInt() should be(7)
      // Check Memory write at address 0x80000000
      c.memWriteAddr.peekInt() should be(0x80000000L)
      c.memWriteData.peekInt() should be(7)
      c.clock.step(1 + writeLatency) // sw
      // Check Memory read at address 0x80000000
      c.memReadAddr.peekInt() should be(0x80000000L)
      c.memReadData.peekInt() should be(7)
      c.clock.step(1 + readLatency) // lw
      c.registers(2).peekInt() should be(7)
      c.clock.step(1) // add
      c.registers(9).peekInt() should be(18)
      c.clock.step(1) // jal (skip next addi)
      c.clock.step(1) // add
      c.registers(2).peekInt() should be(25)
      // // Check Memory address 0x80000000 + 100 (0x64)
      c.memWriteAddr.peekInt() should be(0x80000000L + 0x64)
      c.memWriteData.peekInt() should be(25)
      c.clock.step(1 + writeLatency) // sw

      // Add some padding at the end
      c.clock.step(10)
    }
  }

  it should "loop thru ascii table writing to 0x3000_0000 region" in {
    val filename = "./gcc/test/test_ascii.mem"
    defaultDut(filename) { c =>
      c.clock.setTimeout(0)
      c.clock.step(1)
      c.registers(1).peekInt() should be(40)
      c.clock.step(1)
      c.registers(2).peekInt() should be(33)
      c.clock.step(1)
      c.registers(3).peekInt() should be(126)
      c.clock.step(1)
      c.registers(4).peekInt() should be(0x3000_0000)

      for (i <- 33 until 127) {
        // Check memory write at address 0x30000000
        c.memWriteAddr.peekInt() should be(0x30000000L)
        c.memWriteData.peekInt() should be(i)
        c.clock.step(1) // sb (no latency to write 0x30000000 region)
        c.clock.step(1) // addi
        c.clock.step(1) // bge
      }
      c.clock.step(10)
    }
  }
}
