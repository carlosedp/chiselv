package chiselv

import chisel3._
import chisel3.util.{is, switch}
import circt.stage.ChiselStage

// Chisel Bundle implementation of RISC-V Formal Interface (RVFI)
class RVFIPort extends Bundle {
  val valid     = Output(Bool())
  val order     = Output(UInt(64.W))
  val insn      = Output(UInt(32.W))
  val trap      = Output(Bool())
  val halt      = Output(Bool())
  val intr      = Output(Bool())
  val ixl       = Output(UInt(2.W))
  val mode      = Output(UInt(2.W))
  val rs1_addr  = Output(UInt(5.W))
  val rs1_rdata = Output(SInt(32.W))
  val rs2_addr  = Output(UInt(5.W))
  val rs2_rdata = Output(SInt(32.W))
  val rd_addr   = Output(UInt(5.W))
  val rd_wdata  = Output(SInt(32.W))
  val pc_rdata  = Output(UInt(32.W))
  val pc_wdata  = Output(UInt(32.W))
  val mem_addr  = Output(UInt(32.W))
  val mem_rmask = Output(UInt(4.W))
  val mem_wmask = Output(UInt(4.W))
  val mem_rdata = Output(UInt(32.W))
  val mem_wdata = Output(UInt(32.W))
}

class RVFICPUWrapper(
    cpuFrequency:          Int = 50000000,
    bitWidth:              Int = 32,
    instructionMemorySize: Int = 64 * 1024,
    dataMemorySize:        Int = 64 * 1024,
  ) extends CPUSingleCycle(
      cpuFrequency          = cpuFrequency,
      entryPoint            = 0x0,
      bitWidth              = bitWidth,
      instructionMemorySize = instructionMemorySize,
      dataMemorySize        = dataMemorySize,
      numGPIO               = 0,
    ) {
  val rvfi = IO(new RVFIPort) // RVFI interface for RISCV-Formal

  // RVFI Interface
  val rvfi_valid = RegInit(false.B)
  val rvfi_order = RegInit(0.U(64.W))
  val rvfi_insn  = RegInit(0.U(32.W))

  val rvfi_halt = RegInit(false.B)

  val rvfi_intr = RegInit(false.B)
  val rvfi_mode = RegInit(3.U(2.W))

  val rvfi_mem_size_mask = WireDefault(0.U(2.W))
  switch(memoryIOManager.io.DataMemPort.dataSize) {
    is(1.U)(rvfi_mem_size_mask := 1.U)
    is(2.U)(rvfi_mem_size_mask := 3.U)
    is(3.U)(rvfi_mem_size_mask := 15.U)
  }

  // Connect RVFI interface outputs
  rvfi_valid := !reset.asBool && !stall
  when(rvfi_valid) {
    rvfi_order := rvfi_order + 1.U
  }
  rvfi.valid := rvfi_valid
  rvfi.order := rvfi_order
  rvfi.insn  := decoder.io.DecoderPort.op

  rvfi.rs1_addr  := registerBank.io.regPort.rs1_addr
  rvfi.rs1_rdata := registerBank.io.regPort.rs1
  rvfi.rs2_addr  := registerBank.io.regPort.rs2_addr
  rvfi.rs2_rdata := registerBank.io.regPort.rs2
  rvfi.rd_addr   := registerBank.io.regPort.regwr_addr
  rvfi.rd_wdata  := registerBank.io.regPort.regwr_data

  rvfi.pc_rdata := PC.io.pcPort.PC
  rvfi.pc_wdata := Mux(
    PC.io.pcPort.writeAdd,
    (PC.io.pcPort.PC.asSInt + PC.io.pcPort.dataIn.asSInt).asUInt,
    PC.io.pcPort.PC4,
  )

  rvfi.mem_addr  := Mux(decoder.io.DecoderPort.is_load || decoder.io.DecoderPort.is_store, ALU.io.ALUPort.x, 0.U)
  rvfi.mem_rdata := Mux(decoder.io.DecoderPort.is_load, memoryIOManager.io.DataMemPort.readData, 0.U)
  rvfi.mem_rmask := Mux(decoder.io.DecoderPort.is_load, rvfi_mem_size_mask, 0.U)

  rvfi.mem_wdata := Mux(decoder.io.DecoderPort.is_store, memoryIOManager.io.DataMemPort.writeData, 0.U)
  rvfi.mem_wmask := Mux(decoder.io.DecoderPort.is_store, rvfi_mem_size_mask, 0.U)

  rvfi.mode := rvfi_mode
  rvfi.trap := false.B
  when(decoder.io.DecoderPort.inst === Instruction.ERR_INST) {
    rvfi.trap := true.B
  }

  rvfi.halt := rvfi_halt
  rvfi.intr := rvfi_intr
  rvfi.ixl  := 1.U
}

// This is the Topmodule used in RISCV-Formal
class RVFI(
    bitWidth: Int = 32
  ) extends Module {
  val io = IO(new Bundle {
    val imem_addr  = Output(UInt(bitWidth.W))
    val imem_ready = Input(Bool())
    val imem_rdata = Input(UInt(bitWidth.W))

    val dmem_rdata = Input(UInt(bitWidth.W))
    val dmem_raddr = Output(UInt(bitWidth.W))
    val dmem_waddr = Output(UInt(bitWidth.W))
    val dmem_wdata = Output(UInt(bitWidth.W))
    val dmem_wen   = Output(Bool())
  })
  val rvfi = IO(new RVFIPort) // RVFI interface for RISCV-Formal

  // Instantiate the wrapped CPU with RVFI interface
  val CPU = Module(
    new RVFICPUWrapper(bitWidth)
  )

  // Initialize unused IO
  CPU.io.UART0Port.rxQueue.bits  := 0.U
  CPU.io.UART0Port.rxEmpty       := true.B
  CPU.io.UART0Port.txQueue.ready := true.B
  CPU.io.UART0Port.txFull        := false.B
  CPU.io.UART0Port.txEmpty       := true.B
  CPU.io.UART0Port.rxFull        := false.B
  CPU.io.UART0Port.rxQueue.valid := false.B
  CPU.io.SysconPort.DataOut      := 0.U

  // Connect RVFI port
  rvfi <> CPU.rvfi

  // Connect instruction memory
  io.imem_addr                       := CPU.io.instructionMemPort.readAddr
  CPU.io.instructionMemPort.readData := io.imem_rdata
  CPU.io.instructionMemPort.ready    := io.imem_ready

  // Connect data memory
  io.dmem_waddr := CPU.io.dataMemPort.writeAddress
  io.dmem_wdata := CPU.io.dataMemPort.writeData
  io.dmem_wen   := CPU.io.dataMemPort.writeEnable

  io.dmem_raddr               := CPU.io.dataMemPort.readAddress
  CPU.io.dataMemPort.readData := io.dmem_rdata
}

object RVFI extends App {
  // Generate Verilog

  ChiselStage.emitSystemVerilogFile(
    new RVFI,
    args,
    Array("--disable-all-randomization", "--strip-debug-info", "-lower-memories"),
  )
}
