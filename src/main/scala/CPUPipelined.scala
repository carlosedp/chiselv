package chiselv

import chisel3._
import chisel3.experimental._

import Instruction._

class CPUPipelined(
  cpuFrequency:          Int,
  bitWidth:              Int = 32,
  instructionMemorySize: Int = 1 * 1024,
  dataMemorySize:        Int = 1 * 1024,
  numGPIO:               Int = 8
) extends Module {
  val io = IO(new Bundle {
    val GPIO0External      = Analog(numGPIO.W)       // GPIO external port
    val UART0Port          = Flipped(new UARTPort()) // UART0 data port
    val instructionMemPort = Flipped(new InstructionSyncMemPort(bitWidth, instructionMemorySize))
    val dataMemPort        = Flipped(new MemoryPortDual(bitWidth, dataMemorySize))
  })

  val stall = WireDefault(false.B)

  // Instantiate and initialize the Register Bank
  val registerBank = Module(new RegisterBank(bitWidth))
  registerBank.io.regPort.stall := stall

  // Instantiate and initialize the Program Counter
  val PC = Module(new ProgramCounter(bitWidth))
  PC.io.pcPort.writeEnable := false.B
  PC.io.pcPort.dataIn      := 0.U
  PC.io.pcPort.writeAdd    := false.B

  // Instantiate and initialize the ALU
  val ALU = Module(new ALU(bitWidth))
  ALU.io.ALUPort.inst := ERR_INST
  ALU.io.ALUPort.a    := 0.U
  ALU.io.ALUPort.b    := 0.U

  // Instantiate and initialize the Instruction Decoder
  val decoder = Module(new Decoder(bitWidth))

  // Instantiate and initialize the Memory IO Manager
  val memoryIOManager = Module(new MemoryIOManager(bitWidth, cpuFrequency, dataMemorySize))
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

  // Instantiate and connect GPIO
  val GPIO0 = Module(new GPIO(bitWidth, numGPIO))
  memoryIOManager.io.GPIO0Port <> GPIO0.io.GPIOPort
  if (numGPIO > 0) {
    GPIO0.io.externalPort <> io.GPIO0External
  }

  // Instantiate and connect the Timer
  val timer0 = Module(new Timer(bitWidth, cpuFrequency))
  memoryIOManager.io.Timer0Port <> timer0.io.timerPort

  // ////////////////////////////////////////////////////////////////////
  // CPU Pipeline Control
  // ////////////////////////////////////////////////////////////////////

  // Pipeline Registers
  val flushPipe = RegInit(false.B)

  val decodePC  = RegInit(0.U(bitWidth.W))
  val decodePC4 = RegInit(0.U(bitWidth.W))
  val decodeOp  = RegInit(0.U(bitWidth.W))

  val executePC          = RegInit(0.U(bitWidth.W))
  val executePC4         = RegInit(0.U(bitWidth.W))
  val executeRS1         = RegInit(0.S(bitWidth.W))
  val executeRS2         = RegInit(0.S(bitWidth.W))
  val executeRD          = RegInit(0.U(bitWidth.W))
  val executeIMM         = RegInit(0.S(bitWidth.W))
  val executeInstruction = RegInit(ERR_INST)
  val executeToALU       = RegInit(false.B)
  val executeBranch      = RegInit(false.B)
  val executeJump        = RegInit(false.B)
  val executeUseIMM      = RegInit(false.B)

  val memoryPC        = RegInit(0.U(bitWidth.W))
  val memoryPC4       = RegInit(0.U(bitWidth.W))
  val memoryRD        = RegInit(0.U(bitWidth.W))
  val memoryALUResult = RegInit(0.S(bitWidth.W))
  val memoryWriteReg  = RegInit(false.B)

  val writeBackPC        = RegInit(0.U(bitWidth.W))
  val writeBackPC4       = RegInit(0.U(bitWidth.W))
  val writeBackRD        = RegInit(0.U(bitWidth.W))
  val writeBackALUResult = RegInit(0.S(bitWidth.W))
  val writeBackWriteReg  = RegInit(false.B)

  // State of the CPU Stall
  stall := memoryIOManager.io.stall

  // --------------------------- Instruction Fetch ------------------- //

  when(!stall) {
    // If CPU is stalled, do not advance PC
    PC.io.pcPort.writeEnable := true.B
    PC.io.pcPort.dataIn      := PC.io.pcPort.PC4
  }

  // Connect PC output to instruction memory
  io.instructionMemPort.readAddr := PC.io.pcPort.PC
  decodePC                       := PC.io.pcPort.PC
  decodeOp                       := io.instructionMemPort.readData
  decodePC4                      := PC.io.pcPort.PC4

  // --------------------------- Instruction Decode ------------------ //

  // Connect the instruction memory to the decoder
  decoder.io.DecoderPort.op        := decodeOp
  registerBank.io.regPort.rs1_addr := decoder.io.DecoderPort.rs1
  registerBank.io.regPort.rs2_addr := decoder.io.DecoderPort.rs2
  executeInstruction               := decoder.io.DecoderPort.inst
  executeToALU                     := decoder.io.DecoderPort.toALU
  executeBranch                    := decoder.io.DecoderPort.branch
  executeJump                      := decoder.io.DecoderPort.jump
  executeUseIMM                    := decoder.io.DecoderPort.use_imm

  executeRD  := decoder.io.DecoderPort.rd
  executeIMM := decoder.io.DecoderPort.imm
  executeRS1 := registerBank.io.regPort.rs1
  executeRS2 := registerBank.io.regPort.rs2
  executePC  := decodePC
  executePC4 := decodePC4

  // Avoid the compiler from optimizing out the registers (temporary)
  dontTouch(executePC)
  dontTouch(executePC4)
  dontTouch(executeRS1)
  dontTouch(executeRS2)
  dontTouch(executeRD)
  dontTouch(executeIMM)
  dontTouch(executeToALU)
  dontTouch(executeUseIMM)

  // --------------------------- Execute ----------------------------- //

  // ALU Operations
  when(executeToALU) {
    ALU.io.ALUPort.inst := executeInstruction
    ALU.io.ALUPort.a    := executeRS1.asUInt
    ALU.io.ALUPort.b := Mux(
      executeUseIMM,
      executeIMM,
      executeRS2
    ).asUInt
    memoryPC        := executePC
    memoryPC4       := executePC4
    memoryRD        := executeRD
    memoryALUResult := ALU.io.ALUPort.x.asSInt
    memoryWriteReg  := true.B
    // Avoid the compiler from optimizing out the registers (temporary)
    dontTouch(memoryRD)
    dontTouch(memoryPC)
    dontTouch(memoryPC4)
    dontTouch(memoryALUResult)
  }
    .elsewhen(executeInstruction === LUI) {
      memoryWriteReg  := true.B
      memoryRD        := executeRD
      memoryALUResult := executeIMM
      memoryPC        := executePC
      memoryPC4       := executePC4
    }
    .otherwise(memoryWriteReg := false.B)

  // Branch Operations
  // when(executeBranch) {
  //   ALU.io.ALUPort.a := executeRS1.asUInt
  //   ALU.io.ALUPort.b := executeRS2.asUInt
  //   switch(executeInstruction) {
  //     is(BEQ)(ALU.io.ALUPort.inst  := EQ)
  //     is(BNE)(ALU.io.ALUPort.inst  := NEQ)
  //     is(BLT)(ALU.io.ALUPort.inst  := SLT)
  //     is(BGE)(ALU.io.ALUPort.inst  := GTE)
  //     is(BLTU)(ALU.io.ALUPort.inst := SLTU)
  //     is(BGEU)(ALU.io.ALUPort.inst := GTEU)
  //   }
  //   val destPC = executePC + executeIMM.asUInt
  //   when(ALU.io.ALUPort.x === 1.U) {
  //     flushPipe                := true.B
  //     PC.io.pcPort.writeEnable := true.B
  //     // PC.io.pcPort.writeAdd    := true.B
  //     PC.io.pcPort.dataIn := destPC
  //   }.otherwise(flushPipe := false.B)

  //   memoryPC4 := destPC
  // }

  // Jump Operations
  // when(executeJump) {
  //   // Write next instruction address to rd
  //   flushPipe       := true.B
  //   memoryWriteReg  := true.B
  //   memoryRD        := executeRD
  //   memoryALUResult := executePC4.asSInt

  //   PC.io.pcPort.writeEnable := true.B
  //   when(executeInstruction === JAL) {
  //     // Set PC to jump address
  //     PC.io.pcPort.writeAdd := true.B
  //     PC.io.pcPort.dataIn   := executeIMM.asUInt
  //     memoryPC4             := executeIMM.asUInt
  //   }
  //   when(executeInstruction === JALR) {
  //     // Set PC to jump address
  //     val newPC = Cat((executeRS1 + executeIMM).asUInt()(31, 1), 0.U)
  //     PC.io.pcPort.writeAdd := true.B
  //     PC.io.pcPort.dataIn   := newPC
  //     memoryPC4             := newPC
  //   }

  // }.otherwise(flushPipe := false.B)
  // // LUI

  // --------------------------- Memory Access ----------------------- //
  writeBackPC        := memoryPC
  writeBackPC4       := memoryPC4
  writeBackRD        := memoryRD
  writeBackALUResult := memoryALUResult
  writeBackWriteReg  := memoryWriteReg
  // Avoid the compiler from optimizing out the registers (temporary)
  dontTouch(writeBackRD)
  dontTouch(writeBackPC)
  dontTouch(writeBackPC4)
  dontTouch(writeBackALUResult)

  // --------------------------- WriteBack --------------------------- //

  registerBank.io.regPort.writeEnable := writeBackWriteReg
  registerBank.io.regPort.regwr_addr  := writeBackRD
  registerBank.io.regPort.regwr_data  := writeBackALUResult

}

