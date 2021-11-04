package chiselv

import chiseltest._
import chiseltest.experimental._
import org.scalatest._

import java.io.{File, PrintWriter}

import flatspec._
import matchers._

// Extend the Control module to add the observer for sub-module signals
class CPUSingleCycleInstWrapper(
  cpuFrequency: Int,
  bitWidth: Int,
  instructionMemorySize: Int,
  memorySize: Int,
  memoryFile: String,
) extends CPUSingleCycle(cpuFrequency, bitWidth, instructionMemorySize, memorySize, memoryFile) {
  val registers    = expose(registerBank.regs)
  val pc           = expose(PC.pc)
  val memWriteAddr = expose(memoryIOManager.io.MemoryIOPort.writeAddr)
  val memWriteData = expose(memoryIOManager.io.MemoryIOPort.writeData)
  val memReadAddr  = expose(memoryIOManager.io.MemoryIOPort.readAddr)
  val memReadData  = expose(memoryIOManager.io.MemoryIOPort.readData)
}

class CPUSingleCycleInstructionSpec extends AnyFlatSpec with ChiselScalatestTester with should.Matchers {
  behavior of "CPUSingleCycleInstruction"

  val cpuFrequency          = 25000000
  val bitWidth              = 32
  val instructionMemorySize = 1 * 1024
  val memorySize            = 1 * 1024
  val memReadLatency        = 1
  val memWriteLatency       = 1

  def defaultDut(memoryfile: String) =
    test(new CPUSingleCycleInstWrapper(cpuFrequency, bitWidth, instructionMemorySize, memorySize, memoryfile))
      .withAnnotations(
        Seq(
          WriteVcdAnnotation,
          VerilatorBackendAnnotation,
        )
      )

  it should "validate ADD/ADDI instructions" in {
    val filename = "CPUSpecMemoryTestFileADDI.hex"
    // addi x1, x1, 1 | addi x2, x2, 1 | add x3, x1, x2
    new PrintWriter(new File(filename)) { write("00108093\r\n00110113\r\n002081b3\r\n"); close }
    defaultDut(filename) { c =>
      c.clock.setTimeout(0)
      c.registers(1).peek().litValue should be(0)
      c.registers(2).peek().litValue should be(0)
      c.registers(3).peek().litValue should be(0)
      c.clock.step(1)
      c.registers(1).peek().litValue should be(1)
      c.clock.step(1)
      c.registers(2).peek().litValue should be(1)
      c.clock.step(1)
      c.registers(3).peek().litValue should be(2)
    }
    new File(filename).delete()
  }

  it should "validate Branch instructions" in {
    val filename = "CPUSpecMemoryTestFileBranch.hex"
    /*
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
     */
    new PrintWriter(new File(filename)) {
      write("""
    00400093
    00400113
    00200193
    00208463
    0000006f
    00311463
    0000006f
    0021c463
    0000006f
    00315463
    0000006f
    """.stripMargin); close
    }
    defaultDut(filename) { c =>
      c.clock.setTimeout(0)
      c.registers(1).peek().litValue should be(0x0)
      c.registers(2).peek().litValue should be(0x0)
      c.registers(3).peek().litValue should be(0x0)
      c.clock.step(1)
      c.registers(1).peek().litValue should be(4)
      c.clock.step(1)
      c.registers(2).peek().litValue should be(4)
      c.clock.step(1)
      c.registers(3).peek().litValue should be(2)
      c.pc.peek().litValue should be(0x0c)
      c.clock.step(1)
      c.pc.peek().litValue should be(0x14)
      c.clock.step(1)
      c.pc.peek().litValue should be(0x1c)
      c.clock.step(1)
      c.pc.peek().litValue should be(0x24)
      c.clock.step(1)
      c.pc.peek().litValue should be(0x2c)
    }
    new File(filename).delete()
  }

