package chiselv

import chiseltest._
import chiseltest.experimental._
import com.carlosedp.scalautils.riscvassembler.RISCVAssembler
import org.scalatest._

import flatspec._
import matchers._

// Extend the Control module to add the observer for sub-module signals
class CPUSingleCycleIOWrapper(
  memoryFile: String,
) extends SOC(
    cpuFrequency = 25000000,
    entryPoint = 0,
    bitWidth = 32,
    instructionMemorySize = 1 * 1024,
    dataMemorySize = 1 * 1024,
    memoryFile = memoryFile,
    numGPIO = 8,
  ) {
  val registers    = expose(core.registerBank.regs)
  val pc           = expose(core.PC.pc)
  val memWriteAddr = expose(core.memoryIOManager.io.MemoryIOPort.writeAddr)
  val memWriteData = expose(core.memoryIOManager.io.MemoryIOPort.writeData)
  val memReadAddr  = expose(core.memoryIOManager.io.MemoryIOPort.readAddr)
  val memReadData  = expose(core.memoryIOManager.io.MemoryIOPort.readData)

  val GPIO0_value     = expose(core.GPIO0.GPIO)
  val GPIO0_direction = expose(core.GPIO0.direction)
  val timerCounter    = expose(core.timer0.counter)
}

class CPUSingleCycleIOSpec
  extends AnyFlatSpec
  with ChiselScalatestTester
  with BeforeAndAfterEach
  with BeforeAndAfter
  with should.Matchers {
  val cpuFrequency = 25000000
  val ms           = cpuFrequency / 1000
  var memoryfile: os.Path = _
  val tmpdir = "tmphex"

  before {
    os.makeDir.all(os.pwd / tmpdir)
  }
  after {
    try {
      os.remove(os.pwd / tmpdir)
    } catch {
      case _: Exception => // not empty, ignore
    }
  }
  override def beforeEach(): Unit =
    memoryfile = os.pwd / tmpdir / (scala.util.Random.alphanumeric.filter(_.isLetter).take(15).mkString + ".hex")
  override def afterEach(): Unit =
    os.remove.all(memoryfile)

  def defaultDut(prog: String) = {
    val hex = RISCVAssembler.fromString(prog)
    os.write(memoryfile, hex)
    test(new CPUSingleCycleIOWrapper(memoryFile = memoryfile.toString))
      .withAnnotations(
        Seq(
          WriteVcdAnnotation,
          VerilatorBackendAnnotation, // GPIO needs Verilator backend
        ),
      )
  }

  behavior of "GPIO"

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
      c.registers(1).peekInt() should be(0)
      c.registers(2).peekInt() should be(0)
      c.registers(3).peekInt() should be(0)
      c.registers(4).peekInt() should be(0)
      c.clock.step(1) // lui
      c.registers(1).peekInt() should be(0x30001000)
      c.clock.step(1) // addi
      c.registers(5).peekInt() should be(0xffffffff)
      c.clock.step(1) // addi
      c.registers(3).peekInt() should be(1)
      c.clock.step(1) // addi
      c.registers(4).peekInt() should be(7)
      // Check memory address 0x30001000L for direction (GPIO0)
      c.memWriteAddr.peekInt() should be(0x30001000L)
      c.memWriteData.peekInt() should be(0xffffffffL)
      c.clock.step(1) // sw
      c.clock.step(1) // add
      c.GPIO0_direction.peekInt() should be(0xffffffffL)
      c.registers(2).peekInt() should be(1)
      c.memWriteAddr.peekInt() should be(0x30001004L)
      c.memWriteData.peekInt() should be(1)
      c.clock.step(1) // sw
      c.GPIO0_value.peekInt() should be(1)
      c.clock.step(1) // jal
      c.clock.step(1) // add
      c.registers(2).peekInt() should be(2)
      c.memWriteAddr.peekInt() should be(0x30001004L)
      c.memWriteData.peekInt() should be(2)
      c.clock.step(1) // sw
      c.GPIO0_value.peekInt() should be(2)
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
      c.registers(1).peekInt() should be(0)
      c.registers(2).peekInt() should be(0)
      c.registers(3).peekInt() should be(0)
      c.timerCounter.peekInt() should be(0)
      c.clock.step(1) // lui
      c.registers(1).peekInt() should be(0x30003000)
      c.clock.step(1) // addi
      c.registers(2).peekInt() should be(2)
      // Check read from memory address 0x30003000L
      c.memReadAddr.peekInt() should be(0x30003000L)
      c.memReadData.peekInt() should be(0)
      c.clock.step(1) // lw
      c.registers(3).peekInt() should be(0)
      c.clock.step(1)      // bne
      c.clock.step(2 * ms) // wait 2ms
      c.timerCounter.peekInt() should be(2)
      c.registers(3).peekInt() should be(2)
      c.clock.step(1) // addi
      c.registers(4).peekInt() should be(1)
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
      c.registers(1).peekInt() should be(0)
      c.registers(2).peekInt() should be(0)
      c.registers(3).peekInt() should be(0)
      c.timerCounter.peekInt() should be(0)
      c.clock.step(1) // lui
      c.registers(1).peekInt() should be(0x30003000)
      c.clock.step(1) // addi
      c.registers(2).peekInt() should be(1)
      // Check read from memory address 0x30003000L
      c.memReadAddr.peekInt() should be(0x30003000L)
      c.memReadData.peekInt() should be(0)
      c.registers(3).peekInt() should be(0)
      c.clock.step(ms) // wait 1ms
      c.timerCounter.peekInt() should be(1)
      c.registers(3).peekInt() should be(1)
      c.clock.step(1)
      c.clock.step(1)
      c.clock.step(1) // sw
      // Check write to memory address 0x30003000L (reset)
      c.memWriteAddr.peekInt() should be(0x30003000L)
      c.memWriteData.peekInt() should be(0)
      c.timerCounter.peekInt() should be(0)
      c.registers(3).peekInt() should be(0)
      c.clock.step(ms) // wait 1ms
      c.timerCounter.peekInt() should be(1)
      c.clock.step(1) // lw
      c.clock.step(1) // bne
      c.clock.step(1) // addi
      c.registers(3).peekInt() should be(2)
    }
  }
}
