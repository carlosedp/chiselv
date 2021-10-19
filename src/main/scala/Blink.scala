import chisel3._
import chisel3.util._

/** The blinking LED module.
  *
  * @constructor
  *   creates a new blinking object.
  * @param io.led0
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
