package chiselv

import chiseltest._
import chiseltest.experimental._
import org.scalatest._

import flatspec._
import matchers._

// Extend the SOC module to add the observer for sub-module signals
class SOCWrapperDemo(
  memoryFile: String,
  ramFile:    String
  // baudRate: Int,
) extends SOC(
    cpuFrequency = 10000,
    instructionMemorySize = 64 * 1024,
    dataMemorySize = 64 * 1024,
    memoryFile = memoryFile,
    ramFile = ramFile
  ) {
  val registers = expose(core.registerBank.regs)
  val pc        = expose(core.PC.pc)
}

class CPUDemoAppsSpec extends AnyFlatSpec with ChiselScalatestTester with should.Matchers {
  def defaultDut(memoryfile: String, ramFile: String = "") =
    test(
      new SOCWrapperDemo(
        memoryfile,
        ramFile
      )
    )
      .withAnnotations(
        Seq(
          WriteVcdAnnotation,
          VerilatorBackendAnnotation
        )
      )

  behavior of "CPULEDDemoSpec"

  it should "lit a LED connected to GPIO from gcc program" in {
    val filename = "./gcc/simpleLED/main.mem"
    defaultDut(filename) { c =>
      c.clock.setTimeout(10000)
      c.clock.step(1) // Step from initial PC
      while (c.pc.peek().litValue != 0x0) {
        c.clock.step(1)
      }
    }
  }

  it should "blink LED connected to GPIO0 from gcc program" in {
    val filename = "./gcc/blinkLED/main-rom.mem"
    defaultDut(filename) { c =>
      c.clock.setTimeout(3000)
      c.clock.step(2000)
    }
  }

  behavior of "UARTHello"
  it should "print to UART0 from gcc program" in {
    val filename     = "./gcc/helloUART/main-rom.mem"
    val filename_ram = "./gcc/helloUART/main-ram.mem"
    defaultDut(filename, filename_ram) { c =>
      c.clock.setTimeout(30000)
      c.clock.step(25000)
    // while (c.pc.peek().litValue != 0x90) { // 0x90 is the address of _halt
    //   c.clock.step(1)
    // }
    }
  }
}
