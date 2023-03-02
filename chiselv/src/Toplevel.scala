package chiselv

import chisel3._
import circt.stage.ChiselStage
import chisel3.experimental.{Analog, FlatIO}
import mainargs.{Leftover, ParserForMethods, arg, main}

// Project Top level
class Toplevel(
  board:        String,
  invReset:     Boolean = true,
  cpuFrequency: Int,
) extends Module {
  val io = FlatIO(new Bundle {
    val led0  = Output(Bool())     // LED 0 is the heartbeat
    val UART0 = new UARTSerialPort // UART 0
    val GPIO0 = Analog(8.W)        // GPIO 0
  })

  // Instantiate PLL module based on board
  val pll = Module(new PLL0(board))
  pll.io.clki := clock
  // Define if reset should be inverted based on board switch
  val customReset = if (invReset) ~reset.asBool else reset

  // Instantiate the Core connecting using the PLL clock
  withClockAndReset(pll.io.clko, customReset) {
    val bitWidth              = 32
    val instructionMemorySize = 64 * 1024
    val dataMemorySize        = 64 * 1024
    val numGPIO               = 8

    val SOC =
      Module(
        new SOC(
          cpuFrequency          = cpuFrequency,
          entryPoint            = 0x00000000,
          bitWidth              = bitWidth,
          instructionMemorySize = instructionMemorySize,
          dataMemorySize        = dataMemorySize,
          memoryFile            = "progload.mem",
          ramFile               = "progload-RAM.mem",
          numGPIO               = numGPIO,
        ),
      )

    // Connect IO
    io.led0 <> SOC.io.led0
    io.GPIO0 <> SOC.io.GPIO0External
    io.UART0 <> SOC.io.UART0SerialPort
  }
}

// The Main object extending App to generate the Verilog code.
object Toplevel {
  @main
  def run(
    // Parse command line arguments and extract required parameters
    @arg(short = 'b', doc = "FPGA Board to use") board:                 String = "bypass",
    @arg(short = 'r', doc = "FPGA Board have inverted reset") invreset: Boolean = false,
    @arg(short = 'f', doc = "CPU Frequency to run core") cpufreq:       Int = 50000000,
    @arg(short = 'c', doc = "Chisel arguments") chiselArgs:             Leftover[String],
  ) =
    // Generate Verilog
    ChiselStage.emitSystemVerilogFile(
      new Toplevel(board, invreset, cpufreq),
      chiselArgs.value.toArray,
      Array(
        "--disable-all-randomization",
        "--strip-debug-info",
        "-lower-memories",
        "--lowering-options=disallowLocalVariables,disallowPackedArrays", // Ref. https://github.com/llvm/circt/issues/4751
      ),
    )

  def main(
    args: Array[String],
  ): Unit = ParserForMethods(this).runOrExit(args.toIndexedSeq)
}
