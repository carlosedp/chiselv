package chiselv

import chisel3._
import chisel3.experimental.Analog
import chisel3.util.{Cat, Fill, is, switch}
import chiselv.Instruction._

class CPUSingleCycle(
    cpuFrequency:          Int,
    entryPoint:            Long,
    bitWidth:              Int = 32,
    instructionMemorySize: Int = 1 * 1024,
    dataMemorySize:        Int = 1 * 1024,
    numGPIO:               Int = 8,
  ) extends Module {
  val io = IO(new Bundle {
    val GPIO0External = Analog(numGPIO.W) // GPIO external port

    val UART0Port          = Flipped(new UARTPort) // UART0 data port
    val SysconPort         = Flipped(new SysconPort(bitWidth))
    val instructionMemPort = Flipped(new InstructionMemPort(bitWidth, instructionMemorySize))
    val dataMemPort        = Flipped(new MemoryPortDual(bitWidth, dataMemorySize))
  })

  val stall = WireDefault(false.B)

  // Instantiate and initialize the Register Bank
  val registerBank = Module(new RegisterBank(bitWidth))
  registerBank.io.writeEnable := false.B
  registerBank.io.regwr_data  := 0.U
  registerBank.io.stall       := stall

  // Instantiate and initialize the Program Counter
  val PC = Module(new ProgramCounter(bitWidth, entryPoint))
  PC.io.writeEnable := false.B
  PC.io.dataIn      := 0.U
  PC.io.writeAdd    := false.B

  // Instantiate and initialize the ALU
  val ALU = Module(new ALU(bitWidth))
  ALU.io.inst := ERR_INST
  ALU.io.a    := 0.U
  ALU.io.b    := 0.U

  // Instantiate and initialize the Instruction Decoder
  val decoder = Module(new Decoder(bitWidth))
  decoder.io.op := 0.U

  // Instantiate and initialize the Memory IO Manager
  val memoryIOManager = Module(new MemoryIOManager(bitWidth, dataMemorySize))
  memoryIOManager.io.MemoryIOPort.readRequest  := false.B
  memoryIOManager.io.MemoryIOPort.writeRequest := false.B
  memoryIOManager.io.MemoryIOPort.readAddr     := 0.U
  memoryIOManager.io.MemoryIOPort.writeAddr    := 0.U
  memoryIOManager.io.MemoryIOPort.writeData    := 0.U
  memoryIOManager.io.MemoryIOPort.dataSize     := 0.U
  memoryIOManager.io.MemoryIOPort.writeMask    := 0.U
  // Connect MMIO to the devices
  memoryIOManager.io.DataMemPort <> io.dataMemPort
  memoryIOManager.io.UART0Port <> io.UART0Port
  memoryIOManager.io.SysconPort <> io.SysconPort

  // Instantiate and connect GPIO
  val GPIO0 = Module(new GPIO(bitWidth, numGPIO))
  memoryIOManager.io.GPIO0Port <> GPIO0.io.GPIOPort
  if (numGPIO > 0) {
    GPIO0.io.externalPort <> io.GPIO0External
  }

  // Instantiate and connect the Timer
  val timer0 = Module(new Timer(bitWidth, cpuFrequency))
  memoryIOManager.io.Timer0Port <> timer0.io

  // --------------- CPU Control --------------- //
  // State of the CPU Stall
  stall := memoryIOManager.io.stall
  when(!stall) {
    // If CPU is stalled, do not advance PC
    PC.io.writeEnable := true.B
    PC.io.dataIn      := PC.io.PC4
  }

  // Connect PC output to instruction memory
  when(io.instructionMemPort.ready) {
    io.instructionMemPort.readAddr := PC.io.PC
  }.otherwise(
    io.instructionMemPort.readAddr := DontCare
  )

  // Connect the instruction memory to the decoder
  decoder.io.op := io.instructionMemPort.readData

  // Connect the decoder output to register bank inputs
  registerBank.io.regwr_addr := decoder.io.rd
  registerBank.io.rs1_addr   := decoder.io.rs1
  registerBank.io.rs2_addr   := decoder.io.rs2

  // ----- CPU Operations ----- //

  // ALU Operations
  when(decoder.io.toALU) {
    ALU.io.inst := decoder.io.inst
    ALU.io.a    := registerBank.io.rs1
    ALU.io.b := Mux(
      decoder.io.use_imm,
      decoder.io.imm.asUInt,
      registerBank.io.rs2,
    )

    registerBank.io.writeEnable := true.B
    registerBank.io.regwr_data  := ALU.io.x
  }

  // Branch Operations
  when(decoder.io.branch) {
    ALU.io.a := registerBank.io.rs1
    ALU.io.b := registerBank.io.rs2
    switch(decoder.io.inst) {
      is(BEQ)(ALU.io.inst  := EQ)
      is(BNE)(ALU.io.inst  := NEQ)
      is(BLT)(ALU.io.inst  := SLT)
      is(BGE)(ALU.io.inst  := GTE)
      is(BLTU)(ALU.io.inst := SLTU)
      is(BGEU)(ALU.io.inst := GTEU)
    }
    when(ALU.io.x === 1.U) {
      PC.io.writeEnable := true.B
      PC.io.writeAdd    := true.B
      PC.io.dataIn      := decoder.io.imm.asUInt
    }
  }

  // Jump Operations
  when(decoder.io.jump) {
    // Write next instruction address to rd
    registerBank.io.writeEnable := true.B
    // Use the ALU to get the result
    ALU.io.inst                := ADD
    ALU.io.a                   := PC.io.PC
    ALU.io.b                   := 4.U
    registerBank.io.regwr_data := ALU.io.x

    PC.io.writeEnable := true.B
    when(decoder.io.inst === JAL) {
      // Set PC to jump address
      PC.io.writeAdd := true.B
      PC.io.dataIn   := decoder.io.imm.asUInt
    }
    when(decoder.io.inst === JALR) {
      // Set PC to jump address
      PC.io.dataIn := Cat(
        (registerBank.io.rs1 + decoder.io.imm.asUInt)(31, 1),
        0.U,
      )
    }
  }

  // LUI
  when(decoder.io.inst === LUI) {
    registerBank.io.writeEnable := true.B
    registerBank.io.regwr_data  := decoder.io.imm.asUInt
  }

  // AUIPC
  when(decoder.io.inst === AUIPC) {
    registerBank.io.writeEnable := true.B
    ALU.io.inst                 := ADD
    ALU.io.a                    := PC.io.PC
    ALU.io.b :=
      decoder.io.imm.asUInt
    registerBank.io.regwr_data := ALU.io.x
  }

  // Loads & Stores
  when(decoder.io.is_load || decoder.io.is_store) {
    // Use the ALU to get the resulting address
    ALU.io.inst := ADD
    ALU.io.a    := registerBank.io.rs1
    ALU.io.b    := decoder.io.imm.asUInt

    memoryIOManager.io.MemoryIOPort.writeAddr := ALU.io.x
    memoryIOManager.io.MemoryIOPort.readAddr  := ALU.io.x
  }

  when(decoder.io.is_load) {
    val dataSize = WireDefault(0.U(2.W)) // Data size, 1 = byte, 2 = halfword, 3 = word
    val dataOut  = WireDefault(0.U(32.W))

    // Load Word
    when(decoder.io.inst === LW) {
      dataSize := 3.U
      dataOut  := memoryIOManager.io.MemoryIOPort.readData
    }
    // Load Halfword
    when(decoder.io.inst === LH) {
      dataSize := 2.U
      dataOut := Cat(
        Fill(16, memoryIOManager.io.MemoryIOPort.readData(15)),
        memoryIOManager.io.MemoryIOPort.readData(15, 0),
      )
    }
    // Load Halfword Unsigned
    when(decoder.io.inst === LHU) {
      dataSize := 2.U
      dataOut  := Cat(Fill(16, 0.U), memoryIOManager.io.MemoryIOPort.readData(15, 0))
    }
    // Load Byte
    when(decoder.io.inst === LB) {
      dataSize := 1.U
      dataOut := Cat(
        Fill(24, memoryIOManager.io.MemoryIOPort.readData(7)),
        memoryIOManager.io.MemoryIOPort.readData(7, 0),
      )
    }
    // Load Byte Unsigned
    when(decoder.io.inst === LBU) {
      dataSize := 1.U
      dataOut  := Cat(Fill(24, 0.U), memoryIOManager.io.MemoryIOPort.readData(7, 0))
    }
    memoryIOManager.io.MemoryIOPort.readRequest := decoder.io.is_load
    memoryIOManager.io.MemoryIOPort.dataSize    := dataSize
    registerBank.io.writeEnable                 := true.B
    registerBank.io.regwr_data                  := dataOut
  }

  when(decoder.io.is_store) {
    // Define if operation is a load or store
    memoryIOManager.io.MemoryIOPort.writeRequest := decoder.io.is_store

    // Stores
    val dataOut  = WireDefault(0.U(32.W))
    val dataSize = WireDefault(0.U(2.W)) // Data size, 1 = byte, 2 = halfword, 3 = word

    // Store Word
    when(decoder.io.inst === SW) {
      dataOut  := registerBank.io.rs2
      dataSize := 3.U
    }
    // Store Halfword
    when(decoder.io.inst === SH) {
      dataOut  := Cat(Fill(16, 0.U), registerBank.io.rs2(15, 0))
      dataSize := 2.U
    }
    // Store Byte
    when(decoder.io.inst === SB) {
      dataOut  := Cat(Fill(24, 0.U), registerBank.io.rs2(7, 0))
      dataSize := 1.U
    }
    memoryIOManager.io.MemoryIOPort.dataSize  := dataSize
    memoryIOManager.io.MemoryIOPort.writeData := dataOut
  }
}
