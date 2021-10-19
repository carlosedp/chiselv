import chisel3._
import chiseltest._
import org.scalatest._

import flatspec._
import matchers._

// Extend the Control module to add the observer for sub-module signals
class CPUSingleCycleWrapperApps(
  cpuFrequency: Int,
  bitWidth: Int,
  instructionMemorySize: Int,
  memorySize: Int,
  memoryFile: String,
) extends CPUSingleCycle(cpuFrequency, bitWidth, instructionMemorySize, memorySize, memoryFile)
  with Observer {
  val registers    = observe(registerBank.regs)
  val memWriteAddr = observe(memoryIOManager.io.MemoryIOPort.writeAddr)
  val memWriteData = observe(memoryIOManager.io.MemoryIOPort.writeData)
  val memReadAddr  = observe(memoryIOManager.io.MemoryIOPort.readAddr)
  val memReadData  = observe(memoryIOManager.io.MemoryIOPort.readData)
}

class CPUSingleCycleAppsSpec extends AnyFlatSpec with ChiselScalatestTester with should.Matchers {
  behavior of "CPUSingleCycleApps"

  val cpuFrequency          = 25000000
  val bitWidth              = 32
  val instructionMemorySize = 1 * 1024
  val memorySize            = 1 * 1024

  def defaultDut(memoryfile: String) =
    test(new CPUSingleCycleWrapperApps(cpuFrequency, bitWidth, instructionMemorySize, memorySize, memoryfile))
      .withAnnotations(
        Seq(
          WriteVcdAnnotation,
          VerilatorBackendAnnotation,
        )
      )

  it should "load instructions from file to write to all registers with ADDI" in {
    val filename = "./gcc/test/test_addi.mem"
    defaultDut(filename) { c =>
      c.clock.setTimeout(0)
      val results = List.fill(8)(List(0, 1000, 3000, 2000)).flatten
      for ((i, r) <- (0 until 31 zip results)) {
        c.registers(i).peek().litValue() should be(r)
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
      c.registers(2).peek().litValue() should be(5)
      c.clock.step(1) // addi
      c.registers(3).peek().litValue() should be(12)
      c.clock.step(1) // addi
      c.registers(7).peek().litValue() should be(3)
      c.clock.step(1) // lui
      c.registers(6).expect(0x8000_0000L.S)
      c.clock.step(1) // or
      c.registers(4).peek().litValue() should be(7)
      c.clock.step(1) // and
      c.registers(5).peek().litValue() should be(4)
      c.clock.step(1) // add
      c.registers(5).peek().litValue() should be(11)
      c.clock.step(1) // beq (skip)
      c.clock.step(1) // slt
      c.registers(4).peek().litValue() should be(0)
      c.clock.step(1) // beq (skip next addi)
      c.clock.step(1) // slt
      c.registers(4).peek().litValue() should be(1)
      c.clock.step(1) // add
      c.registers(7).peek().litValue() should be(12)
      c.clock.step(1) // sub
      c.registers(7).peek().litValue() should be(7)
      // Check Memory write at address 0x80000000
      c.memWriteAddr.peek().litValue() should be(BigInt("80000000", 16))
      c.memWriteData.peek().litValue() should be(7)
      c.clock.step(1) // sw
      // Check Memory read at address 0x80000000
      c.memReadAddr.peek().litValue() should be(BigInt("80000000", 16))
      c.memReadData.peek().litValue() should be(7)
      c.clock.step(1) // lw
      c.registers(2).peek().litValue() should be(7)
      c.clock.step(1) // add
      c.registers(9).peek().litValue() should be(18)
      c.clock.step(1) // jal (skip next addi)
      c.clock.step(1) // add
      c.registers(2).peek().litValue() should be(25)
      // // Check Memory address 0x80000000 + 100 (0x64)
      c.memWriteAddr.peek().litValue() should be(BigInt("80000000", 16) + 100)
      c.memWriteData.peek().litValue() should be(25)
      c.clock.step(1) // sw

      // Add some padding at the end
      c.clock.step(10)
    }
  }

  it should "loop thru ascii table writing to 0x3000_0000 region" in {
    val filename = "./gcc/test/test_ascii.mem"
    defaultDut(filename) { c =>
      c.clock.setTimeout(0)
      c.clock.step(1)
      c.registers(1).peek().litValue() should be(40)
      c.clock.step(1)
      c.registers(2).peek().litValue() should be(33)
      c.clock.step(1)
      c.registers(3).peek().litValue() should be(126)
      c.clock.step(1)
      c.registers(4).peek().litValue() should be(0x3000_0000)

      for (i <- 33 until 127) {
        // Check memory write at address 0x3000_0000
        c.memWriteAddr.peek().litValue() should be(0x3000_0000)
        c.memWriteData.peek().litValue() should be(i)
        c.clock.step(1)
        c.clock.step(1)
        c.clock.step(1)
      }
      c.clock.step(10)
    }
  }
}
