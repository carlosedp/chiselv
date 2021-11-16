package chiselv

import chisel3._
import chisel3.experimental._

class SOC(
  cpuFrequency:          Int,
  bitWidth:              Int = 32,
  instructionMemorySize: Int = 1 * 1024,
  dataMemorySize:        Int = 1 * 1024,
  memoryFile:            String = "",
  ramFile:               String = "",
  numGPIO:               Int = 8,
) extends Module {
  val io = IO(new Bundle {
    val led0            = Output(Bool())       // LED 0 is the heartbeat
    val GPIO0External   = Analog(numGPIO.W)    // GPIO external port
    val UART0SerialPort = new UARTSerialPort() // UART0 serial port
  })

  // Heartbeat LED - Keep on if reached an error
  val blink = Module(new Blinky(cpuFrequency))
  io.led0 := blink.io.led0

  // Instantiate and initialize the Instruction memory
  val instructionMemory = Module(new InstructionMemory(bitWidth, instructionMemorySize, memoryFile))
  instructionMemory.io.memPort.readAddr := 0.U

  // Instantiate and initialize the Data memory
  val dataMemory = Module(new DualPortRAM(bitWidth, dataMemorySize, ramFile))
  dataMemory.io.dualPort.writeEnable  := false.B
  dataMemory.io.dualPort.writeData    := 0.U
  dataMemory.io.dualPort.readAddress  := 0.U
  dataMemory.io.dualPort.writeAddress := 0.U
  dataMemory.io.dualPort.dataSize     := 0.U
  dataMemory.io.dualPort.writeMask    := 0.U

  // Instantiate and connect the UART
  val fifoLength  = 128
  val rxOverclock = 16
  val UART0       = Module(new Uart(fifoLength, rxOverclock))
  UART0.io.serialPort <> io.UART0SerialPort

  // Instantiate our core
  val core = Module(
    new CPUSingleCycle(cpuFrequency, bitWidth, instructionMemorySize, dataMemorySize, numGPIO)
  )

  // Connect the core to the devices
  core.io.instructionMemPort <> instructionMemory.io.memPort
  core.io.dataMemPort <> dataMemory.io.dualPort
  core.io.UART0Port <> UART0.io.dataPort
  core.io.GPIO0External <> io.GPIO0External
}
