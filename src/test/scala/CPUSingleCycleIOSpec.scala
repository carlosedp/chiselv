package chiselv

import chiseltest._
import chiseltest.experimental._
import org.scalatest._

import com.carlosedp.scalautils.riscvassembler._

import java.io.{File, PrintWriter}
import java.nio.file.{Files, Paths, DirectoryNotEmptyException}

import flatspec._
import matchers._

// Extend the Control module to add the observer for sub-module signals
class CPUSingleCycleIOWrapper(
  cpuFrequency:          Int,
  bitWidth:              Int,
  instructionMemorySize: Int,
  memorySize:            Int,
  memoryFile:            String,
) extends SOC(cpuFrequency, bitWidth, instructionMemorySize, memorySize, memoryFile) {
  val registers    = expose(core.registerBank.regs)
  val memWriteAddr = expose(core.memoryIOManager.io.MemoryIOPort.writeAddr)
  val memWriteData = expose(core.memoryIOManager.io.MemoryIOPort.writeData)
  val memReadAddr  = expose(core.memoryIOManager.io.MemoryIOPort.readAddr)
  val memReadData  = expose(core.memoryIOManager.io.MemoryIOPort.readData)

  val GPIO0_value     = expose(core.GPIO0.GPIO)
  val GPIO0_direction = expose(core.GPIO0.direction)

  val timerCounter = expose(core.timer0.counter)
}

