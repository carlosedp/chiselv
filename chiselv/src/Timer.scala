package chiselv

import chisel3._
import chisel3.util.Counter

class TimerPort(bitWidth: Int = 32) extends Bundle {
  val dataIn      = Input(UInt(bitWidth.W))
  val dataOut     = Output(UInt(bitWidth.W))
  val writeEnable = Input(Bool())
  val stall       = Output(Bool()) // >1 => Stall, 0 => Run
}

class Timer(bitWidth: Int = 32, cpuFrequency: Int) extends Module {
  val io = IO(new TimerPort(bitWidth))

  val counter = RegInit(0.U(bitWidth.W))

  when(io.writeEnable) {
    counter := io.dataIn
  }.otherwise {
    // Count up in milliseconds
    val (_, counterWrap) = Counter(true.B, cpuFrequency / 1000)
    when(counterWrap) {
      counter := counter + 1.U
    }
  }
  io.dataOut := counter
  io.stall   := false.B
}
