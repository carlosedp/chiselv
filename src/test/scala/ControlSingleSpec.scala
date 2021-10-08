import chiseltest._
import org.scalatest._

import java.io.{File, PrintWriter}

import flatspec._
import matchers._

// Extend the Control module to add the observer for sub-module signals
class ControlSingleWrapper(bitWidth: Int, memorySize: Int, memoryFile: String)
  extends ControlSingle(bitWidth, memorySize, memoryFile)
  with Observer {
  val registers = observe(registerBank.regs)
}

class ControlSingleSpec extends AnyFlatSpec with ChiselScalatestTester with should.Matchers {
  behavior of "ControlSingle"

  it should "load simple instructions" in {
    // Create memory test file with 32bit address space
    // Instructions are: addi x1 x0 32 ,lui x2 0xc0000000, lbu x3 0(x1), jal x0, -16
    val filename = "CPUSpecMemoryTestFile.hex"
    new PrintWriter(new File(filename)) { write("02000093\r\nc0000137\r\n0000c183\r\nff1ff06f\r\n"); close }
    test(new ControlSingleWrapper(32, 1 * 1024, filename)).withAnnotations(
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
    test(new ControlSingleWrapper(32, 1 * 1024, filename)).withAnnotations(
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
    }
  }
  it should "load program and should have value 25 in mem address 100" in {
    val filename = "./gcc/riscvtest.mem"
    test(new ControlSingleWrapper(32, 1 * 1024, filename)).withAnnotations(
      Seq(
        WriteVcdAnnotation,
        VerilatorBackendAnnotation,
      )
    ) { c =>
      c.clock.setTimeout(0)
      c.clock.step(50)
    }
  }
}
