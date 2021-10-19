import chisel3._
import chisel3.experimental._
import com.carlosedp.scalautils.ParseArguments

// Project Top level
class Toplevel(board: String, invReset: Boolean = true) extends Module {
  val io = IO(new Bundle {
    val led0  = Output(Bool()) // LED 0 is the heartbeat
    val GPIO0 = Analog(1.W)    // GPIO 0
  })

  // Instantiate PLL module based on board
  val cpuFrequency = 25000000
  val pll          = Module(new PLL0(board))
  pll.io.clki := clock
  // Define if reset should be inverted based on board switch
  val customReset = Wire(Bool())
  customReset := (if (invReset) ~reset.asBool() else reset)

  // Instantiate the core connecting using the PLL clock
  withClockAndReset(pll.io.clko, customReset) {
    val CPU = Module(new CPUSingleCycle(cpuFrequency, 32, 64 * 1024, 64 * 1024, "progload.mem"))
    io.led0 := CPU.io.led0
    io.GPIO0 <> CPU.io.GPIO0External
  }
}

// The Main object extending App to generate the Verilog code.
object Toplevel extends App {

  // Parse command line arguments and extract required parameters
  // pass the input arguments and a list of parameters to be extracted
  // The funcion will return the parameters map and the remaining non-extracted args
  val (params, chiselargs) = ParseArguments(args, List("board", "invreset"))
  val board: String =
    params.getOrElse("board", throw new IllegalArgumentException("The '-board' argument should be informed."))
  val invReset: Boolean =
    params.getOrElse("invreset", "true").toBoolean

  // Generate Verilog
  (new chisel3.stage.ChiselStage).emitVerilog(
    new Toplevel(board, invReset),
    chiselargs,
  )
}
