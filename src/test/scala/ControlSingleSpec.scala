import chisel3._
import chiseltest._
import org.scalatest._

import java.io.{File, PrintWriter}

import flatspec._
import matchers._

// Extend the Control module to add the observer for sub-module signals
class ControlSingleWrapper(bitWidth: Int, instructionMemorySize: Int, dataMemorySize: Int, memoryFile: String)
  extends ControlSingle(bitWidth, instructionMemorySize, dataMemorySize, memoryFile)
  with Observer {
  val registers        = observe(registerBank.regs)
  val dataMemReadAddr  = observe(dataMemory.io.dualPort.readAddr)
  val dataMemReadData  = observe(dataMemory.io.dualPort.readData)
  val dataMemWriteAddr = observe(dataMemory.io.dualPort.writeAddr)
  val dataMemWriteData = observe(dataMemory.io.dualPort.writeData)
}

class ControlSingleSpec extends AnyFlatSpec with ChiselScalatestTester with should.Matchers {
  behavior of "ControlSingle"

  it should "load simple instructions" in {
    // Create memory test file with 32bit address space
    // Instructions are: addi x1 x0 32 ,lui x2 0xc0000000, lbu x3 0(x1), jal x0, -16
    val filename = "CPUSpecMemoryTestFile.hex"
    new PrintWriter(new File(filename)) { write("02000093\r\nc0000137\r\n0000c183\r\nff1ff06f\r\n"); close }
    test(new ControlSingleWrapper(32, 1 * 1024, 1 * 1024, filename)).withAnnotations(
      Seq(
        VerilatorBackendAnnotation,
        WriteVcdAnnotation,
      )
    ) { c =>
      c.clock.setTimeout(0)
      c.clock.step(20)
    }
    new File(filename).delete()
  }

  it should "load instructions from file to write to all registers with ADDI" in {
    val filename = "./gcc/test_alu.mem"
    test(new ControlSingleWrapper(32, 1 * 1024, 1 * 1024, filename)).withAnnotations(
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

  it should "validate LUI instruction" in {
    val filename = "CPUSpecMemoryTestFileLUI.hex"
    /// lui x2, 0xc0000000
    new PrintWriter(new File(filename)) { write("C0000137\r\n"); close }
    test(new ControlSingleWrapper(32, 1 * 1024, 1 * 1024, filename)).withAnnotations(
      Seq(
        WriteVcdAnnotation,
        VerilatorBackendAnnotation,
      )
    ) { c =>
      c.clock.setTimeout(0)
      c.registers(2).expect(0x00000000L.S)
      c.clock.step(1)
      c.registers(2).expect(0xc0000000L.S)
    }
    new File(filename).delete()
  }

  it should "validate AUIPC instruction" in {
    val filename = "CPUSpecMemoryTestFileAUIPC.hex"
    /// lui x2, 0xc0000000
    new PrintWriter(new File(filename)) { write("00001117\r\n00001197\r\n"); close }
    test(new ControlSingleWrapper(32, 1 * 1024, 1 * 1024, filename)).withAnnotations(
      Seq(
        WriteVcdAnnotation,
        VerilatorBackendAnnotation,
      )
    ) { c =>
      c.clock.setTimeout(0)
      c.registers(2).expect(0x00000000L.S)
      c.clock.step(1)
      c.registers(2).expect(0x00001000L.S)
      c.clock.step(1)
      c.registers(3).expect(0x00001004L.S)
    }
    new File(filename).delete()
  }

  it should "validate load instructions" in {
    val filename = "CPUSpecMemoryTestFileLUI.hex"
    /// lui x2, 0xc0000000
    new PrintWriter(new File(filename)) { write("00001117\r\n00001197\r\n"); close }
    test(new ControlSingleWrapper(32, 1 * 1024, 1 * 1024, filename)).withAnnotations(
      Seq(
        WriteVcdAnnotation,
        VerilatorBackendAnnotation,
      )
    ) { c =>
      c.clock.setTimeout(0)
      c.registers(2).expect(0x00000000L.S)
      c.clock.step(1)
      c.registers(2).expect(0x00001000L.S)
      c.clock.step(1)
      c.registers(3).expect(0x00001004L.S)
    }
    new File(filename).delete()
  }

  it should "load program and end with 25 (0x19) in mem address 100 (0x64)" in {
    val filename = "./gcc/riscvtest.mem"
    test(new ControlSingleWrapper(32, 1 * 1024, 1 * 1024, filename)).withAnnotations(
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
      // Check Memory address 96 (0x60)
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
}
