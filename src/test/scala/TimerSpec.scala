package chiselv

import chisel3._
import chiseltest._
import chiseltest.experimental._
import org.scalatest._

import flatspec._
import matchers._

class TimerWrapper(bitWidth: Int = 32, cpuFrequency: Int) extends chiselv.Timer(bitWidth, cpuFrequency) {
  val obs_counter = expose(counter)
}
class TimerSpec extends AnyFlatSpec with ChiselScalatestTester with should.Matchers {
  behavior of "Timer"

  val cpuFrequency = 25000000
  def defaultDut() =
    test(new TimerWrapper(32, cpuFrequency)).withAnnotations(
      Seq(
        // WriteVcdAnnotation,
        // VerilatorBackendAnnotation,
      )
    )

  val ms = cpuFrequency / 1000
  it should "read timer after 1ms" in {
    defaultDut() { c =>
      c.clock.setTimeout(0)
      c.io.timerPort.dataOut.peek().litValue should be(0)
      c.obs_counter.peek().litValue should be(0)
      c.clock.step(ms)
      c.io.timerPort.dataOut.peek().litValue should be(1)
      c.obs_counter.peek().litValue should be(1)
    }
  }
  it should "reset timer after 3ms and continue counting" in {
    defaultDut() { c =>
      c.clock.setTimeout(0)
      c.clock.step(3 * ms)
      c.obs_counter.peek().litValue should be(3)
      c.io.timerPort.dataOut.peek().litValue should be(3)
      c.io.timerPort.writeEnable.poke(true.B)
      c.io.timerPort.dataIn.poke(0.U)
      c.clock.step()
      c.io.timerPort.writeEnable.poke(false.B)
      c.obs_counter.peek().litValue should be(0)
      c.obs_counter.peek().litValue should be(0)
      c.clock.step(ms)
      c.io.timerPort.dataOut.peek().litValue should be(1)
      c.obs_counter.peek().litValue should be(1)
    }
  }
}
