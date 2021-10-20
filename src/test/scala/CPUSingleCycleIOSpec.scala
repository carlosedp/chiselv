package chiselv

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

  val timerCounter = observe(timer0.counter)
}

class CPUSingleCycleIOSpec extends AnyFlatSpec with ChiselScalatestTester with should.Matchers {
  behavior of "GPIO"

  val cpuFrequency          = 25000000
  val bitWidth              = 32
  val instructionMemorySize = 1 * 1024
  val memorySize            = 1 * 1024
  val ms                    = cpuFrequency / 1000

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

  behavior of "Timer"
  it should "read timer and wait for 2 ms" in {
    val filename = "CPUSpecMemoryTestFileTimer0_1.hex"
    /*
    main:   lui x1, %hi(0x30003000)
            addi x2, x0, 2
    wait:   lw x3, 0(x1)
            bne x2, x3, wait
    cont:   addi x4, x0, 1
     */
    new PrintWriter(new File(filename)) {
      write("""
    300030b7
    00200113
    0000a183
    fe311ee3
    00100213
    """.stripMargin); close
    }
    defaultDut(filename) { c =>
      c.clock.setTimeout(0)
      c.registers(1).expect(0.S)
      c.registers(2).expect(0.S)
      c.registers(3).expect(0.S)
      c.timerCounter.expect(0.U)
      c.clock.step(1) // lui
      c.registers(1).expect(0x30003000L.S)
      c.clock.step(1) // addi
      c.registers(2).expect(2.S)
      // Check read from memory address 0x30003000L
      c.memReadAddr.expect(0x30003000L.U)
      c.memReadData.expect(0.U)
      c.clock.step(1) // lw
      c.registers(3).expect(0.S)
      c.clock.step(1)      // bne
      c.clock.step(2 * ms) // wait 2ms
      c.timerCounter.expect(2.U)
      c.registers(3).expect(2.S)
      c.clock.step(1) // addi
      c.registers(4).expect(1.S)
    }
    new File(filename).delete()
  }

  it should "reset timer after 1ms and wait for 1 ms" in {
    val filename = "CPUSpecMemoryTestFileTimer0_2.hex"
    /*
    main:   lui x1, %hi(0x30003000)
            addi x2, x0, 1
    wait:   lw x3, 0(x1)
            bne x2, x3, wait
    cont:   sw x0, 0(x1)
    wait2:  lw x3, 0(x1)
            bne x2, x3, wait2
    cont2:  addi x3, x0, 2
     */
    new PrintWriter(new File(filename)) {
      write("""
      300030b7
      00100113
      0000a183
      fe311ee3
      0000a023
      0000a183
      fe311ee3
      00200193
    """.stripMargin); close
    }
    defaultDut(filename) { c =>
      c.clock.setTimeout(0)
      c.registers(1).expect(0.S)
      c.registers(2).expect(0.S)
      c.registers(3).expect(0.S)
      c.timerCounter.expect(0.U)
      c.clock.step(1) // lui
      c.registers(1).expect(0x30003000L.S)
      c.clock.step(1) // addi
      c.registers(2).expect(1.S)
      // Check read from memory address 0x30003000L
      c.memReadAddr.expect(0x30003000L.U)
      c.memReadData.expect(0.U)
      c.registers(3).expect(0.S)
      c.clock.step(ms) // wait 1ms
      c.timerCounter.expect(1.U)
      c.registers(3).expect(1.S)
      c.clock.step(1)
      // Check write to memory address 0x30003000L (reset)
      c.memWriteAddr.expect(0x30003000L.U)
      c.memWriteData.expect(0.U)
      c.clock.step(1) // sw
      c.timerCounter.expect(0.U)
      c.registers(3).expect(0.S)
      c.clock.step(ms) // wait 1ms
      c.timerCounter.expect(1.U)
      c.clock.step(1) // lw
      c.clock.step(1) // bne
      c.clock.step(1) // addi
      c.registers(3).expect(2.S)
    }
    new File(filename).delete()
  }
}