//   // AUIPC
//   when(decoder.io.DecoderPort.inst === AUIPC) {
//     registerBank.io.regPort.writeEnable := true.B
//     ALU.io.ALUPort.inst                 := ADD
//     ALU.io.ALUPort.a                    := FetchPC
//     ALU.io.ALUPort.b :=
//       decoder.io.DecoderPort.imm.asUInt()
//     registerBank.io.regPort.regwr_data := ALU.io.ALUPort.x.asSInt
//   }

//   // Loads & Stores
//   when(decoder.io.DecoderPort.is_load || decoder.io.DecoderPort.is_store) {
//     // Use the ALU to get the resulting address
//     ALU.io.ALUPort.inst := ADD
//     ALU.io.ALUPort.a    := registerBank.io.regPort.rs1.asUInt
//     ALU.io.ALUPort.b    := decoder.io.DecoderPort.imm.asUInt()

//     memoryIOManager.io.MemoryIOPort.writeAddr := ALU.io.ALUPort.x
//     memoryIOManager.io.MemoryIOPort.readAddr  := ALU.io.ALUPort.x
//   }

//   when(decoder.io.DecoderPort.is_load) {
//     val dataSize = WireDefault(0.U(2.W)) // Data size, 1 = byte, 2 = halfword, 3 = word
//     val dataOut  = WireDefault(0.S(32.W))

