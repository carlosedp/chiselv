import chisel3._
import chiseltest._
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
) extends CPUSingleCycle(cpuFrequency, bitWidth, instructionMemorySize, memorySize, memoryFile)
  with Observer {
  val registers    = observe(registerBank.regs)
  val pc           = observe(PC.pc)
  val memWriteAddr = observe(memoryIOManager.io.MemoryIOPort.writeAddr)
  val memWriteData = observe(memoryIOManager.io.MemoryIOPort.writeData)
  val memReadAddr  = observe(memoryIOManager.io.MemoryIOPort.readAddr)
  val memReadData  = observe(memoryIOManager.io.MemoryIOPort.readData)
}

class CPUSingleCycleInstructionSpec extends AnyFlatSpec with ChiselScalatestTester with should.Matchers {
  behavior of "CPUSingleInstructionCycle"

  val cpuFrequency          = 25000000
  val bitWidth              = 32
  val instructionMemorySize = 1 * 1024
  val memorySize            = 1 * 1024

  def defaultDut(memoryfile: String) =
    test(new CPUSingleCycleInstWrapper(cpuFrequency, bitWidth, instructionMemorySize, memorySize, memoryfile))
      .withAnnotations(
        Seq(
          // WriteVcdAnnotation,
          VerilatorBackendAnnotation
        )
      )

  it should "validate ADD/ADDI instructions" in {
    val filename = "CPUSpecMemoryTestFileADDI.hex"
    // addi x1, x1, 1 | addi x2, x2, 1 | add x3, x1, x2
    new PrintWriter(new File(filename)) { write("00108093\r\n00110113\r\n002081b3\r\n"); close }
    defaultDut(filename) { c =>
      c.clock.setTimeout(0)
      c.registers(1).expect(0.S)
      c.registers(2).expect(0.S)
      c.registers(3).expect(0.S)
      c.clock.step(1)
      c.registers(1).expect(1.S)
      c.clock.step(1)
      c.registers(2).expect(1.S)
      c.clock.step(1)
      c.registers(3).expect(2.S)
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
      c.registers(1).expect(0x00000000.S)
      c.registers(2).expect(0x00000000.S)
      c.registers(3).expect(0x00000000.S)
      c.clock.step(1)
      c.registers(1).expect(4.S)
      c.clock.step(1)
      c.registers(2).expect(4.S)
      c.clock.step(1)
      c.registers(3).expect(2.S)
      c.pc.expect(0x0c.U)
      c.clock.step(1)
      c.pc.expect(0x14.U)
      c.clock.step(1)
      c.pc.expect(0x1c.U)
      c.clock.step(1)
      c.pc.expect(0x24.U)
      c.clock.step(1)
      c.pc.expect(0x2c.U)
    }
    new File(filename).delete()
  }

  it should "validate JAL/JALR instructions" in {
    // Create memory test file with 32bit address space
    // Instructions are: jal x1, +8 | nop | jalr x2, x1, -4
    val filename = "CPUSpecMemoryTestFileJAL.hex"
    new PrintWriter(new File(filename)) { write("008000ef\r\n00000013\r\nffc08167\r\n"); close }
    defaultDut(filename) { c =>
      c.pc.expect(0.U)
      c.registers(1).expect(0.S)
      c.registers(2).expect(0.S)
      c.clock.step(1)
      c.pc.expect(8.U)
      c.registers(1).expect(4.S)
      c.clock.step(1)
      c.pc.expect(0.U)
      c.registers(2).expect(0xcL.S)
      c.clock.step(1)
      c.pc.expect(8.U)
      c.registers(1).expect(4.S)
      c.clock.step(1)
      c.pc.expect(0.U)
      c.registers(2).expect(0xcL.S)
    }
    new File(filename).delete()
  }