  it should "validate JAL/JALR instructions" in {
    // Create memory test file with 32bit address space
    // Instructions are: jal x1, +8 | nop | jalr x2, x1, -4
    val filename = "CPUSpecMemoryTestFileJAL.hex"
    new PrintWriter(new File(filename)) { write("008000ef\r\n00000013\r\nffc08167\r\n"); close }
    defaultDut(filename) { c =>
      c.pc.peek().litValue should be(0)
      c.registers(1).peek().litValue should be(0)
      c.registers(2).peek().litValue should be(0)
      c.clock.step(1)
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
    new File(filename).delete()
  }

  it should "validate LUI instruction" in {
    val filename = "CPUSpecMemoryTestFileLUI.hex"
    /// lui x2, 0xc0000000
    new PrintWriter(new File(filename)) { write("C0000137\r\n"); close }
    defaultDut(filename) { c =>
      c.clock.setTimeout(0)
      c.registers(2).peek().litValue should be(0x00000000)
      c.clock.step(1)
      c.registers(2).peek().litValue should be(0xc0000000)
    }
    new File(filename).delete()
  }

  it should "validate AUIPC instruction" in {
    val filename = "CPUSpecMemoryTestFileAUIPC.hex"
    // auipc x2, 4096 | auipc x3, 4096
    new PrintWriter(new File(filename)) { write("00001117\r\n00001197\r\n"); close }
    defaultDut(filename) { c =>
      c.clock.setTimeout(0)
      c.registers(2).peek().litValue should be(0x00000000)
      c.clock.step(1)
      c.registers(2).peek().litValue should be(0x00001000)
      c.clock.step(1)
      c.registers(3).peek().litValue should be(0x00001004)
    }
    new File(filename).delete()
  }

  it should "validate SW instruction" in {
    // Create memory test file with 32bit address space
    // Instructions are:
    // lui x1, %hi(0x80000000)
    // addi x1, x1, 20
    // addi x2, x0, 291
    // sw x2, 0(x1)
    // lw x3, 0(x1)
    val filename = "CPUSpecMemoryTestFileSW.hex"
    new PrintWriter(new File(filename)) { write("800000b7\r\n01408093\r\n12300113\r\n0020a023\r\n0000a183\r\n"); close }
    defaultDut(filename) { c =>
      c.registers(1).peek().litValue should be(0)
      c.clock.step(1) // lui
      c.registers(1).peek().litValue should be(0x80000000)
      c.clock.step(1) // addi
      c.registers(1).peek().litValue should be(0x80000014)
      c.clock.step(1) // addi
      c.registers(2).peek().litValue should be(0x123)
      // Check memory address 0x14 with offset
      c.memWriteAddr.peek().litValue should be(0x80000000L + 0x14)
      c.memWriteData.peek().litValue should be(0x123)
      c.clock.step(1 + memWriteLatency) // sw
      // Check memory address 0x14 with offset
      c.memReadAddr.peek().litValue should be(0x80000000L + 0x14)
      c.memReadData.peek().litValue should be(0x123)
      c.clock.step(2 + memReadLatency) // lw
      c.registers(3).peek().litValue should be(0x123)
      new File(filename).delete()
    }
  }

  it should "validate SH instruction" in {
    // Create memory test file
    // lui x1, %hi(0x80000000)
    // lui x2, %hi(0x12345000)
    // addi x2, x2, 1656
    // addi x3, x3, -1
    // sw x3, 48(x1)  // Memory offset 48 (0x30) should be 0xffffffff
    // sh x2, 52(x1)  // Memory offset 52 (0x34) should be 0x00005678
    // sh x2, 48(x1)  // Memory offset 48 (0x30) should be 0xffff5678
    // sh x2, 54(x1)  // Memory offset 52 (0x34) should be 0x56785678
    // lw x4, 48(x1)  // x4 should be 0xffff5678
    // lw x5, 52(x1)  // x5 should be 0x56785678
    val filename = "CPUSpecMemoryTestFileSH.hex"
    new PrintWriter(new File(filename)) {
      write("""
      800000b7
      12345137
      67810113
      fff18193
      0230a823
      02209a23
      02209823
      02209b23
      0300a203
      0340a283
      """.stripMargin); close
    }
    defaultDut(filename) { c =>
      c.registers(1).peek().litValue should be(0)
      c.clock.step(1) // lui
      c.registers(1).peek().litValue should be(0x80000000)
      c.clock.step(1) // lui
      c.registers(2).peek().litValue should be(0x12345000)
      c.clock.step(1) // addi
      c.registers(2).peek().litValue should be(0x12345678)
      c.clock.step(1) //addi
      c.registers(3).peek().litValue should be(0xffffffff)
      // Check memory address offset 0x30
      c.memWriteAddr.peek().litValue should be(0x80000000L + 0x30)
      c.memWriteData.peek().litValue should be(0xffffffffL)
      c.clock.step(1 + memWriteLatency) // sw
      // Check memory address offset 0x34
      c.memWriteAddr.peek().litValue should be(0x80000000L + 0x34)
      c.memWriteData.peek().litValue should be(0x5678L)
      c.clock.step(1 + memWriteLatency) //sh
      // Check memory address 0x30
      c.memWriteAddr.peek().litValue should be(0x80000000L + 0x30)
      c.memWriteData.peek().litValue should be(0x5678L)
      c.clock.step(1 + memWriteLatency) //sh
      // Check memory address offset 0x34
      c.memWriteAddr.peek().litValue should be(0x80000000L + 0x36)
      c.memWriteData.peek().litValue should be(0x5678L)
      c.clock.step(1 + memWriteLatency) //sh
      // Check memory address 0x30
      c.clock.step(1) // lw - stall
      c.memReadAddr.peek().litValue should be(0x80000000L + 0x30)
      c.memReadData.peek().litValue should be(0xffff5678L)
      c.clock.step(1) // lw
      c.clock.step(1) // lw - stall
      c.registers(4).peek().litValue should be(0xffff5678)
      // Check memory address 0x34
      c.memReadAddr.peek().litValue should be(0x80000000L + 0x34)
      c.memReadData.peek().litValue should be(0x56785678L)
      c.clock.step(1) // lw
      c.registers(5).peek().litValue should be(0x56785678)
      c.clock.step(10) // padding
      new File(filename).delete()
    }
  }

  it should "validate SB instruction" in {
    // Create memory test file
    // lui x1, %hi(0x80000000)
    // lui x2, %hi(0x12345000)
    // addi x2, x2, 1656
    // addi x3, x3, -1
    // sw x3, 48(x1)  // Memory offset 48 (0x30) should be 0xffffffff
    // sb x2, 52(x1)  // Memory offset 52 (0x34) should be 0x00000078
    // sb x2, 48(x1)  // Memory offset 48 (0x30) should be 0xffffff78
    // sb x2, 54(x1)  // Memory offset 52 (0x34) should be 0x00780078
    // sb x2, 51(x1)  // Memory offset 51 (0x33) should be 0x78ffff78
    // lw x4, 48(x1)  // x4 should be 0x78ffff78
    // lw x5, 52(x1)  // x5 should be 0x00780078
    val filename = "CPUSpecMemoryTestFileSB.hex"
    new PrintWriter(new File(filename)) {
      write("""
      800000b7
      12345137
      67810113
      fff18193
      0230a823
      02208a23
      02208823
      02208b23
      022089a3
      0300a203
      0340a283
      """.stripMargin); close
    }
    defaultDut(filename) { c =>
      c.registers(1).peek().litValue should be(0)
      c.clock.step(1) // lui
      c.registers(1).peek().litValue should be(0x80000000)
      c.clock.step(1) // lui
      c.registers(2).peek().litValue should be(0x12345000)
      c.clock.step(1) // addi
      c.registers(2).peek().litValue should be(0x12345678)
      c.clock.step(1) //addi
      c.registers(3).peek().litValue should be(0xffffffff)
      // Check memory address offset 0x30
      c.memWriteAddr.peek().litValue should be(0x80000000L + 0x30)
      c.memWriteData.peek().litValue should be(0xffffffffL)
      c.clock.step(1 + memWriteLatency) // sw
      // Check memory address offset 0x34
      c.memWriteAddr.peek().litValue should be(0x80000000L + 0x34)
      c.memWriteData.peek().litValue should be(0x78L)
      c.clock.step(1 + memWriteLatency) //sb
      // Check memory address 0x30
      c.memWriteAddr.peek().litValue should be(0x80000000L + 0x30)
      c.memWriteData.peek().litValue should be(0x78L)
      c.clock.step(1 + memWriteLatency) //sb
      // Check memory address offset 0x34
      c.memWriteAddr.peek().litValue should be(0x80000000L + 0x36)
      c.memWriteData.peek().litValue should be(0x78L)
      c.clock.step(1 + memWriteLatency) //sb
      // Check memory address offset 0x33
      c.memWriteAddr.peek().litValue should be(0x80000000L + 0x33)
      c.memWriteData.peek().litValue should be(0x78L)
      c.clock.step(1 + memWriteLatency) //sb
      // Check memory address 0x30
      c.memReadAddr.peek().litValue should be(0x80000000L + 0x30)
      c.clock.step(1) // lw
      c.memReadData.peek().litValue should be(0x78ffff78L)
      c.clock.step(1) // lw
      c.registers(4).peek().litValue should be(0x78ffff78)
      // Check memory address 0x34
      c.memReadAddr.peek().litValue should be(0x80000000L + 0x34)
      c.clock.step(1) // lw
      c.memReadData.peek().litValue should be(0x00780078L)
      c.clock.step(1) // lw
      c.registers(5).peek().litValue should be(0x00780078)
      c.clock.step(10) // padding
      new File(filename).delete()
    }
  }

  it should "validate LW instruction" in {
    // Create memory test file with 32bit address space
    // Instructions are: lui x1, 0xf0f0f000 | addi x1, x1, 240 |lui x2, 0x80000000 | sw x1, 0(x2) | lw x3, 0(x2)
    val filename = "CPUSpecMemoryTestFileLW.hex"
    new PrintWriter(new File(filename)) {
      write("""
      f0f0f0b7
      0f008093
      80000137
      00112023
      00012183
      """.stripMargin); close
    }
    defaultDut(filename) { c =>
      c.registers(1).peek().litValue should be(0)
      c.clock.step(1) // lui
      c.registers(1).peek().litValue should be(0xf0f0f000)
      c.clock.step(1) // addi
      c.registers(1).peek().litValue should be(0xf0f0f0f0)
      c.clock.step(1) // lui
      c.registers(2).peek().litValue should be(0x80000000)
      // Check memory write at address 0x80000000L
      c.memWriteAddr.peek().litValue should be(0x80000000L)
      c.memWriteData.peek().litValue should be(0xf0f0f0f0L)
      c.clock.step(1 + memWriteLatency) // sw
      // Check memory read at address 0x20 (32)
      c.memReadAddr.peek().litValue should be(0x80000000L)
      c.memReadData.peek().litValue should be(0xf0f0f0f0L)
      c.clock.step(1 + memReadLatency) // lw
      // Check loaded data
      c.registers(3).peek().litValue should be(0xf0f0f0f0)
      c.clock.step(5) // Paddding
      new File(filename).delete()
    }
  }

  it should "validate LH instruction" in {
    // Create memory test file with 32bit address space
    // Instructions are: lui x1, 0xffff1000 | addi x1, x1, 564 |lui x2, 0x80000000 | sw x1, 0(x2) | lh x3, 0(x2) | lh x4, 2(x2)
    val filename = "CPUSpecMemoryTestFileLH.hex"
    new PrintWriter(new File(filename)) {
      write("""
      ffff10b7
      23408093
      80000137
      00112023
      00011183
      00211203
      """.stripMargin); close
    }
    defaultDut(filename) { c =>
      c.registers(1).peek().litValue should be(0)
      c.clock.step(1)
      c.registers(1).peek().litValue should be(0xffff1000)
      c.clock.step(1)
      c.registers(1).peek().litValue should be(0xffff1234)
      c.clock.step(1)
      c.registers(2).peek().litValue should be(0x80000000)
      // Check memory write at address 0x80000000L
      c.memWriteAddr.peek().litValue should be(0x80000000L)
      c.memWriteData.peek().litValue should be(0xffff1234L)
      c.clock.step(1 + memWriteLatency) // sw
      // Check memory read at address 0x80000000L
      c.memReadAddr.peek().litValue should be(0x80000000L)
      c.memReadData.peek().litValue should be(0x00001234L)
      c.clock.step(1 + memReadLatency) // lh
      // Check loaded data
      c.registers(3).peek().litValue should be(0x00001234)
      // Check memory read at address 0x80000002L
      c.memReadAddr.peek().litValue should be(0x80000000L + 0x2)
      c.memReadData.peek().litValue should be(0x0000ffffL)
      c.clock.step(1 + memReadLatency) // lh
      c.registers(4).peek().litValue should be(0xffffffff)
      c.clock.step(5) // Paddding
      new File(filename).delete()
    }
  }

  it should "validate LHU instruction" in {
    // Create memory test file with 32bit address space
    // Instructions are: lui x1, 0xffff1000 | addi x1, x1, 564 |lui x2, 0x80000000 | sw x1, 0(x2) | lhu x3, 0(x2) | lhu x4, 2(x2)
    val filename = "CPUSpecMemoryTestFileLHU.hex"
    new PrintWriter(new File(filename)) {
      write("""
      ffff10b7
      23408093
      80000137
      00112023
      00015183
      00215203
      """.stripMargin); close
    }
    defaultDut(filename) { c =>
      c.registers(1).peek().litValue should be(0)
      c.clock.step(1)
      c.registers(1).peek().litValue should be(0xffff1000)
      c.clock.step(1)
      c.registers(1).peek().litValue should be(0xffff1234)
      c.clock.step(1)
      c.registers(2).peek().litValue should be(0x80000000)
      // Check memory write at address 0x80000000L
      c.memWriteAddr.peek().litValue should be(0x80000000L)
      c.memWriteData.peek().litValue should be(0xffff1234L)
      c.clock.step(1 + memWriteLatency) // sw
      // Check memory read at address 0x80000000L
      c.memReadAddr.peek().litValue should be(0x80000000L)
      c.memReadData.peek().litValue should be(0x00001234L)
      c.clock.step(1 + memReadLatency) // lh
      // Check loaded data
      c.registers(3).peek().litValue should be(0x00001234)
      // Check memory read at address 0x80000002L
      c.memReadAddr.peek().litValue should be(0x80000000L + 0x2)
      c.memReadData.peek().litValue should be(0x0000ffffL)
      c.clock.step(1 + memReadLatency) // lh
      c.registers(4).peek().litValue should be(0x0000ffff)
      c.clock.step(5) // Paddding
      new File(filename).delete()
    }
  }

  it should "validate LB instruction" in {
    // Create memory test file with 32bit address space
    // Instructions are: lui x1, 0xf0f0f000 | addi x1, x1, 240 |lui x2, 0x80000000 | sw x1, 0(x2) | lb x3, 0(x2) | lb x4, 1(x2) | lb x5, 2(x2) | lb x6, 3(x2)
    val filename = "CPUSpecMemoryTestFileLB.hex"
    new PrintWriter(new File(filename)) {
      write("""
      abcde0b7
      12308093
      80000137
      00112023
      00010183
      00110203
      00210283
      00310303
      """.stripMargin); close
    }
    defaultDut(filename) { c =>
      c.registers(1).peek().litValue should be(0)
      c.clock.step(1)
      c.registers(1).peek().litValue should be(0xabcde000)
      c.clock.step(1)
      c.registers(1).peek().litValue should be(0xabcde123)
      c.clock.step(1)
      c.registers(2).peek().litValue should be(0x80000000)
      // Check memory write at address 0x80000000L
      c.memWriteAddr.peek().litValue should be(0x80000000L)
      c.memWriteData.peek().litValue should be(0xabcde123L)
      c.clock.step(1 + memWriteLatency) // sw
      // Check memory read at address 0x80000000L
      c.memReadAddr.peek().litValue should be(0x80000000L)
      c.memReadData.peek().litValue should be(0x23L)
      c.clock.step(1 + memReadLatency) // lb (offset 0)
      c.registers(3).peek().litValue should be(0x00000023)
      // Check memory read at address 0x80000001L
      c.memReadAddr.peek().litValue should be(0x80000001L)
      c.memReadData.peek().litValue should be(0xe1L)
      c.clock.step(1 + memReadLatency) // lb (offset 1)
      c.registers(4).peek().litValue should be(0xffffffe1)
      // Check memory read at address 0x80000002L
      c.memReadAddr.peek().litValue should be(0x80000002L)
      c.memReadData.peek().litValue should be(0xcdL)
      c.clock.step(1 + memReadLatency) // lb (offset 2)
      c.registers(5).peek().litValue should be(0xffffffcd)
      // Check memory read at address 0x80000003L
      c.memReadAddr.peek().litValue should be(0x80000003L)
      c.memReadData.peek().litValue should be(0xabL)
      c.clock.step(1 + memReadLatency) // lb (offset 3)
      c.registers(6).peek().litValue should be(0xffffffab)
      // Check loaded data
      c.clock.step(5) // Paddding
      new File(filename).delete()
    }
  }

  it should "validate LBU instruction" in {
    // Create memory test file with 32bit address space
    // Instructions are: lui x1, 0xf0f0f000 | addi x1, x1, 240 |lui x2, 0x80000000 | sw x1, 0(x2) | lbu x3, 0(x2) | lbu x4, 1(x2) | lbu x5, 2(x2) | lbu x6, 3(x2)
    val filename = "CPUSpecMemoryTestFileLBU.hex"
    new PrintWriter(new File(filename)) {
      write("""
      abcde0b7
      12308093
      80000137
      00112023
      00014183
      00114203
      00214283
      00314303
      """.stripMargin); close
    }
    defaultDut(filename) { c =>
      c.registers(1).peek().litValue should be(0)
      c.clock.step(1)
      c.registers(1).peek().litValue should be(0xabcde000)
      c.clock.step(1)
      c.registers(1).peek().litValue should be(0xabcde123)
      c.clock.step(1)
      c.registers(2).peek().litValue should be(0x80000000)
      // Check memory write at address 0x80000000L
      c.memWriteAddr.peek().litValue should be(0x80000000L)
      c.memWriteData.peek().litValue should be(0xabcde123L)
      c.clock.step(1 + memWriteLatency) // sw
      // Check memory read at address 0x80000000L
      c.memReadAddr.peek().litValue should be(0x80000000L)
      c.memReadData.peek().litValue should be(0x23L)
      c.clock.step(1 + memReadLatency) // lb (offset 0)
      c.registers(3).peek().litValue should be(0x00000023)
      // Check memory read at address 0x80000001L
      c.memReadAddr.peek().litValue should be(0x80000001L)
      c.memReadData.peek().litValue should be(0xe1L)
      c.clock.step(1 + memReadLatency) // lb (offset 1)
      c.registers(4).peek().litValue should be(0x000000e1)
      // Check memory read at address 0x80000002L
      c.memReadAddr.peek().litValue should be(0x80000002L)
      c.memReadData.peek().litValue should be(0xcdL)
      c.clock.step(1 + memReadLatency) // lb (offset 2)
      c.registers(5).peek().litValue should be(0x000000cd)
      // Check memory read at address 0x80000003L
      c.memReadAddr.peek().litValue should be(0x80000003L)
      c.memReadData.peek().litValue should be(0xabL)
      c.clock.step(1 + memReadLatency) // lb (offset 3)
      c.registers(6).peek().litValue should be(0x000000ab)
      // Check loaded data
      c.clock.step(5) // Paddding
      new File(filename).delete()
    }
  }
}
