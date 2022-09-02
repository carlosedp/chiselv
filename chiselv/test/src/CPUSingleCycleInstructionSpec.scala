package chiselv

import chiseltest._
import chiseltest.experimental._
import com.carlosedp.scalautils.riscvassembler.RISCVAssembler
import org.scalatest._

import flatspec._
import matchers._

// Extend the Control module to add the observer for sub-module signals
class CPUSingleCycleInstWrapper(
  memoryFile: String,
) extends SOC(
    cpuFrequency = 25000000,
    entryPoint = 0,
    bitWidth = 32,
    instructionMemorySize = 1 * 1024,
    dataMemorySize = 1 * 1024,
    memoryFile = memoryFile,
    numGPIO = 0,
  ) {
  val registers    = expose(core.registerBank.regs)
  val pc           = expose(core.PC.pc)
  val memWriteAddr = expose(core.memoryIOManager.io.MemoryIOPort.writeAddr)
  val memWriteData = expose(core.memoryIOManager.io.MemoryIOPort.writeData)
  val memReadAddr  = expose(core.memoryIOManager.io.MemoryIOPort.readAddr)
  val memReadData  = expose(core.memoryIOManager.io.MemoryIOPort.readData)
}

class CPUSingleCycleInstructionSpec
  extends AnyFlatSpec
  with ChiselScalatestTester
  with BeforeAndAfterEach
  with BeforeAndAfter
  with should.Matchers {
  val memReadLatency  = 1
  val memWriteLatency = 1
  var memoryfile: os.Path = _
  val tmpdir = os.pwd / "tmphex"

  before {
    os.makeDir.all(tmpdir)
  }
  after {
    try {
      os.remove(tmpdir)
    } catch {
      case _: Exception => // not empty, ignore
    }
  }
  override def beforeEach(): Unit =
    memoryfile = tmpdir / (scala.util.Random.alphanumeric.filter(_.isLetter).take(15).mkString + ".hex")
  override def afterEach(): Unit =
    os.remove.all(memoryfile)

  def defaultDut(prog: String) = {
    // Generate the hex file from asm source
    val hex = RISCVAssembler.fromString(prog)
    os.write(memoryfile, hex)
    test(new CPUSingleCycleInstWrapper(memoryfile.relativeTo(os.pwd).toString))
      .withAnnotations(
        Seq(
          WriteVcdAnnotation,
        ),
      )
  }

  behavior of "CPUSingleCycleInstruction"

  it should "validate ADD/ADDI instructions" in {
    val prog = """
    addi x1, x1, 1
    addi x2, x2, 1
    add x3, x1, x2
    """
    defaultDut(prog) { c =>
      c.clock.setTimeout(0)
      c.registers(1).peekInt() should be(0)
      c.registers(2).peekInt() should be(0)
      c.registers(3).peekInt() should be(0)
      c.clock.step(1)
      c.registers(1).peekInt() should be(1)
      c.clock.step(1)
      c.registers(2).peekInt() should be(1)
      c.clock.step(1)
      c.registers(3).peekInt() should be(2)
    }
  }

  it should "validate Branch instructions" in {
    val prog = """
    addi x1, x0, 4
    addi x2, x0, 4
    addi x3, x0, 2
    beq x1, x2, +8
    jal x0, 0
    bne x2, x3, +8
    jal x0, 0
    blt x3, x2, +8
    jal x0, 0
    bge x2, x3, +8
    jal x0, 0
     """
    defaultDut(prog) { c =>
      c.clock.setTimeout(0)
      c.registers(1).peekInt() should be(0x0)
      c.registers(2).peekInt() should be(0x0)
      c.registers(3).peekInt() should be(0x0)
      c.clock.step(1)
      c.registers(1).peekInt() should be(4)
      c.clock.step(1)
      c.registers(2).peekInt() should be(4)
      c.clock.step(1)
      c.registers(3).peekInt() should be(2)
      c.pc.peekInt() should be(0x0c)
      c.clock.step(1)
      c.pc.peekInt() should be(0x14)
      c.clock.step(1)
      c.pc.peekInt() should be(0x1c)
      c.clock.step(1)
      c.pc.peekInt() should be(0x24)
      c.clock.step(1)
      c.pc.peekInt() should be(0x2c)
    }
  }

  it should "validate JAL/JALR instructions" in {
    val prog = """
      jal x1, +8
      nop
      jalr x2, x1, +2044
      """
    defaultDut(prog) { c =>
      c.pc.peekInt() should be(0)
      c.registers(1).peekInt() should be(0)
      c.registers(2).peekInt() should be(0)
      c.clock.step(1) // jal (next nop is skipped)
      c.pc.peekInt() should be(8)
      c.registers(1).peekInt() should be(4)
      c.clock.step(1) // jalr
      c.pc.peekInt() should be(2048)
      c.registers(2).peekInt() should be(0xc)
    }
  }

  it should "validate LUI instruction" in {
    val prog = """
    lui x2, 0xc0000000
    """
    defaultDut(prog) { c =>
      c.clock.setTimeout(0)
      c.registers(2).peekInt() should be(0x00000000)
      c.clock.step(1)
      c.registers(2).peekInt() should be(0xc0000000)
    }
  }

  it should "validate AUIPC instruction" in {
    val prog = """
    auipc x2, 4096
    auipc x3, 4096
    """
    defaultDut(prog) { c =>
      c.clock.setTimeout(0)
      c.registers(2).peekInt() should be(0x00000000)
      c.clock.step(1)
      c.registers(2).peekInt() should be(0x00001000)
      c.clock.step(1)
      c.registers(3).peekInt() should be(0x00001004)
    }
  }

  it should "validate SW instruction" in {
    val prog = """
      lui x1, 0x80000000
      addi x1, x1, 20
      addi x2, x0, 291
      sw x2, 0(x1)
      lw x3, 0(x1)
    """
    defaultDut(prog) { c =>
      c.registers(1).peekInt() should be(0)
      c.clock.step(1) // lui
      c.registers(1).peekInt() should be(0x80000000)
      c.clock.step(1) // addi
      c.registers(1).peekInt() should be(0x80000014)
      c.clock.step(1) // addi
      c.registers(2).peekInt() should be(0x123)
      // Check memory address 0x14 with offset
      c.memWriteAddr.peekInt() should be(0x80000000L + 0x14)
      c.memWriteData.peekInt() should be(0x123)
      c.clock.step(1 + memWriteLatency) // sw
      // Check memory address 0x14 with offset
      c.memReadAddr.peekInt() should be(0x80000000L + 0x14)
      c.memReadData.peekInt() should be(0x123)
      c.clock.step(2 + memReadLatency) // lw
      c.registers(3).peekInt() should be(0x123)
    }
  }

  it should "validate SH instruction" in {
    val prog = """
    lui x1, 0x80000000
    lui x2, 0x12345000
    addi x2, x2, 1656
    addi x3, x3, -1
    sw x3, 48(x1)  // Memory offset 48 (0x30) should be 0xffffffff
    sh x2, 52(x1)  // Memory offset 52 (0x34) should be 0x00005678
    sh x2, 48(x1)  // Memory offset 48 (0x30) should be 0xffff5678
    sh x2, 54(x1)  // Memory offset 52 (0x34) should be 0x56785678
    lw x4, 48(x1)  // x4 should be 0xffff5678
    lw x5, 52(x1)  // x5 should be 0x56785678
    """
    defaultDut(prog) { c =>
      c.registers(1).peekInt() should be(0)
      c.clock.step(1) // lui
      c.registers(1).peekInt() should be(0x80000000)
      c.clock.step(1) // lui
      c.registers(2).peekInt() should be(0x12345000)
      c.clock.step(1) // addi
      c.registers(2).peekInt() should be(0x12345678)
      c.clock.step(1) // addi
      c.registers(3).peekInt() should be(0xffffffff)
      // Check memory address offset 0x30
      c.memWriteAddr.peekInt() should be(0x80000000L + 0x30)
      c.memWriteData.peekInt() should be(0xffffffffL)
      c.clock.step(1 + memWriteLatency) // sw
      // Check memory address offset 0x34
      c.memWriteAddr.peekInt() should be(0x80000000L + 0x34)
      c.memWriteData.peekInt() should be(0x5678L)
      c.clock.step(1 + memWriteLatency) // sh
      // Check memory address 0x30
      c.memWriteAddr.peekInt() should be(0x80000000L + 0x30)
      c.memWriteData.peekInt() should be(0x5678L)
      c.clock.step(1 + memWriteLatency) // sh
      // Check memory address offset 0x34
      c.memWriteAddr.peekInt() should be(0x80000000L + 0x36)
      c.memWriteData.peekInt() should be(0x5678L)
      c.clock.step(1 + memWriteLatency) // sh
      // Check memory address 0x30
      c.clock.step(1) // lw - stall
      c.memReadAddr.peekInt() should be(0x80000000L + 0x30)
      c.memReadData.peekInt() should be(0xffff5678L)
      c.clock.step(1) // lw
      c.clock.step(1) // lw - stall
      c.registers(4).peekInt() should be(0xffff5678)
      // Check memory address 0x34
      c.memReadAddr.peekInt() should be(0x80000000L + 0x34)
      c.memReadData.peekInt() should be(0x56785678L)
      c.clock.step(1) // lw
      c.registers(5).peekInt() should be(0x56785678)
      c.clock.step(10) // padding
    }
  }

  it should "validate SB instruction" in {
    val prog = """
    lui x1, 0x80000000
    lui x2, 0x12345000
    addi x2, x2, 1656
    addi x3, x3, -1
    sw x3, 48(x1)  // Memory offset 48 (0x30) should be 0xffffffff
    sb x2, 52(x1)  // Memory offset 52 (0x34) should be 0x00000078
    sb x2, 48(x1)  // Memory offset 48 (0x30) should be 0xffffff78
    sb x2, 54(x1)  // Memory offset 52 (0x34) should be 0x00780078
    sb x2, 51(x1)  // Memory offset 51 (0x33) should be 0x78ffff78
    lw x4, 48(x1)  // x4 should be 0x78ffff78
    lw x5, 52(x1)  // x5 should be 0x00780078
    """
    defaultDut(prog) { c =>
      c.registers(1).peekInt() should be(0)
      c.clock.step(1) // lui
      c.registers(1).peekInt() should be(0x80000000)
      c.clock.step(1) // lui
      c.registers(2).peekInt() should be(0x12345000)
      c.clock.step(1) // addi
      c.registers(2).peekInt() should be(0x12345678)
      c.clock.step(1) // addi
      c.registers(3).peekInt() should be(0xffffffff)
      // Check memory address offset 0x30
      c.memWriteAddr.peekInt() should be(0x80000000L + 0x30)
      c.memWriteData.peekInt() should be(0xffffffffL)
      c.clock.step(1 + memWriteLatency) // sw
      // Check memory address offset 0x34
      c.memWriteAddr.peekInt() should be(0x80000000L + 0x34)
      c.memWriteData.peekInt() should be(0x78L)
      c.clock.step(1 + memWriteLatency) // sb
      // Check memory address 0x30
      c.memWriteAddr.peekInt() should be(0x80000000L + 0x30)
      c.memWriteData.peekInt() should be(0x78L)
      c.clock.step(1 + memWriteLatency) // sb
      // Check memory address offset 0x34
      c.memWriteAddr.peekInt() should be(0x80000000L + 0x36)
      c.memWriteData.peekInt() should be(0x78L)
      c.clock.step(1 + memWriteLatency) // sb
      // Check memory address offset 0x33
      c.memWriteAddr.peekInt() should be(0x80000000L + 0x33)
      c.memWriteData.peekInt() should be(0x78L)
      c.clock.step(1 + memWriteLatency) // sb
      // Check memory address 0x30
      c.memReadAddr.peekInt() should be(0x80000000L + 0x30)
      c.clock.step(1) // lw
      c.memReadData.peekInt() should be(0x78ffff78L)
      c.clock.step(1) // lw
      c.registers(4).peekInt() should be(0x78ffff78)
      // Check memory address 0x34
      c.memReadAddr.peekInt() should be(0x80000000L + 0x34)
      c.clock.step(1) // lw
      c.memReadData.peekInt() should be(0x00780078L)
      c.clock.step(1) // lw
      c.registers(5).peekInt() should be(0x00780078)
      c.clock.step(10) // padding
    }
  }

  it should "validate LW instruction" in {
    val prog = """
    lui x1, 0xf0f0f000
    addi x1, x1, 240
    lui x2, 0x80000000
    sw x1, 0(x2)
    lw x3, 0(x2)
    """
    defaultDut(prog) { c =>
      c.registers(1).peekInt() should be(0)
      c.clock.step(1) // lui
      c.registers(1).peekInt() should be(0xf0f0f000)
      c.clock.step(1) // addi
      c.registers(1).peekInt() should be(0xf0f0f0f0)
      c.clock.step(1) // lui
      c.registers(2).peekInt() should be(0x80000000)
      // Check memory write at address 0x80000000L
      c.memWriteAddr.peekInt() should be(0x80000000L)
      c.memWriteData.peekInt() should be(0xf0f0f0f0L)
      c.clock.step(1 + memWriteLatency) // sw
      // Check memory read at address 0x20 (32)
      c.memReadAddr.peekInt() should be(0x80000000L)
      c.memReadData.peekInt() should be(0xf0f0f0f0L)
      c.clock.step(1 + memReadLatency) // lw
      // Check loaded data
      c.registers(3).peekInt() should be(0xf0f0f0f0)
      c.clock.step(5) // Paddding
    }
  }

  it should "validate LH instruction" in {
    val prog = """
      lui x1, 0xffff1000
      addi x1, x1, 564
      lui x2, 0x80000000
      sw x1, 0(x2)
      lh x3, 0(x2)
      lh x4, 2(x2)
      """
    defaultDut(prog) { c =>
      c.registers(1).peekInt() should be(0)
      c.clock.step(1)
      c.registers(1).peekInt() should be(0xffff1000)
      c.clock.step(1)
      c.registers(1).peekInt() should be(0xffff1234)
      c.clock.step(1)
      c.registers(2).peekInt() should be(0x80000000)
      // Check memory write at address 0x80000000L
      c.memWriteAddr.peekInt() should be(0x80000000L)
      c.memWriteData.peekInt() should be(0xffff1234L)
      c.clock.step(1 + memWriteLatency) // sw
      // Check memory read at address 0x80000000L
      c.memReadAddr.peekInt() should be(0x80000000L)
      c.memReadData.peekInt() should be(0x00001234L)
      c.clock.step(1 + memReadLatency) // lh
      // Check loaded data
      c.registers(3).peekInt() should be(0x00001234)
      // Check memory read at address 0x80000002L
      c.memReadAddr.peekInt() should be(0x80000000L + 0x2)
      c.memReadData.peekInt() should be(0x0000ffffL)
      c.clock.step(1 + memReadLatency) // lh
      c.registers(4).peekInt() should be(0xffffffff)
      c.clock.step(5) // Paddding
    }
  }

  it should "validate LHU instruction" in {
    val prog = """
    lui x1, 0xffff1000
    addi x1, x1, 564
    lui x2, 0x80000000
    sw x1, 0(x2)
    lhu x3, 0(x2)
    lhu x4, 2(x2)
    """
    defaultDut(prog) { c =>
      c.registers(1).peekInt() should be(0)
      c.clock.step(1)
      c.registers(1).peekInt() should be(0xffff1000)
      c.clock.step(1)
      c.registers(1).peekInt() should be(0xffff1234)
      c.clock.step(1)
      c.registers(2).peekInt() should be(0x80000000)
      // Check memory write at address 0x80000000L
      c.memWriteAddr.peekInt() should be(0x80000000L)
      c.memWriteData.peekInt() should be(0xffff1234L)
      c.clock.step(1 + memWriteLatency) // sw
      // Check memory read at address 0x80000000L
      c.memReadAddr.peekInt() should be(0x80000000L)
      c.memReadData.peekInt() should be(0x00001234L)
      c.clock.step(1 + memReadLatency) // lh
      // Check loaded data
      c.registers(3).peekInt() should be(0x00001234)
      // Check memory read at address 0x80000002L
      c.memReadAddr.peekInt() should be(0x80000000L + 0x2)
      c.memReadData.peekInt() should be(0x0000ffffL)
      c.clock.step(1 + memReadLatency) // lh
      c.registers(4).peekInt() should be(0x0000ffff)
      c.clock.step(5) // Paddding
    }
  }

  it should "validate LB instruction" in {
    val prog = """
    lui x1, 0xabcde000
    addi x1, x1, 0x123
    lui x2, 0x80000000
    sw x1, 0(x2)
    lb x3, 0(x2)
    lb x4, 1(x2)
    lb x5, 2(x2)
    lb x6, 3(x2)
    """
    defaultDut(prog) { c =>
      c.registers(1).peekInt() should be(0)
      c.clock.step(1)
      c.registers(1).peekInt() should be(0xabcde000)
      c.clock.step(1)
      c.registers(1).peekInt() should be(0xabcde123)
      c.clock.step(1)
      c.registers(2).peekInt() should be(0x80000000)
      // Check memory write at address 0x80000000L
      c.memWriteAddr.peekInt() should be(0x80000000L)
      c.memWriteData.peekInt() should be(0xabcde123L)
      c.clock.step(1 + memWriteLatency) // sw
      // Check memory read at address 0x80000000L
      c.memReadAddr.peekInt() should be(0x80000000L)
      c.memReadData.peekInt() should be(0x23L)
      c.clock.step(1 + memReadLatency) // lb (offset 0)
      c.registers(3).peekInt() should be(0x00000023)
      // Check memory read at address 0x80000001L
      c.memReadAddr.peekInt() should be(0x80000001L)
      c.memReadData.peekInt() should be(0xe1L)
      c.clock.step(1 + memReadLatency) // lb (offset 1)
      c.registers(4).peekInt() should be(0xffffffe1)
      // Check memory read at address 0x80000002L
      c.memReadAddr.peekInt() should be(0x80000002L)
      c.memReadData.peekInt() should be(0xcdL)
      c.clock.step(1 + memReadLatency) // lb (offset 2)
      c.registers(5).peekInt() should be(0xffffffcd)
      // Check memory read at address 0x80000003L
      c.memReadAddr.peekInt() should be(0x80000003L)
      c.memReadData.peekInt() should be(0xabL)
      c.clock.step(1 + memReadLatency) // lb (offset 3)
      c.registers(6).peekInt() should be(0xffffffab)
      // Check loaded data
      c.clock.step(5) // Paddding
    }
  }

  it should "validate LBU instruction" in {
    val prog = """
    lui x1, 0xabcde000
    addi x1, x1, 0x123
    lui x2, 0x80000000
    sw x1, 0(x2)
    lbu x3, 0(x2)
    lbu x4, 1(x2)
    lbu x5, 2(x2)
    lbu x6, 3(x2)
    """
    defaultDut(prog) { c =>
      c.registers(1).peekInt() should be(0)
      c.clock.step(1)
      c.registers(1).peekInt() should be(0xabcde000)
      c.clock.step(1)
      c.registers(1).peekInt() should be(0xabcde123)
      c.clock.step(1)
      c.registers(2).peekInt() should be(0x80000000)
      // Check memory write at address 0x80000000L
      c.memWriteAddr.peekInt() should be(0x80000000L)
      c.memWriteData.peekInt() should be(0xabcde123L)
      c.clock.step(1 + memWriteLatency) // sw
      // Check memory read at address 0x80000000L
      c.memReadAddr.peekInt() should be(0x80000000L)
      c.memReadData.peekInt() should be(0x23L)
      c.clock.step(1 + memReadLatency) // lb (offset 0)
      c.registers(3).peekInt() should be(0x00000023)
      // Check memory read at address 0x80000001L
      c.memReadAddr.peekInt() should be(0x80000001L)
      c.memReadData.peekInt() should be(0xe1L)
      c.clock.step(1 + memReadLatency) // lb (offset 1)
      c.registers(4).peekInt() should be(0x000000e1)
      // Check memory read at address 0x80000002L
      c.memReadAddr.peekInt() should be(0x80000002L)
      c.memReadData.peekInt() should be(0xcdL)
      c.clock.step(1 + memReadLatency) // lb (offset 2)
      c.registers(5).peekInt() should be(0x000000cd)
      // Check memory read at address 0x80000003L
      c.memReadAddr.peekInt() should be(0x80000003L)
      c.memReadData.peekInt() should be(0xabL)
      c.clock.step(1 + memReadLatency) // lb (offset 3)
      c.registers(6).peekInt() should be(0x000000ab)
      // Check loaded data
      c.clock.step(5) // Paddding
    }
  }
}
