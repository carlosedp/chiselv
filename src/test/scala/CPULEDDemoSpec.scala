package chiselv

import chiseltest._
import chiseltest.experimental._
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
) extends CPUSingleCycle(cpuFrequency, bitWidth, instructionMemorySize, memorySize, memoryFile) {
  val registers = expose(registerBank.regs)
  val pc        = expose(PC.pc)
}

class CPULEDDemoSpec extends AnyFlatSpec with ChiselScalatestTester with should.Matchers {
  behavior of "CPULEDDemoSpec"

  val cpuFrequency          = 10000
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

  it should "lit a LED connected to GPIO from gcc program" in {
    val filename = "./gcc/simpleLED/main.mem"
    defaultDut(filename) { c =>
      c.clock.setTimeout(2000)
      c.clock.step(1) // Step from initial PC
      while (c.pc.peek().litValue != 0) {
        c.clock.step(1)
      }
    }
  }
}