//     // Load Word
//     when(decoder.io.DecoderPort.inst === LW) {
//       dataSize := 3.U
//       dataOut  := memoryIOManager.io.MemoryIOPort.readData.asSInt
//     }
//     // Load Halfword
//     when(decoder.io.DecoderPort.inst === LH) {
//       dataSize := 2.U
//       dataOut := Cat(
//         Fill(16, memoryIOManager.io.MemoryIOPort.readData(15)),
//         memoryIOManager.io.MemoryIOPort.readData(15, 0),
//       ).asSInt
//     }
//     // Load Halfword Unsigned
//     when(decoder.io.DecoderPort.inst === LHU) {
//       dataSize := 2.U
//       dataOut  := Cat(Fill(16, 0.U), memoryIOManager.io.MemoryIOPort.readData(15, 0)).asSInt
//     }
//     // Load Byte
//     when(decoder.io.DecoderPort.inst === LB) {
//       dataSize := 1.U
//       dataOut := Cat(
//         Fill(24, memoryIOManager.io.MemoryIOPort.readData(7)),
//         memoryIOManager.io.MemoryIOPort.readData(7, 0),
//       ).asSInt
//     }
//     // Load Byte Unsigned
//     when(decoder.io.DecoderPort.inst === LBU) {
//       dataSize := 1.U
//       dataOut  := Cat(Fill(24, 0.U), memoryIOManager.io.MemoryIOPort.readData(7, 0)).asSInt
//     }
//     memoryIOManager.io.MemoryIOPort.readRequest := decoder.io.DecoderPort.is_load
//     memoryIOManager.io.MemoryIOPort.dataSize    := dataSize
//     registerBank.io.regPort.writeEnable         := true.B
//     registerBank.io.regPort.regwr_data          := dataOut
//   }

//   when(decoder.io.DecoderPort.is_store) {
//     // Define if operation is a load or store
//     memoryIOManager.io.MemoryIOPort.writeRequest := decoder.io.DecoderPort.is_store

//     // Stores
//     val dataOut  = WireDefault(0.U(32.W))
//     val dataSize = WireDefault(0.U(2.W)) // Data size, 1 = byte, 2 = halfword, 3 = word

//     // Store Word
//     when(decoder.io.DecoderPort.inst === SW) {
//       dataOut  := registerBank.io.regPort.rs2.asUInt
//       dataSize := 3.U
//     }
//     // Store Halfword
//     when(decoder.io.DecoderPort.inst === SH) {
//       dataOut  := Cat(Fill(16, 0.U), registerBank.io.regPort.rs2(15, 0).asUInt)
//       dataSize := 2.U
//     }
//     // Store Byte
//     when(decoder.io.DecoderPort.inst === SB) {
//       dataOut  := Cat(Fill(24, 0.U), registerBank.io.regPort.rs2(7, 0).asUInt)
//       dataSize := 1.U
//     }
//     memoryIOManager.io.MemoryIOPort.dataSize  := dataSize
//     memoryIOManager.io.MemoryIOPort.writeData := dataOut
//   }
//   cpuState := CPUState.Fetch
// }
