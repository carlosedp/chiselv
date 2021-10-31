package chiselv

import chiseltest._
import org.scalatest._

import flatspec._
import matchers._

// Extend the Control module to add the observer for sub-module signals
class CPUSingleCycleWrapperLED(
  cpuFrequency: Int,
  bitWidth: Int,
  instructionMemorySize: Int,
  memorySize: Int,
  memoryFile: String,
) extends CPUSingleCycle(cpuFrequency, bitWidth, instructionMemorySize, memorySize, memoryFile)
  with Observer {
  val registers = observe(registerBank.regs)
  val pc        = observe(PC.pc)
}

class CPULEDDemoSpec extends AnyFlatSpec with ChiselScalatestTester with should.Matchers {
  behavior of "CPULEDDemoSpec"

  val cpuFrequency          = 10000000
  val bitWidth              = 32
  val instructionMemorySize = 64 * 1024
  val memorySize            = 64 * 1024

  def defaultDut(memoryfile: String) =
    test(new CPUSingleCycleWrapperLED(cpuFrequency, bitWidth, instructionMemorySize, memorySize, memoryfile))
      .withAnnotations(
        Seq(
          WriteVcdAnnotation,
          VerilatorBackendAnnotation,
        )
      )

  it should "set GPIO direction and turn on LED on GPIO0" in {
    val filename = "./gcc/simpleLED/main.mem"
    defaultDut(filename) { c =>
      c.clock.setTimeout(500)
      c.clock.step(1) // Step from initial PC
      while (c.pc.peek().litValue != 0) {
        c.clock.step(1)
      }
    }
  }
}
