// import chisel3._
import chiseltest._
import org.scalatest._
import treadle.ClockInfoAnnotation
import treadle.executable.ClockInfo

import java.io.{File, PrintWriter}

import flatspec._
import matchers._

class ControlSpec extends AnyFlatSpec with ChiselScalatestTester with should.Matchers {
  behavior of "Control"

  it should "load simple instructions" in {
    // Create memory test file with 32bit address space
    // Instructions are: addi x1 x0 32 ,lui x2 0xc0000000, lbu x3 0(x1), jal x0, -16
    val filename = "CPUSpecMemoryTestFile.hex"
    new PrintWriter(new File(filename)) { write("02000093\r\nc0000137\r\n0000c183\r\nff1ff06f\r\n"); close }
    test(new ControlSingle(32, 4 * 1024, filename)).withAnnotations(
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
    test(new ControlSingle(32, 4 * 1024, filename)).withAnnotations(
      Seq(
        WriteVcdAnnotation,
        VerilatorBackendAnnotation,
        ClockInfoAnnotation(Seq(ClockInfo(period = 2))),
      )
    ) { c =>
      c.clock.setTimeout(0)
      // BoringUtils.addSource(clock_uint.asBool, "clock_bore")

      c.clock.step(1)

      c.clock.step(100)
    }
  }
  it should "load program and should have value 25 in mem address 100" in {
    val filename = "./gcc/riscvtest.mem"
    test(new ControlSingle(32, 4 * 1024, filename)).withAnnotations(
      Seq(
        WriteVcdAnnotation,
        VerilatorBackendAnnotation,
        ClockInfoAnnotation(Seq(ClockInfo(period = 2))),
      )
    ) { c =>
      c.clock.setTimeout(0)
      c.clock.step(100)
    }
  }
}
