import chisel3._
import chiseltest._
import org.scalatest._

import java.io.{File, PrintWriter}

import flatspec._
import matchers._

// Extend the Control module to add the observer for sub-module signals
class CPUSingleCycleIOWrapper(
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

  val GPIO0_value     = observe(GPIO0.GPIO)
  val GPIO0_direction = observe(GPIO0.direction)
}

class CPUSingleCycleIOSpec extends AnyFlatSpec with ChiselScalatestTester with should.Matchers {
  behavior of "CPUSingleIOCycle"

  val cpuFrequency          = 25000000
  val bitWidth              = 32
  val instructionMemorySize = 1 * 1024
  val memorySize            = 1 * 1024

  def defaultDut(memoryfile: String) =
    test(new CPUSingleCycleIOWrapper(cpuFrequency, bitWidth, instructionMemorySize, memorySize, memoryfile))
      .withAnnotations(
        Seq(
          WriteVcdAnnotation,
          VerilatorBackendAnnotation,
        )
      )

  it should "write to GPIO0" in {
    val filename = "CPUSpecMemoryTestFileGPIO0.hex"
    /*
            lui x1, %hi(0x30001000)
            addi x5, x0, -1
            addi x3, x3, 1
            addi x4, x0, 7
            sw x5, 0(x1)
    loop:   add x2, x2, x3
            sw x2, 4(x1)
            jal x0, loop
     */
    new PrintWriter(new File(filename)) {
      write("""
      300010b7
      fff00293
      00118193
      00700213
      0050a023
      00310133
      0020a223
      ff9ff06f
    """.stripMargin); close
    }
    defaultDut(filename) { c =>
      c.clock.setTimeout(0)
      c.registers(1).expect(0.S)
      c.registers(2).expect(0.S)
      c.registers(3).expect(0.S)
      c.registers(4).expect(0.S)
      c.clock.step(1) // lui
      c.registers(1).expect(0x30001000L.S)
      c.clock.step(1) // addi
      c.registers(5).expect(0xffffffffL.S)
      c.clock.step(1) // addi
      c.registers(3).expect(1.S)
      c.clock.step(1) // addi
      c.registers(4).expect(7.S)
      // Check memory address 0x30001000L for direction (GPIO0)
      c.memWriteAddr.expect(0x30001000L.U)
      c.memWriteData.expect(0xffffffffL.U)
      c.clock.step(1) // sw
      c.clock.step(1) // add
      c.GPIO0_direction.expect(0xffffffffL.U)
      c.registers(2).expect(1.S)
      c.memWriteAddr.expect(0x30001004L.U)
      c.memWriteData.expect(1.U)
      c.clock.step(1) // sw
      c.GPIO0_value.expect(1.U)
      c.clock.step(1) // jal
      c.clock.step(1) // add
      c.registers(2).expect(2.S)
      c.memWriteAddr.expect(0x30001004L.U)
      c.memWriteData.expect(2.U)
      c.clock.step(1) // sw
      c.GPIO0_value.expect(2.U)
    }
    new File(filename).delete()
  }

}