  it should "validate LUI instruction" in {
    val filename = "CPUSpecMemoryTestFileLUI.hex"
    /// lui x2, 0xc0000000
    new PrintWriter(new File(filename)) { write("C0000137\r\n"); close }
    defaultDut(filename) { c =>
      c.clock.setTimeout(0)
      c.registers(2).expect(0x00000000L.S)
      c.clock.step(1)
      c.registers(2).expect(0xc0000000L.S)
    }
    new File(filename).delete()
  }

  it should "validate AUIPC instruction" in {
    val filename = "CPUSpecMemoryTestFileAUIPC.hex"
    // auipc x2, 4096 | auipc x3, 4096
    new PrintWriter(new File(filename)) { write("00001117\r\n00001197\r\n"); close }
    defaultDut(filename) { c =>
      c.clock.setTimeout(0)
      c.registers(2).expect(0x00000000L.S)
      c.clock.step(1)
      c.registers(2).expect(0x00001000L.S)
      c.clock.step(1)
      c.registers(3).expect(0x00001004L.S)
    }
    new File(filename).delete()
  }

  it should "validate SW instruction" in {
    // Create memory test file with 32bit address space
    // Instructions are: lui x1, 0x80f0f000 | addi x1, x1, 240 | sw x1, 20(x0)
    val filename = "CPUSpecMemoryTestFileSW.hex"
    new PrintWriter(new File(filename)) { write("80f0f0b7\r\n0f008093\r\n00102a23\r\n"); close }
    defaultDut(filename) { c =>
      c.registers(1).expect(0.S)
      c.clock.step(1)
      c.registers(1).expect(0x80f0f000L.S)
      c.clock.step(1)
      c.registers(1).expect(0x80f0f0f0L.S)
      // Check memory address 0x14 (20)
      c.memWriteAddr.expect(0x14.U)
      c.memWriteData.expect(0x80f0f0f0L.U)
      new File(filename).delete()
    }
  }

  it should "validate SH instruction" in {
    // Create memory test file with 32bit address space
    // Instructions are: lui x1, 0x80f0f000 | addi x1, x1, 240 | sh x1, 20(x0) | sh x1, 22(x0)
    val filename = "CPUSpecMemoryTestFileSW.hex"
    new PrintWriter(new File(filename)) { write("80f0f0b7\r\n0f008093\r\n00101a23\r\n00101b23\r\n"); close }
    defaultDut(filename) { c =>
      c.registers(1).expect(0.S)
      c.clock.step(1)
      c.registers(1).expect(0x80f0f000L.S)
      c.clock.step(1)
      c.registers(1).expect(0x80f0f0f0L.S)
      // Check memory address 0x14 (20)
      c.memWriteAddr.expect(0x14.U)
      c.memWriteData.expect(0xf0f0L.U)
      c.clock.step(1)
      // Check memory address 0x16 (22)
      c.memWriteAddr.expect(0x16.U)
      c.memWriteData.expect(0xf0f0L.U)
      new File(filename).delete()
    }
  }

  it should "validate SB instruction" in {
    // Create memory test file with 32bit address space
    // Instructions are: lui x1, 0x80f0f000 | addi x1, x1, 240 | sb x1, 32(x0) | sb x1, 33(x0) | sb x1, 34(x0) | sb x1, 35(x0)
    val filename = "CPUSpecMemoryTestFileSB.hex"
    new PrintWriter(new File(filename)) {
      write("""
      80f0f0b7
      0f008093
      02100023
      021000a3
      02100123
      021001a3
    """.stripMargin); close
    }
    defaultDut(filename) { c =>
      c.registers(1).expect(0.S)
      c.clock.step(1)
      c.registers(1).expect(0x80f0f000L.S)
      c.clock.step(1)
      c.registers(1).expect(0x80f0f0f0L.S)
      // Check memory address 0x20 (32)
      c.memWriteAddr.expect(0x20.U)
      c.memWriteData.expect(0xf0.U)
      c.clock.step(1)
      // Check memory address 0x21 (33)
      c.memWriteAddr.expect(0x21.U)
      c.memWriteData.expect(0xf0.U)
      c.clock.step(1)
      // Check memory address 0x22 (34)
      c.memWriteAddr.expect(0x22.U)
      c.memWriteData.expect(0xf0.U)
      c.clock.step(1)
      // Check memory address 0x23 (35)
      c.memWriteAddr.expect(0x23.U)
      c.memWriteData.expect(0xf0.U)
      c.clock.step(1)

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
      c.registers(1).expect(0.S)
      c.clock.step(1)
      c.registers(1).expect(0xf0f0f000L.S)
      c.clock.step(1)
      c.registers(1).expect(0xf0f0f0f0L.S)
      c.clock.step(1)
      c.registers(2).expect(0x80000000L.S)
      // Check memory write at address 0x80000000L
      c.memWriteAddr.expect(0x80000000L.U)
      c.memWriteData.expect(0xf0f0f0f0L.U)
      c.clock.step(1)
      // Check memory read at address 0x20 (32)
      c.memReadAddr.expect(0x80000000L.U)
      c.memReadData.expect(0xf0f0f0f0L.U)
      c.clock.step(1)
      // Check loaded data
      c.registers(3).expect(0xf0f0f0f0L.S)
      c.clock.step(5) // Paddding
      new File(filename).delete()
    }
  }