class CPUSingleCycleIOSpec
  extends AnyFlatSpec
  with ChiselScalatestTester
  with BeforeAndAfterEach
  with BeforeAndAfter
  with should.Matchers {
  behavior of "GPIO"

  val cpuFrequency          = 25000000
  val bitWidth              = 32
  val instructionMemorySize = 1 * 1024
  val memorySize            = 1 * 1024
  val ms                    = cpuFrequency / 1000
  var memoryfile            = ""
  val tmpdir                = "tmphex"

  before {
    Files.createDirectories(Paths.get(tmpdir));
  }
  after {
    try {
      Files.deleteIfExists(Paths.get(tmpdir));
    } catch {
      case _: DirectoryNotEmptyException =>
      // println("Directory not empty")
    }
  }
  override def beforeEach(): Unit =
    memoryfile =
      Paths.get(tmpdir, scala.util.Random.alphanumeric.filter(_.isLetter).take(15).mkString + ".hex").toString()
  override def afterEach(): Unit = {
    val _ = new File(memoryfile).delete()
  }

  def defaultDut(prog: String) = {
    val hex = RISCVAssembler.fromString(prog)
    new PrintWriter(new File(memoryfile)) { write(hex); close }
    test(new CPUSingleCycleIOWrapper(cpuFrequency, bitWidth, instructionMemorySize, memorySize, memoryfile))
      .withAnnotations(
        Seq(
          WriteVcdAnnotation,
          VerilatorBackendAnnotation,
        )
      )
  }

  it should "write to GPIO0" in {
    val prog = """
    lui x1, 0x30001000
    addi x5, x0, -1
    addi x3, x3, 1
    addi x4, x0, 7
    sw x5, 0(x1)
    add x2, x2, x3
    sw x2, 4(x1)
    jal x0, -8
    """
    defaultDut(prog) { c =>
      c.clock.setTimeout(0)
      c.registers(1).peek().litValue should be(0)
      c.registers(2).peek().litValue should be(0)
      c.registers(3).peek().litValue should be(0)
      c.registers(4).peek().litValue should be(0)
      c.clock.step(1) // lui
      c.registers(1).peek().litValue should be(0x30001000)
      c.clock.step(1) // addi
      c.registers(5).peek().litValue should be(0xffffffff)
      c.clock.step(1) // addi
      c.registers(3).peek().litValue should be(1)
      c.clock.step(1) // addi
      c.registers(4).peek().litValue should be(7)
      // Check memory address 0x30001000L for direction (GPIO0)
      c.memWriteAddr.peek().litValue should be(0x30001000L)
      c.memWriteData.peek().litValue should be(0xffffffffL)
      c.clock.step(1) // sw
      c.clock.step(1) // add
      c.GPIO0_direction.peek().litValue should be(0xffffffffL)
      c.registers(2).peek().litValue should be(1)
      c.memWriteAddr.peek().litValue should be(0x30001004L)
      c.memWriteData.peek().litValue should be(1)
      c.clock.step(1) // sw
      c.GPIO0_value.peek().litValue should be(1)
      c.clock.step(1) // jal
      c.clock.step(1) // add
      c.registers(2).peek().litValue should be(2)
      c.memWriteAddr.peek().litValue should be(0x30001004L)
      c.memWriteData.peek().litValue should be(2)
      c.clock.step(1) // sw
      c.GPIO0_value.peek().litValue should be(2)
    }
  }

  behavior of "Timer"
  it should "read timer and wait for 2 ms" in {
    val prog = """
    main:   lui x1, 0x30003000
            addi x2, x0, 2
    wait:   lw x3, 0(x1)
            bne x2, x3, -4
    cont:   addi x4, x0, 1
    """
    defaultDut(prog) { c =>
      c.clock.setTimeout(0)
      c.registers(1).peek().litValue should be(0)
      c.registers(2).peek().litValue should be(0)
      c.registers(3).peek().litValue should be(0)
      c.timerCounter.peek().litValue should be(0)
      c.clock.step(1) // lui
      c.registers(1).peek().litValue should be(0x30003000)
      c.clock.step(1) // addi
      c.registers(2).peek().litValue should be(2)
      // Check read from memory address 0x30003000L
      c.memReadAddr.peek().litValue should be(0x30003000L)
      c.memReadData.peek().litValue should be(0)
      c.clock.step(1) // lw
      c.registers(3).peek().litValue should be(0)
      c.clock.step(1)      // bne
      c.clock.step(2 * ms) // wait 2ms
      c.timerCounter.peek().litValue should be(2)
      c.registers(3).peek().litValue should be(2)
      c.clock.step(1) // addi
      c.registers(4).peek().litValue should be(1)
    }
  }

  it should "reset timer after 1ms and wait for 1 ms" in {
    val prog = """
    main:   lui x1, 0x30003000
            addi x2, x0, 1
    wait:   lw x3, 0(x1)
            bne x2, x3, -4
    cont:   sw x0, 0(x1)
    wait2:  lw x3, 0(x1)
            bne x2, x3, -4
    cont2:  addi x3, x0, 2
    """
    defaultDut(prog) { c =>
      c.clock.setTimeout(0)
      c.registers(1).peek().litValue should be(0)
      c.registers(2).peek().litValue should be(0)
      c.registers(3).peek().litValue should be(0)
      c.timerCounter.peek().litValue should be(0)
      c.clock.step(1) // lui
      c.registers(1).peek().litValue should be(0x30003000)
      c.clock.step(1) // addi
      c.registers(2).peek().litValue should be(1)
      // Check read from memory address 0x30003000L
      c.memReadAddr.peek().litValue should be(0x30003000L)
      c.memReadData.peek().litValue should be(0)
      c.registers(3).peek().litValue should be(0)
      c.clock.step(ms) // wait 1ms
      c.timerCounter.peek().litValue should be(1)
      c.registers(3).peek().litValue should be(1)
      c.clock.step(1)
      c.clock.step(1)
      c.clock.step(1) // sw
      // Check write to memory address 0x30003000L (reset)
      c.memWriteAddr.peek().litValue should be(0x30003000L)
      c.memWriteData.peek().litValue should be(0)
      c.timerCounter.peek().litValue should be(0)
      c.registers(3).peek().litValue should be(0)
      c.clock.step(ms) // wait 1ms
      c.timerCounter.peek().litValue should be(1)
      c.clock.step(1) // lw
      c.clock.step(1) // bne
      c.clock.step(1) // addi
      c.registers(3).peek().litValue should be(2)
    }
  }
}
