package chiselv

import chiseltest._
import chiseltest.experimental._
import org.scalatest._

import flatspec._
import matchers._

// Extend the SOC module to add the observer for sub-module signals
class SOCWrapperDemo(
  cpuFrequency:          Int,
  bitWidth:              Int,
  instructionMemorySize: Int,
  memorySize:            Int,
  memoryFile:            String,
  ramFile:               String,
  numGPIO:               Int,
  // baudRate: Int,
) extends SOC(
    cpuFrequency,
    entryPoint = 0,
    bitWidth,
    instructionMemorySize,
    memorySize,
    memoryFile,
    ramFile,
    numGPIO,
  ) {
  val registers = expose(core.registerBank.regs)
  val pc        = expose(core.PC.pc)
}

class CPUDemoAppsSpec extends AnyFlatSpec with ChiselScalatestTester with should.Matchers {
  behavior of "CPUSOCDemoSpec"

  val cpuFrequency          = 10000
  val bitWidth              = 32
  val instructionMemorySize = 64 * 1024
  val memorySize            = 64 * 1024

  def defaultDut(memoryfile: String, ramFile: String = "") =
    test(
      new SOCWrapperDemo(
        cpuFrequency,
        bitWidth,
        instructionMemorySize,
        memorySize,
        memoryfile,
        ramFile,
        8,
        // 1200,
      ),
    )
      .withAnnotations(
        Seq(
          WriteVcdAnnotation,
          VerilatorBackendAnnotation,
        ),
      )

  it should "lit a LED connected to GPIO from gcc program" in {
    val filename = "./gcc/simpleLED/main.mem"
    defaultDut(filename) { c =>
      c.clock.setTimeout(10000)
      c.clock.step(1) // Step from initial PC
      while (c.pc.peekInt() != 0x0) {
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
    // while (c.pc.peekInt() != 0x90) { // 0x90 is the address of _halt
    //   c.clock.step(1)
    // }
    }
  }
}