  it should "validate LH instruction" in {
    // Create memory test file with 32bit address space
    // Instructions are: lui x1, 0xf0f0f000 | addi x1, x1, 240 |lui x2, 0x80000000 | sw x1, 0(x2) | lh x3, 0(x2)
    val filename = "CPUSpecMemoryTestFileLH.hex"
    new PrintWriter(new File(filename)) {
      write("""
      f0f0f0b7
      0f008093
      80000137
      00112023
      00011183
      """.stripMargin); close
    }
    defaultDut(filename) { c =>
      c.registers(1).expect(0.S)
      c.clock.step(1)
      c.registers(1).expect(0xf0f0f000L.S)
      c.clock.step(1)
      c.registers(1).expect(0xf0f0f0f0L.S)
      c.clock.step(1)
      c.registers(2).expect(0x80000000L.S)
      // Check memory write at address 0x80000000L
      c.memWriteAddr.expect(0x80000000L.U)
      c.memWriteData.expect(0xf0f0f0f0L.U)
      c.clock.step(1)
      // Check memory read at address 0x80000000L
      c.memReadAddr.expect(0x80000000L.U)
      c.memReadData.expect(0xf0f0f0f0L.U)
      c.clock.step(1)
      // Check loaded data
      c.registers(3).expect(0xfffff0f0L.S)
      c.clock.step(5) // Paddding
      new File(filename).delete()
    }
  }

  it should "validate LB instruction" in {
    // Create memory test file with 32bit address space
    // Instructions are: lui x1, 0xf0f0f000 | addi x1, x1, 240 |lui x2, 0x80000000 | sw x1, 0(x2) | lb x3, 0(x2)
    val filename = "CPUSpecMemoryTestFileLB.hex"
    new PrintWriter(new File(filename)) {
      write("""
      f0f0f0b7
      0f008093
      80000137
      00112023
      00010183
      """.stripMargin); close
    }
    defaultDut(filename) { c =>
      c.registers(1).expect(0.S)
      c.clock.step(1)
      c.registers(1).expect(0xf0f0f000L.S)
      c.clock.step(1)
      c.registers(1).expect(0xf0f0f0f0L.S)
      c.clock.step(1)
      c.registers(2).expect(0x80000000L.S)
      // Check memory write at address 0x80000000L
      c.memWriteAddr.expect(0x80000000L.U)
      c.memWriteData.expect(0xf0f0f0f0L.U)
      c.clock.step(1)
      // Check memory read at address 0x80000000L
      c.memReadAddr.expect(0x80000000L.U)
      c.memReadData.expect(0xf0f0f0f0L.U)
      c.clock.step(1)
      // Check loaded data
      c.registers(3).expect(0xfffffff0L.S)
      c.clock.step(5) // Paddding
      new File(filename).delete()
    }
  }
}
