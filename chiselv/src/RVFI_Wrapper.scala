package chiselv

import chisel3._
import chisel3.util.{is, switch}
import circt.stage.ChiselStage
import mainargs.{Leftover, ParserForMethods, arg, main}

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
  val rs1_rdata = Output(UInt(32.W))
  val rs2_addr  = Output(UInt(5.W))
  val rs2_rdata = Output(UInt(32.W))
  val rd_addr   = Output(UInt(5.W))
  val rd_wdata  = Output(UInt(32.W))
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
  rvfi.insn  := decoder.io.op

  rvfi.rs1_addr  := registerBank.io.rs1_addr
  rvfi.rs1_rdata := registerBank.io.rs1
  rvfi.rs2_addr  := registerBank.io.rs2_addr
  rvfi.rs2_rdata := registerBank.io.rs2
  rvfi.rd_addr   := registerBank.io.regwr_addr
  rvfi.rd_wdata  := registerBank.io.regwr_data

  rvfi.pc_rdata := PC.io.PC
  rvfi.pc_wdata := Mux(
    PC.io.writeAdd,
    (PC.io.PC.asSInt + PC.io.dataIn.asSInt).asUInt,
    PC.io.PC4,
  )

  rvfi.mem_addr  := Mux(decoder.io.is_load || decoder.io.is_store, ALU.io.x, 0.U)
  rvfi.mem_rdata := Mux(decoder.io.is_load, memoryIOManager.io.DataMemPort.readData, 0.U)
  rvfi.mem_rmask := Mux(decoder.io.is_load, rvfi_mem_size_mask, 0.U)

  rvfi.mem_wdata := Mux(decoder.io.is_store, memoryIOManager.io.DataMemPort.writeData, 0.U)
  rvfi.mem_wmask := Mux(decoder.io.is_store, rvfi_mem_size_mask, 0.U)

  rvfi.mode := rvfi_mode
  rvfi.trap := false.B
  when(decoder.io.inst === Instruction.ERR_INST) {
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

object RVFI {
  @main
  def run(@arg(short = 'c', doc = "Chisel arguments") chiselArgs: Leftover[String]) =
    // Generate SystemVerilog
    ChiselStage.emitSystemVerilogFile(
      new RVFI,
      chiselArgs.value.toArray,
      Array(
        // Removes debug information from the generated Verilog
        "--strip-debug-info",
        // Disables reg and memory randomization on initialization
        "--disable-all-randomization",
        // Creates memories with write masks in a single reg. Ref. https://github.com/llvm/circt/pull/4275
        "--lower-memories",
        // Avoids "unexpected TOK_AUTOMATIC" errors in Yosys. Ref. https://github.com/llvm/circt/issues/4751
        "--lowering-options=disallowLocalVariables,disallowPackedArrays",
        // Splits the generated Verilog into multiple files
        // "--split-verilog",
        // Generates the Verilog files in the specified directory
        "-o=./generated/Toplevel_RVFI.sv",
      ),
    )

  def main(args: Array[String]): Unit =
    ParserForMethods(this).runOrExit(args.toIndexedSeq)
}
