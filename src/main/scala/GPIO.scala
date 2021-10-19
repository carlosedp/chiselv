import chisel3._
import chisel3.experimental._
import chisel3.util.HasBlackBoxInline // For Analog type

class GPIOPort(bitWidth: Int = 1) extends Bundle {
  val dataIn      = Input(UInt(bitWidth.W))
  val dataOut     = Output(UInt(bitWidth.W))
  val dir         = Input(UInt(bitWidth.W)) // 1 = set output, 0 = read input
  val writeEnable = Input(Bool())
}

class GPIO(bitWidth: Int = 1) extends Module {
  val io = IO(new Bundle {
    val GPIOPort     = new GPIOPort(bitWidth)
    val externalPort = Analog(bitWidth.W)
  })

  val GPIO      = Reg(UInt(1.W))
  val direction = Reg(UInt(1.W))

  when(io.GPIOPort.writeEnable) {
    GPIO := io.GPIOPort.dataIn
  }
  direction := io.GPIOPort.dir

  val InOut = Module(new GPIOInOut())
  InOut.io.dataIn     := GPIO
  InOut.io.dir        := io.GPIOPort.dir
  io.GPIOPort.dataOut := InOut.io.dataOut
  io.externalPort <> InOut.io.dataIO
}

class GPIOInOut(bitWidth: Int = 1) extends BlackBox(Map("WIDTH" -> bitWidth)) with HasBlackBoxInline {
  val io = IO(new Bundle {
    val dataIn  = Input(UInt(1.W))
    val dataOut = Output(UInt(1.W))
    val dir     = Input(UInt(1.W))
    val dataIO  = Analog(1.W)
  })
  setInline(
    "GPIOInOut.v",
    s"""module GPIOInOut #(parameter WIDTH=1) (
       |  inout   dataIO,
       |  input   dataIn,
       |  output  dataOut,
       |  input   dir);
       |
       |  generate
       |    genvar idx;
       |      `ifdef SIMULATION
       |      assign dataIO = dir ? dataIn : 1'b0;
       |      `else
       |      assign dataIO = dir? dataIn : 1'bZ;
       |      `endif
       |  endgenerate
       |  assign dataOut = dataIO;
       |
       |endmodule
       |""".stripMargin,
  )
}
