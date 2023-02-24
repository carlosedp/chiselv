package chiselv

import chisel3._
import chisel3.experimental.Analog
import chisel3.util.HasBlackBoxInline // For Analog type

class GPIOPort(bitWidth: Int = 32) extends Bundle {
  val dataIn         = Input(UInt(bitWidth.W))
  val valueOut       = Output(UInt(bitWidth.W))
  val directionOut   = Output(UInt(bitWidth.W))
  val writeValue     = Input(Bool())
  val writeDirection = Input(Bool())
  val stall          = Output(Bool()) // >1 => Stall, 0 => Run
}

class GPIO(bitWidth: Int = 32, numGPIO: Int = 8) extends Module {
  val io = IO(new Bundle {
    val GPIOPort     = new GPIOPort(bitWidth)
    val externalPort = Analog(numGPIO.W)
  })

  val GPIO      = RegInit(0.U(bitWidth.W))
  val direction = RegInit(0.U(bitWidth.W)) // 0 = input, 1 = output

  io.GPIOPort.stall        := false.B
  io.GPIOPort.directionOut := direction

  // Only instantiate GPIO external interface if needed
  // this avoids using Verilator backend on tests that don't need it
  if (numGPIO > 0) {
    val InOut = Module(new GPIOInOut(numGPIO))
    io.GPIOPort.valueOut := InOut.io.dataOut
    InOut.io.dataIn      := GPIO
    InOut.io.dir         := direction
    io.externalPort <> InOut.io.dataIO
  } else {
    io.GPIOPort.valueOut := 0.U
    io.externalPort      := DontCare
  }

  when(io.GPIOPort.writeValue) {
    GPIO := io.GPIOPort.dataIn
  }
  when(io.GPIOPort.writeDirection) {
    direction := io.GPIOPort.dataIn
  }
}

class GPIOInOut(bitWidth: Int = 32, numGPIO: Int = 8) extends BlackBox(Map("WIDTH" -> numGPIO)) with HasBlackBoxInline {
  val io = IO(new Bundle {
    val dataIn  = Input(UInt(bitWidth.W))
    val dataOut = Output(UInt(bitWidth.W))
    val dir     = Input(UInt(bitWidth.W))
    val dataIO  = Analog(numGPIO.W)
  })
  setInline(
    "GPIOInOut.v",
    s"""// This module is inspired by Lucas Teske's Riscow digital port
       |// https://github.com/racerxdl/riskow/blob/main/devices/digital_port.v
       |//
       |module GPIOInOut #(parameter WIDTH=1, NUMGPIO=8) (
       |  inout   [NUMGPIO-1:0] dataIO,
       |  input   [WIDTH-1:0] dataIn,
       |  output  [WIDTH-1:0] dataOut,
       |  input   [WIDTH-1:0] dir);
       |
       |  generate
       |    genvar idx;
       |    for(idx = 0; idx < NUMGPIO; idx = idx+1) begin: register
       |      `ifdef SIMULATION
       |      assign dataIO[idx] = dir[idx] ? dataIn[idx] : 1'b0;
       |      `else
       |      assign dataIO [idx]= dir[idx] ? dataIn[idx] : 1'bZ;
       |      `endif
       |     end
       |  endgenerate
       |  assign dataOut = dataIO;
       |
       |endmodule
       |""".stripMargin,
  )
}
