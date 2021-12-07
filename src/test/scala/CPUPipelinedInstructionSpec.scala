package chiselv

import chiseltest._
import chiseltest.experimental._
import com.carlosedp.scalautils.riscvassembler._
import org.scalatest._

import flatspec._
import matchers._

// Extend the Control module to add the observer for sub-module signals
class CPUPipelinedInstructionWrapper(
  memoryFile: String
) extends SOC(
    cpuFrequency = 25000000,
    bitWidth = 32,
    instructionMemorySize = 1 * 1024,
    dataMemorySize = 1 * 1024,
    memoryFile = memoryFile,
    numGPIO = 0
  ) {
  val registers    = expose(core.registerBank.regs)
  val pc           = expose(core.PC.pc)
  val memWriteAddr = expose(core.memoryIOManager.io.MemoryIOPort.writeAddr)
  val memWriteData = expose(core.memoryIOManager.io.MemoryIOPort.writeData)
  val memReadAddr  = expose(core.memoryIOManager.io.MemoryIOPort.readAddr)
  val memReadData  = expose(core.memoryIOManager.io.MemoryIOPort.readData)
}

class CPUPipelinedInstructionSpec
  extends AnyFlatSpec
  with ChiselScalatestTester
  with BeforeAndAfterEach
  with BeforeAndAfter
  with should.Matchers {
  val memReadLatency     = 1
  val memWriteLatency    = 1
  val pipelineLengthtoWB = 5
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
    test(new CPUPipelinedInstructionWrapper(memoryfile.relativeTo(os.pwd).toString))
      .withAnnotations(
        Seq(
          WriteVcdAnnotation
        )
      )
  }

  behavior of "CPUPipelinedInstructions with nops"

  it should "validate ADD/ADDI/SUB instructions" in {
    val prog = """
      addi x1, x1, 1
      addi x2, x2, 1
      nop
      nop
      sub, x3, x1, x2
      add x4, x1, x2
      addi x5, x4, 1
      addi x6, x5, 1
    """
    defaultDut(prog) { c =>
      c.clock.setTimeout(0)
      c.registers(1).peek().litValue should be(0)
      c.registers(2).peek().litValue should be(0)
      c.registers(3).peek().litValue should be(0)
      c.clock.step(pipelineLengthtoWB)
      c.clock.step(1) // addi
      c.registers(1).peek().litValue should be(1)
      c.clock.step(1) // addi
      c.registers(2).peek().litValue should be(1)
      c.clock.step(2) // 2x nop
      c.clock.step(1) // sub
      c.registers(3).peek().litValue should be(0)
      c.clock.step(1) // add
      c.registers(4).peek().litValue should be(2)
      c.clock.step(1) // add
      c.registers(5).peek().litValue should be(1)
      c.clock.step(1) // add
      c.registers(6).peek().litValue should be(1)
    }
  }

  it should "validate AUIPC instruction" in {
    val prog = """
    auipc x2, 4096
    auipc x3, 4096
    """
    defaultDut(prog) { c =>
      c.clock.setTimeout(0)
      c.clock.step(pipelineLengthtoWB)
      c.clock.step(1) // auipc
      c.registers(2).peek().litValue should be(0x00001004)
      c.clock.step(1) // auipc
      c.registers(3).peek().litValue should be(0x00001008)
    }
  }

  it should "validate AND/OR/XOR(I) instructions" in {
    val prog = """
      andi x1, x0, 1
      nop
      nop
      ori x2, x2, 1
      nop
      nop
      or x3, x2, x1
      nop
      nop
      and x4, x2, x3
      xor x5, x2, x3
      xori x6, x2, 0
    """
    defaultDut(prog) { c =>
      c.clock.setTimeout(0)
      c.clock.step(pipelineLengthtoWB)
      c.clock.step(1) // andi
      c.registers(1).peek().litValue should be(0)
      c.clock.step(2) // 2x nop
      c.clock.step(1) // ori
      c.registers(2).peek().litValue should be(1)
      c.clock.step(2) // 2x nop
      c.clock.step(1) // or
      c.registers(3).peek().litValue should be(1)
      c.clock.step(2) // 2x nop
      c.clock.step(1) // and
      c.registers(4).peek().litValue should be(1)
      c.clock.step(1) // xor
      c.registers(5).peek().litValue should be(0)
      c.clock.step(1) // xori
      c.registers(6).peek().litValue should be(1)
    }
  }

  it should "validate SLT/SLTI instructions" in {
    val prog = """
      slti x1, x0, 1
      nop
      nop
      slt x2, x1, x0
    """
    defaultDut(prog) { c =>
      c.clock.setTimeout(0)
      c.clock.step(pipelineLengthtoWB)
      c.clock.step(1) // slti
      c.registers(1).peek().litValue should be(1)
      c.clock.step(2) // 2x nop
      c.clock.step(1) // slt
      c.registers(2).peek().litValue should be(0)
    }
  }

  it should "validate LUI instruction" in {
    val prog = """
    lui x1, 0x80000000
    lui x2, 0xc0000000
    lui x3, 0xabcd0000
    """
    defaultDut(prog) { c =>
      c.clock.setTimeout(0)
      c.registers(2).peek().litValue should be(0x00000000)
      c.clock.step(pipelineLengthtoWB)
      c.clock.step(1) // lui
      c.registers(1).peek().litValue should be(0x80000000)
      c.clock.step(1) // lui
      c.registers(2).peek().litValue should be(0xc0000000)
      c.clock.step(1) // lui
      c.registers(3).peek().litValue should be(0xabcd0000)
    }
  }

  it should "validate SLL/SRL/SRA(I) instructions" in {
    val prog = """
      addi x1, x0, 1
      nop
      nop
      slli x2, x1, 1
      nop
      nop
      sll x3, x1, x2
      nop
      nop
      srli x4, x2, 1
      srl x5, x2, x1
      sra x6, x3, x1
      srai x7, x3, 2
    """
    defaultDut(prog) { c =>
      c.clock.setTimeout(0)
      c.clock.step(pipelineLengthtoWB)
      c.clock.step(1) // addi
      c.registers(1).peek().litValue should be(1)
      c.clock.step(2) // 2x nop
      c.clock.step(1) // slli
      c.registers(2).peek().litValue should be(2)
      c.clock.step(2) // 2x nop
      c.clock.step(1) // sll
      c.registers(3).peek().litValue should be(4)
      c.clock.step(2) // 2x nop
      c.clock.step(1) // srli
      c.registers(4).peek().litValue should be(1)
      c.clock.step(1) // srl
      c.registers(5).peek().litValue should be(1)
      c.clock.step(1) // sra
      c.registers(6).peek().litValue should be(2)
      c.clock.step(1) // srai
      c.registers(7).peek().litValue should be(1)

    }
  }

  ignore should "validate Branch instructions" in {
    val prog = """
    addi x1, x0, 4
    addi x2, x0, 4
    addi x3, x0, 2
    nop
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
      c.clock.setTimeout(1000)
      c.registers(1).peek().litValue should be(0)
      c.registers(2).peek().litValue should be(0)
      c.registers(3).peek().litValue should be(0)

      c.clock.step(30)                 // test
      c.clock.step(pipelineLengthtoWB) // addi
      c.registers(1).peek().litValue should be(4)
      c.clock.step(2) // addi
      c.registers(2).peek().litValue should be(4)
      c.clock.step(1)  // addi
      c.clock.step(20) // nop
      c.registers(3).peek().litValue should be(2)
      c.pc.peek().litValue should be(0x24)
      c.clock.step(2)
      c.pc.peek().litValue should be(0x2c)
      c.clock.step(20)
      c.pc.peek().litValue should be(0x30)
      c.clock.step(2)
      c.pc.peek().litValue should be(0x3c)
      c.clock.step(2)
      c.pc.peek().litValue should be(0x44)
    }
  }

  ignore should "validate JAL/JALR instructions" in {
    val prog = """
      jal x1, +8
      nop
      jalr x2, x1, -4
      """
    defaultDut(prog) { c =>
      c.pc.peek().litValue should be(0)
      c.registers(1).peek().litValue should be(0)
      c.registers(2).peek().litValue should be(0)
      c.clock.step(pipelineLengthtoWB + 10)
      c.pc.peek().litValue should be(8)
      c.registers(1).peek().litValue should be(4)
      c.clock.step(1)
      c.pc.peek().litValue should be(0)
      c.registers(2).peek().litValue should be(0xcL)
      c.clock.step(1)
      c.pc.peek().litValue should be(8)
      c.registers(1).peek().litValue should be(4)
      c.clock.step(1)
      c.pc.peek().litValue should be(0)
      c.registers(2).peek().litValue should be(0xcL)
    }
  }

  it should "validate SW instruction" in {
    val prog = """
    lui x1, 0x80000000
    nop
    nop
    addi x1, x1, 0x14
    addi x2, x0, 0x123
    nop
    nop
    sw x2, 0(x1)
    lw x3, 0(x1)
    """
    defaultDut(prog) { c =>
      c.registers(1).peek().litValue should be(0)
      c.clock.step(pipelineLengthtoWB)
      c.clock.step(1) // lui
      c.registers(1).peek().litValue should be(0x80000000)
      c.clock.step(2) // 2x nop
      c.clock.step(1) // addi
      c.registers(1).peek().litValue should be(0x80000014)
      c.clock.step(1) // addi
      c.registers(2).peek().litValue should be(0x123)
      c.clock.step(1) // nop
      c.clock.step(1) // nop
      // Check memory address 0x14 with offset
      c.memWriteAddr.peek().litValue should be(0x80000000L + 0x14)
      c.memWriteData.peek().litValue should be(0x123)
      c.clock.step(1 + memWriteLatency) // sw
      // Check memory address 0x14 with offset
      c.memReadAddr.peek().litValue should be(0x80000000L + 0x14)
      c.memReadData.peek().litValue should be(0x123)
      c.clock.step(2 + memReadLatency) // lw
      c.registers(3).peek().litValue should be(0x123)
    }
  }

  it should "validate SH instruction" in {
    val prog = """
    lui x1, 0x80000000
    lui x2, 0x12345000
    nop
    nop
    addi x2, x2, 1656
    addi x3, x3, -1
    nop
    nop
    sw x3, 48(x1)  // Memory offset 48 (0x30) should be 0xffffffff
    sh x2, 52(x1)  // Memory offset 52 (0x34) should be 0x00005678
    nop
    nop
    sh x2, 48(x1)  // Memory offset 48 (0x30) should be 0xffff5678
    nop
    nop
    sh x2, 54(x1)  // Memory offset 52 (0x34) should be 0x56785678
    lw x4, 48(x1)  // x4 should be 0xffff5678
    lw x5, 52(x1)  // x5 should be 0x56785678
    """
    defaultDut(prog) { c =>
      c.registers(1).peek().litValue should be(0)
      c.clock.step(pipelineLengthtoWB)
      c.clock.step(1) // lui
      c.registers(1).peek().litValue should be(0x80000000)
      c.clock.step(1) // lui
      c.registers(2).peek().litValue should be(0x12345000)
      c.clock.step(2) // 2x nop
      c.clock.step(1) // addi
      c.registers(2).peek().litValue should be(0x12345678)
      c.clock.step(1) // addi
      c.registers(3).peek().litValue should be(0xffffffff)
      c.clock.step(1) // nop
      // Check memory address offset 0x30
      c.memWriteAddr.peek().litValue should be(0x80000000L + 0x30)
      c.memWriteData.peek().litValue should be(0xffffffffL)
      c.clock.step(1)                   // nop
      c.clock.step(1 + memWriteLatency) // sw
      // Check memory address offset 0x34
      c.memWriteAddr.peek().litValue should be(0x80000000L + 0x34)
      c.memWriteData.peek().litValue should be(0x5678L)
      c.clock.step(1 + memWriteLatency) // sh
      c.clock.step(1)                   // nop
      // Check memory address 0x30
      c.memWriteAddr.peek().litValue should be(0x80000000L + 0x30)
      c.memWriteData.peek().litValue should be(0x5678L)
      c.clock.step(1)                   // nop
      c.clock.step(1 + memWriteLatency) // sh
      c.clock.step(1)                   // nop
      // Check memory address offset 0x34
      c.memWriteAddr.peek().litValue should be(0x80000000L + 0x36)
      c.memWriteData.peek().litValue should be(0x5678L)
      c.clock.step(1)                   // nop
      c.clock.step(1 + memWriteLatency) // sh
      c.clock.step(1)                   // lw - stall
      // Check memory address 0x30
      c.memReadAddr.peek().litValue should be(0x80000000L + 0x30)
      c.memReadData.peek().litValue should be(0xffff5678L)
      c.clock.step(1) // lw
      c.clock.step(1) // lw - stall
      c.clock.step(1) // lw - ??
      c.registers(4).peek().litValue should be(0xffff5678)
      // Check memory address 0x34
      c.memReadAddr.peek().litValue should be(0x80000000L + 0x34)
      c.memReadData.peek().litValue should be(0x56785678L)
      c.clock.step(1) // lw
      c.clock.step(1) // lw - ˜??
      c.registers(5).peek().litValue should be(0x56785678)
      c.clock.step(10) // padding
    }
  }

  it should "validate SB instruction" in {
    val prog = """
    lui x1, 0x80000000
    lui x2, 0x12345000
    nop
    nop
    addi x2, x2, 1656
    addi x3, x3, -1
    nop
    nop
    sw x3, 48(x1)  // Memory offset 48 (0x30) should be 0xffffffff
    sb x2, 52(x1)  // Memory offset 52 (0x34) should be 0x00000078
    nop
    nop
    sb x2, 48(x1)  // Memory offset 48 (0x30) should be 0xffffff78
    nop
    nop
    sb x2, 54(x1)  // Memory offset 52 (0x34) should be 0x00780078
    nop
    nop
    sb x2, 51(x1)  // Memory offset 51 (0x33) should be 0x78ffff78
    lw x4, 48(x1)  // x4 should be 0x78ffff78
    lw x5, 52(x1)  // x5 should be 0x00780078
    """
    defaultDut(prog) { c =>
      c.registers(1).peek().litValue should be(0)
      c.clock.step(pipelineLengthtoWB)
      c.clock.step(1) // lui
      c.registers(1).peek().litValue should be(0x80000000)
      c.clock.step(1) // lui
      c.registers(2).peek().litValue should be(0x12345000)
      c.clock.step(2) // 2x nop
      c.clock.step(1) // addi
      c.registers(2).peek().litValue should be(0x12345678)
      c.clock.step(1) // addi
      c.registers(3).peek().litValue should be(0xffffffff)
      c.clock.step(2) // 2x nop
      // Check memory address offset 0x30
      c.memWriteAddr.peek().litValue should be(0x80000000L + 0x30)
      c.memWriteData.peek().litValue should be(0xffffffffL)
      c.clock.step(1 + memWriteLatency) // sw
      // Check memory address offset 0x34
      c.memWriteAddr.peek().litValue should be(0x80000000L + 0x34)
      c.memWriteData.peek().litValue should be(0x78L)
      c.clock.step(1 + memWriteLatency) // sb
      c.clock.step(1)                   // nop
      // Check memory address 0x30
      c.memWriteAddr.peek().litValue should be(0x80000000L + 0x30)
      c.memWriteData.peek().litValue should be(0x78L)
      c.clock.step(1)                   // nop
      c.clock.step(1 + memWriteLatency) // sb
      c.clock.step(1)                   // nop
      // Check memory address offset 0x34
      c.memWriteAddr.peek().litValue should be(0x80000000L + 0x36)
      c.memWriteData.peek().litValue should be(0x78L)
      c.clock.step(1)                   // nop
      c.clock.step(1 + memWriteLatency) // sb
      c.clock.step(1)                   // nop
      // Check memory address offset 0x33
      c.memWriteAddr.peek().litValue should be(0x80000000L + 0x33)
      c.memWriteData.peek().litValue should be(0x78L)
      c.clock.step(1)                   // nop
      c.clock.step(1 + memWriteLatency) // sb
      // Check memory address 0x30
      c.memReadAddr.peek().litValue should be(0x80000000L + 0x30)
      c.clock.step(1) // lw
      c.memReadData.peek().litValue should be(0x78ffff78L)
      c.clock.step(1) // lw
      c.clock.step(1) // lw
      c.registers(4).peek().litValue should be(0x78ffff78)
      // Check memory address 0x34
      c.memReadAddr.peek().litValue should be(0x80000000L + 0x34)
      c.clock.step(1) // lw
      c.memReadData.peek().litValue should be(0x00780078L)
      c.clock.step(1) // lw
      c.clock.step(1) // lw
      c.clock.step(1) // lw
      c.registers(5).peek().litValue should be(0x00780078)
      c.clock.step(10) // padding
    }
  }

  it should "validate LW instruction" in {
    val prog = """
    lui x1, 0xf0f0f000
    nop
    nop
    addi x1, x1, 240
    lui x2, 0x80000000
    nop
    nop
    sw x1, 0(x2)
    lw x3, 0(x2)
    """
    defaultDut(prog) { c =>
      c.registers(1).peek().litValue should be(0)
      c.clock.step(pipelineLengthtoWB)
      c.clock.step(1) // lui
      c.registers(1).peek().litValue should be(0xf0f0f000)
      c.clock.step(2) // 2x nop
      c.clock.step(1) // addi
      c.registers(1).peek().litValue should be(0xf0f0f0f0)
      c.clock.step(1) // lui
      c.registers(2).peek().litValue should be(0x80000000)
      c.clock.step(2) // 2x nop
      // Check memory write at address 0x80000000L
      c.memWriteAddr.peek().litValue should be(0x80000000L)
      c.memWriteData.peek().litValue should be(0xf0f0f0f0L)
      c.clock.step(1 + memWriteLatency) // sw
      // Check memory read at address 0x20 (32)
      c.memReadAddr.peek().litValue should be(0x80000000L)
      c.memReadData.peek().litValue should be(0xf0f0f0f0L)
      c.clock.step(1 + memReadLatency) // lw
      // Check loaded data
      c.clock.step(10)
      c.registers(3).peek().litValue should be(0xf0f0f0f0)
      c.clock.step(5) // Paddding
    }
  }

  it should "validate LH instruction" in {
    val prog = """
    lui x1, 0xffff1000
    nop
    nop
    addi x1, x1, 564
    lui x2, 0x80000000
    nop
    nop
    sw x1, 0(x2)
    lh x3, 0(x2)
    lh x4, 2(x2)
    """
    defaultDut(prog) { c =>
      c.registers(1).peek().litValue should be(0)
      c.clock.step(pipelineLengthtoWB)
      c.clock.step(1) // lui
      c.registers(1).peek().litValue should be(0xffff1000)
      c.clock.step(2) // 2x nop
      c.clock.step(1) // addi
      c.registers(1).peek().litValue should be(0xffff1234)
      c.clock.step(1) // lui
      c.registers(2).peek().litValue should be(0x80000000)
      // Check memory write at address 0x80000000L
      c.clock.step(1) // nop
      c.memWriteAddr.peek().litValue should be(0x80000000L)
      c.memWriteData.peek().litValue should be(0xffff1234L)
      c.clock.step(1)                   // nop
      c.clock.step(1 + memWriteLatency) // sw
      // Check memory read at address 0x80000000L
      c.memReadAddr.peek().litValue should be(0x80000000L)
      c.memReadData.peek().litValue should be(0x00001234L)
      c.clock.step(1 + memReadLatency) // lh
      c.clock.step(1)
      // Check loaded data
      c.registers(3).peek().litValue should be(0x00001234)
      // Check memory read at address 0x80000002L
      c.memReadAddr.peek().litValue should be(0x80000000L + 0x2)
      c.memReadData.peek().litValue should be(0x0000ffffL)
      c.clock.step(1 + memReadLatency) // lh
      c.registers(4).peek().litValue should be(0xffffffff)
      c.clock.step(5) // Paddding
    }
  }

  it should "validate LHU instruction" in {
    val prog = """
    lui x1, 0xffff1000
    nop
    nop
    addi x1, x1, 564
    lui x2, 0x80000000
    nop
    nop
    sw x1, 0(x2)
    lhu x3, 0(x2)
    lhu x4, 2(x2)
    """
    defaultDut(prog) { c =>
      c.registers(1).peek().litValue should be(0)
      c.clock.step(pipelineLengthtoWB)
      c.clock.step(1) // lui
      c.registers(1).peek().litValue should be(0xffff1000)
      c.clock.step(2) // 2x nop
      c.clock.step(1) // addi
      c.registers(1).peek().litValue should be(0xffff1234)
      c.clock.step(1) // lui
      c.registers(2).peek().litValue should be(0x80000000)
      c.clock.step(1) // nop
      // Check memory write at address 0x80000000L
      c.memWriteAddr.peek().litValue should be(0x80000000L)
      c.memWriteData.peek().litValue should be(0xffff1234L)
      c.clock.step(1)                   // nop
      c.clock.step(1 + memWriteLatency) // sw
      // Check memory read at address 0x80000000L
      c.memReadAddr.peek().litValue should be(0x80000000L)
      c.memReadData.peek().litValue should be(0x00001234L)
      c.clock.step(1 + memReadLatency) // lh
      c.clock.step(1)
      // Check loaded data
      c.registers(3).peek().litValue should be(0x00001234)
      // Check memory read at address 0x80000002L
      c.memReadAddr.peek().litValue should be(0x80000000L + 0x2)
      c.memReadData.peek().litValue should be(0x0000ffffL)
      c.clock.step(1 + memReadLatency) // lh
      c.clock.step(1)
      c.registers(4).peek().litValue should be(0x0000ffff)
      c.clock.step(5) // Paddding
    }
  }

  it should "validate LB instruction" in {
    val prog = """
    lui x1, 0xabcde000
    nop
    nop
    addi x1, x1, 0x123
    lui x2, 0x80000000
    nop
    nop
    sw x1, 0(x2)
    lb x3, 0(x2)
    lb x4, 1(x2)
    nop         // No idea why this is needed
    lb x5, 2(x2)
    lb x6, 3(x2)
    """
    defaultDut(prog) { c =>
      c.registers(1).peek().litValue should be(0)
      c.clock.step(pipelineLengthtoWB)
      c.clock.step(1) // lui
      c.registers(1).peek().litValue should be(0xabcde000)
      c.clock.step(2) // 2x nop
      c.clock.step(1) // addi
      c.registers(1).peek().litValue should be(0xabcde123)
      c.clock.step(1) // lui
      c.registers(2).peek().litValue should be(0x80000000)
      c.clock.step(1) // nop
      // Check memory write at address 0x80000000L
      c.memWriteAddr.peek().litValue should be(0x80000000L)
      c.memWriteData.peek().litValue should be(0xabcde123L)
      c.clock.step(1)                   // nop
      c.clock.step(1 + memWriteLatency) // sw
      // Check memory read at address 0x80000000L
      c.memReadAddr.peek().litValue should be(0x80000000L)
      c.memReadData.peek().litValue should be(0x23L)
      c.clock.step(1 + memReadLatency) // lb (offset 0)
      c.clock.step(1)
      c.registers(3).peek().litValue should be(0x00000023)
      // Check memory read at address 0x80000001L
      // c.clock.step(1)
      // c.memReadAddr.peek().litValue should be(0x80000001L)
      // c.memReadData.peek().litValue should be(0xe1L)
      c.clock.step(1 + memReadLatency) // lb (offset 1)
      c.clock.step(1)
      c.registers(4).peek().litValue should be(0xffffffe1)
      // Check memory read at address 0x80000002L
      // c.memReadAddr.peek().litValue should be(0x80000002L)
      // c.memReadData.peek().litValue should be(0xcdL)
      c.clock.step(1 + memReadLatency) // lb (offset 2)
      c.clock.step(1)
      c.registers(5).peek().litValue should be(0xffffffcd)
      // Check memory read at address 0x80000003L
      // c.memReadAddr.peek().litValue should be(0x80000003L)
      // c.memReadData.peek().litValue should be(0xabL)
      c.clock.step(1 + memReadLatency) // lb (offset 3)
      c.clock.step(1)
      c.registers(6).peek().litValue should be(0xffffffab)
      // Check loaded data
      c.clock.step(5) // Paddding
    }
  }

  it should "validate LBU instruction" in {
    val prog = """
    lui x1, 0xabcde000
    nop
    nop
    addi x1, x1, 0x123
    lui x2, 0x80000000
    nop
    nop
    sw x1, 0(x2)
    lbu x3, 0(x2)
    lbu x4, 1(x2)
    nop             // No idea why this is needed
    lbu x5, 2(x2)
    lbu x6, 3(x2)
    """
    defaultDut(prog) { c =>
      c.registers(1).peek().litValue should be(0)
      c.clock.step(pipelineLengthtoWB)
      c.clock.step(1) // lui
      c.registers(1).peek().litValue should be(0xabcde000)
      c.clock.step(2) // 2x nop
      c.clock.step(1) // addi
      c.registers(1).peek().litValue should be(0xabcde123)
      c.clock.step(1) // lui
      c.registers(2).peek().litValue should be(0x80000000)
      c.clock.step(2) // 2x nop
      // Check memory write at address 0x80000000L
      c.memWriteAddr.peek().litValue should be(0x80000000L)
      c.memWriteData.peek().litValue should be(0xabcde123L)
      c.clock.step(1 + memWriteLatency) // sw
      // Check memory read at address 0x80000000L
      c.memReadAddr.peek().litValue should be(0x80000000L)
      c.memReadData.peek().litValue should be(0x23L)
      c.clock.step(1 + memReadLatency) // lb (offset 0)
      c.clock.step(1)
      c.registers(3).peek().litValue should be(0x00000023)
      // Check memory read at address 0x80000001L
      // c.memReadAddr.peek().litValue should be(0x80000001L)
      // c.memReadData.peek().litValue should be(0xe1L)
      c.clock.step(1 + memReadLatency) // lb (offset 1)
      c.clock.step(1)
      c.registers(4).peek().litValue should be(0x000000e1)
      // Check memory read at address 0x80000002L
      // c.memReadAddr.peek().litValue should be(0x80000002L)
      // c.memReadData.peek().litValue should be(0xcdL)
      c.clock.step(1 + memReadLatency) // lb (offset 2)
      c.clock.step(10)
      c.registers(5).peek().litValue should be(0x000000cd)
      // Check memory read at address 0x80000003L
      // c.memReadAddr.peek().litValue should be(0x80000003L)
      // c.memReadData.peek().litValue should be(0xabL)
      c.clock.step(1 + memReadLatency) // lb (offset 3)
      c.clock.step(1)
      c.registers(6).peek().litValue should be(0x000000ab)
      // Check loaded data
      c.clock.step(5) // Paddding
    }
  }
}
