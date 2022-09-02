package chiselv

import chisel3._
import chisel3.util.Counter

/** The blinking LED module.
  *
  * @constructor
  *   creates a new blinking LED module that flashes every second.
  * @param freq
  *   the frequency of the core in Hertz.
  * @param startOn
  *   if true, the LED is initially on.
  * @param io_led0
  *   (Output) is the led output
  */
class Blinky(freq: Int, startOn: Boolean = false) extends Module {
  val io = IO(new Bundle {
    val led0 = Output(Bool())
  })

  // Blink leds every second (start on)
  val led              = RegInit(startOn.B)
  val (_, counterWrap) = Counter(true.B, freq / 2)
  when(counterWrap) {
    led := ~led
  }

  io.led0 := led
}
