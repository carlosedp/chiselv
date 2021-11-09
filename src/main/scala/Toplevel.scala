package chiselv

import chisel3._
import chisel3.experimental._
import com.carlosedp.scalautils.ParseArguments

// Project Top level
class Toplevel(board: String, invReset: Boolean = true, cpuFrequency: Int) extends Module {
  val io = IO(new Bundle {
    val led0    = Output(Bool())    // LED 0 is the heartbeat
    val GPIO0   = Analog(8.W)       // GPIO 0
    val UART0tx = Output(UInt(1.W)) // UART0 TX
    val UART0rx = Input(UInt(1.W))  // UART0 RX
  })

  // Instantiate PLL module based on board
  val pll = Module(new PLL0(board))
  pll.io.clki := clock
  // Define if reset should be inverted based on board switch
  val customReset = Wire(Bool())
  customReset := (if (invReset) ~reset.asBool() else reset)

  // Instantiate the Core connecting using the PLL clock
  withClockAndReset(pll.io.clko, customReset) {
    val bitWidth              = 32
    val instructionMemorySize = 64 * 1024
    val dataMemorySize        = 64 * 1024
    val numGPIO               = 8

    val CPU =
      Module(
        new CPUSingleCycle(
          cpuFrequency = cpuFrequency,
          bitWidth = bitWidth,
          instructionMemorySize = instructionMemorySize,
          dataMemorySize = dataMemorySize,
          memoryFile = "progload.mem",
          ramFile = "progload-RAM.mem",
          numGPIO = numGPIO,
        )
      )
    // }
    // Connect IO
    io.led0 := CPU.io.led0
    io.GPIO0 <> CPU.io.GPIO0External
    io.UART0tx                := CPU.io.UART0SerialPort.tx
    CPU.io.UART0SerialPort.rx := io.UART0rx
  }
}

// The Main object extending App to generate the Verilog code.
object Toplevel extends App {

  // Parse command line arguments and extract required parameters
  // pass the input arguments and a list of parameters to be extracted
  // The funcion will return the parameters map and the remaining non-extracted args
  val (params, chiselargs) = ParseArguments(args, List("board", "invreset", "cpufreq"))
  val board: String =
    params.getOrElse("board", throw new IllegalArgumentException("The '-board' argument should be informed."))
  val invReset: Boolean =
    params.getOrElse("invreset", "true").toBoolean
  val cpuFrequency =
    params.getOrElse("cpufreq", throw new IllegalArgumentException("The '-cpufreq' argument should be informed.")).toInt

  // Generate Verilog
  (new chisel3.stage.ChiselStage).emitVerilog(
    new Toplevel(board, invReset, cpuFrequency),
    chiselargs,
  )
}
