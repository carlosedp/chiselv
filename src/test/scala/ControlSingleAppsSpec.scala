import chiseltest._
import org.scalatest._

import flatspec._
import matchers._

// Extend the Control module to add the observer for sub-module signals
class ControlSingleWrapperApps(bitWidth: Int, instructionMemorySize: Int, dataMemorySize: Int, memoryFile: String)
  extends ControlSingle(bitWidth, instructionMemorySize, dataMemorySize, memoryFile)
  with Observer {
  val registers        = observe(registerBank.regs)
  val dataMemWriteAddr = observe(memoryIOManager.memory.io.dualPort.writeAddr)
  val dataMemWriteData = observe(memoryIOManager.memory.io.dualPort.writeData)
  val dataMemReadAddr  = observe(memoryIOManager.memory.io.dualPort.readAddr)
  val dataMemReadData  = observe(memoryIOManager.memory.io.dualPort.readData)
}

class ControlSingleAppsSpec extends AnyFlatSpec with ChiselScalatestTester with should.Matchers {
  behavior of "ControlSingleApps"

  it should "load instructions from file to write to all registers with ADDI" in {
    val filename = "./gcc/test_addi.mem"
    test(new ControlSingleWrapperApps(32, 1 * 1024, 1 * 1024, filename)).withAnnotations(
      Seq(
        WriteVcdAnnotation,
        VerilatorBackendAnnotation,
      )
    ) { c =>
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
    val filename = "./gcc/test_book.mem"
    test(new ControlSingleWrapperApps(32, 1 * 1024, 1 * 1024, filename)).withAnnotations(
      Seq(
        WriteVcdAnnotation,
        VerilatorBackendAnnotation,
      )
    ) { c =>
      c.clock.setTimeout(0)
      c.clock.step(1) // addi
      c.registers(2).peek().litValue() should be(5)
      c.clock.step(1) // addi
      c.registers(3).peek().litValue() should be(12)
      c.clock.step(1) // addi
      c.registers(7).peek().litValue() should be(3)
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
      c.clock.step(1) // sw
      // Check Memory address 96 (0x60) added to the memory map
      c.dataMemReadAddr.peek().litValue() should be(96)
      c.dataMemReadData.peek().litValue() should be(7)
      c.clock.step(1) // lw
      c.registers(2).peek().litValue() should be(7)
      c.clock.step(1) // add
      c.registers(9).peek().litValue() should be(18)
      c.clock.step(1) // jal (skip next addi)
      c.clock.step(1) // add
      c.registers(2).peek().litValue() should be(25)
      // // Check Memory address 100 (0x64)
      c.dataMemWriteAddr.peek().litValue() should be(100)
      c.dataMemWriteData.peek().litValue() should be(25)
      c.clock.step(1) // sw

      // Add some padding at the end
      c.clock.step(10)
    }
  }

  it should "loop thru ascii table writing to 0x3000_0000 region" in {
    val filename = "./gcc/test_ascii.mem"
    test(new ControlSingleWrapperApps(32, 4 * 1024, 1 * 1024, filename)).withAnnotations(
      Seq(
        WriteVcdAnnotation,
        VerilatorBackendAnnotation,
      )
    ) { c =>
      c.clock.setTimeout(0)
      c.clock.step(40)
    }
  }
}
