import chisel3._
import chiseltest._
import org.scalatest._

import java.io.{File, PrintWriter}

import flatspec._
import matchers._

// Extend the Control module to add the observer for sub-module signals
class ControlSingleWrapper(bitWidth: Int, instructionMemorySize: Int, dataMemorySize: Int, memoryFile: String)
  extends ControlSingle(bitWidth, instructionMemorySize, dataMemorySize, memoryFile)
  with Observer {
  val registers        = observe(registerBank.regs)
  val pc               = observe(PC.pc)
  val dataMemWriteAddr = observe(memoryIOManager.memory.io.dualPort.writeAddr)
  val dataMemWriteData = observe(memoryIOManager.memory.io.dualPort.writeData)
  val dataMemReadAddr  = observe(memoryIOManager.memory.io.dualPort.readAddr)
  val dataMemReadData  = observe(memoryIOManager.memory.io.dualPort.readData)
}

class ControlSingleSpec extends AnyFlatSpec with ChiselScalatestTester with should.Matchers {
  behavior of "ControlSingle"

  it should "validate ADD/ADDI instructions" in {
    val filename = "CPUSpecMemoryTestFileADDI.hex"
    // addi x1, x1, 1 | addi x2, x2, 1 | add x3, x1, x2
    new PrintWriter(new File(filename)) { write("00108093\r\n00110113\r\n002081b3\r\n"); close }
    test(new ControlSingleWrapper(32, 1 * 1024, 1 * 1024, filename)) { c =>
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
    test(new ControlSingleWrapper(32, 1 * 1024, 1 * 1024, filename)) { c =>
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
    test(new ControlSingleWrapper(32, 1 * 1024, 1 * 1024, filename)) { c =>
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
    test(new ControlSingleWrapper(32, 1 * 1024, 1 * 1024, filename)) { c =>
      c.clock.setTimeout(0)
      c.registers(2).expect(0x00000000.S)
      c.clock.step(1)
      c.registers(2).expect(0xc0000000.S)
    }
    new File(filename).delete()
  }

  it should "validate AUIPC instruction" in {
    val filename = "CPUSpecMemoryTestFileAUIPC.hex"
    /// lui x2, 0xc0000000
    new PrintWriter(new File(filename)) { write("00001117\r\n00001197\r\n"); close }
    test(new ControlSingleWrapper(32, 1 * 1024, 1 * 1024, filename)) { c =>
      c.clock.setTimeout(0)
      c.registers(2).expect(0x00000000.S)
      c.clock.step(1)
      c.registers(2).expect(0x00001000.S)
      c.clock.step(1)
      c.registers(3).expect(0x00001004.S)
    }
    new File(filename).delete()
  }

  it should "validate SW instruction" in {
    // Create memory test file with 32bit address space
    // Instructions are: lui x1, 0xf0f0f000 | addi x1, x1, 240 | sw x1, 20(x0)
    val filename = "CPUSpecMemoryTestFileSW.hex"
    new PrintWriter(new File(filename)) { write("f0f0f0b7\r\n0f008093\r\n00102a23\r\n"); close }
    test(new ControlSingleWrapper(32, 1 * 1024, 1 * 1024, filename)) { c =>
      c.registers(1).expect(0.S)
      c.clock.step(1)
      c.registers(1).expect(0xf0f0f000.S)
      c.clock.step(1)
      c.registers(1).expect(0xf0f0f0f0.S)
      c.clock.step(1)
      // Check memory address 0x14 (20)
      c.dataMemWriteAddr.expect(0x14.U)
      c.dataMemWriteData.expect(0xf0f0f0f0L.U)
      new File(filename).delete()
    }
  }

  it should "validate SH instruction" in {
    // Create memory test file with 32bit address space
    // Instructions are: lui x1, 0xf0f0f000 | addi x1, x1, 240 | sh x1, 20(x0) | sh x1, 22(x0)
    val filename = "CPUSpecMemoryTestFileSW.hex"
    new PrintWriter(new File(filename)) { write("f0f0f0b7\r\n0f008093\r\n00101a23\r\n00101b23\r\n"); close }
    test(new ControlSingleWrapper(32, 1 * 1024, 1 * 1024, filename)) { c =>
      c.registers(1).expect(0.S)
      c.clock.step(1)
      c.registers(1).expect(0xf0f0f000.S)
      c.clock.step(1)
      c.registers(1).expect(0xf0f0f0f0.S)
      // Check memory address 0x14 (20)
      c.dataMemWriteAddr.expect(0x14.U)
      c.dataMemWriteData.expect(0xf0f0L.U)
      c.clock.step(1)
      // Check memory address 0x16 (22)
      c.dataMemWriteAddr.expect(0x16.U)
      c.dataMemWriteData.expect(0xf0f0L.U)
      new File(filename).delete()
    }
  }

  it should "validate SB instruction" in {
    // Create memory test file with 32bit address space
    // Instructions are: lui x1, 0xf0f0f000 | addi x1, x1, 240 | sb x1, 32(x0) | sb x1, 33(x0) | sb x1, 34(x0) | sb x1, 35(x0)
    val filename = "CPUSpecMemoryTestFileSB.hex"
    new PrintWriter(new File(filename)) {
      write("""
      f0f0f0b7
      0f008093
      02100023
      021000a3
      02100123
      021001a3
    """.stripMargin); close
    }
    test(new ControlSingleWrapper(32, 1 * 1024, 1 * 1024, filename)) { c =>
      c.registers(1).expect(0.S)
      c.clock.step(1)
      c.registers(1).expect(0xf0f0f000.S)
      c.clock.step(1)
      c.registers(1).expect(0xf0f0f0f0.S)
      // Check memory address 0x20 (32)
      c.dataMemWriteAddr.expect(0x20.U)
      c.dataMemWriteData.expect(0xf0.U)
      c.clock.step(1)
      // Check memory address 0x21 (33)
      c.dataMemWriteAddr.expect(0x21.U)
      c.dataMemWriteData.expect(0xf0.U)
      c.clock.step(1)
      // Check memory address 0x22 (34)
      c.dataMemWriteAddr.expect(0x22.U)
      c.dataMemWriteData.expect(0xf0.U)
      c.clock.step(1)
      // Check memory address 0x23 (35)
      c.dataMemWriteAddr.expect(0x23.U)
      c.dataMemWriteData.expect(0xf0.U)
      c.clock.step(1)

      new File(filename).delete()
    }
  }

  it should "validate LW instruction" in {
    // Create memory test file with 32bit address space
    // Instructions are: lui x1, 0xf0f0f000 | addi x1, x1, 240 |sw x1, 32(x0) | lw x2, 32(x0)
    val filename = "CPUSpecMemoryTestFileLW.hex"
    new PrintWriter(new File(filename)) { write("f0f0f0b7\r\n0f008093\r\n02102023\r\n02002103\r\n"); close }
    test(new ControlSingleWrapper(32, 1 * 1024, 1 * 1024, filename)) { c =>
      c.registers(1).expect(0.S)
      c.clock.step(1)
      c.registers(1).expect(0xf0f0f000.S)
      c.clock.step(1)
      c.registers(1).expect(0xf0f0f0f0.S)
      // Check memory write at address 0x20 (32)
      c.dataMemWriteAddr.expect(0x20.U)
      c.dataMemWriteData.expect(0xf0f0f0f0L.U)
      c.clock.step(1)
      // Check memory read at address 0x20 (32)
      c.dataMemReadAddr.expect(0x20.U)
      c.dataMemReadData.expect(0xf0f0f0f0L.U)
      c.clock.step(1)
      // Check load
      c.registers(2).expect(0xf0f0f0f0.S)
      new File(filename).delete()
    }
  }

  it should "validate LH instruction" in {
    // Create memory test file with 32bit address space
    // Instructions are: lui x1, 0xf0f0f000 | addi x1, x1, 240 |sw x1, 32(x0) | lh x2, 32(x0)
    val filename = "CPUSpecMemoryTestFileLH.hex"
    new PrintWriter(new File(filename)) { write("f0f0f0b7\r\n0f008093\r\n02102023\r\n02001103\r\n"); close }
    test(new ControlSingleWrapper(32, 1 * 1024, 1 * 1024, filename)) { c =>
      c.registers(1).expect(0.S)
      c.clock.step(1)
      c.registers(1).expect(0xf0f0f000.S)
      c.clock.step(1)
      c.registers(1).expect(0xf0f0f0f0.S)
      // Check memory write at address 0x20 (32)
      c.dataMemWriteAddr.expect(0x20.U)
      c.dataMemWriteData.expect(0xf0f0f0f0L.U)
      c.clock.step(1)
      // Check memory read at address 0x20 (32)
      c.dataMemReadAddr.expect(0x20.U)
      // Data truncation is currently done on control so we can't check it here.
      // c.dataMemWriteData.expect(0xfffff0f0L.U)
      c.clock.step(1)
      // Check load
      c.registers(2).expect(0xfffff0f0.S)
      new File(filename).delete()
    }
  }

  it should "validate LB instruction" in {
    // Create memory test file with 32bit address space
    // Instructions are: lui x1, 0xf0f0f000 | addi x1, x1, 240 |sw x1, 32(x0) | lh x2, 32(x0)
    val filename = "CPUSpecMemoryTestFileLB.hex"
    new PrintWriter(new File(filename)) { write("f0f0f0b7\r\n0f008093\r\n02102023\r\n02000103\r\n"); close }
    test(new ControlSingleWrapper(32, 1 * 1024, 1 * 1024, filename)) { c =>
      c.registers(1).expect(0.S)
      c.clock.step(1)
      c.registers(1).expect(0xf0f0f000.S)
      c.clock.step(1)
      c.registers(1).expect(0xf0f0f0f0.S)
      // Check memory write at address 0x20 (32)
      c.dataMemWriteAddr.expect(0x20.U)
      c.dataMemWriteData.expect(0xf0f0f0f0L.U)
      c.clock.step(1)
      // Check memory read at address 0x20 (32)
      c.dataMemReadAddr.expect(0x20.U)
      // Data truncation is currently done on control so we can't check it here.
      // c.dataMemWriteData.expect(0xfffffff0L.U)
      c.clock.step(1)
      // Check load
      c.registers(2).expect(0xfffffff0.S)
      new File(filename).delete()
    }
  }
}
