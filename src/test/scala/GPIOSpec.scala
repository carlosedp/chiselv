package chiselv

import chisel3._
import chiseltest._
import org.scalatest._

import flatspec._
import matchers._

class GPIOWrapper(bitWidth: Int = 32, numGPIO: Int = 8) extends GPIO(bitWidth, numGPIO) with Observer {
  val obs_GPIO      = observe(GPIO)
  val obs_DIRECTION = observe(direction)
}
class GPIOSpec extends AnyFlatSpec with ChiselScalatestTester with should.Matchers {
  behavior of "GPIO"

  def defaultDut() =
    test(new GPIOWrapper(32, 8)).withAnnotations(
      Seq(
        WriteVcdAnnotation,
        VerilatorBackendAnnotation,
      )
    )

  it should "read IO when as 0 when initialized" in {
    defaultDut() { c =>
      c.io.GPIOPort.dataOut.peek().litValue() should be(0)
      c.obs_GPIO.peek().litValue() should be(0)
      c.obs_DIRECTION.peek().litValue() should be(0)
    }
  }

  it should "write direction" in {
    defaultDut() { c =>
      c.io.GPIOPort.writeDirection.poke(true.B)
      c.io.GPIOPort.dataIn.poke("b10101010".U)
      c.clock.step()
      c.obs_DIRECTION.expect("b10101010".U)
    }
  }

  it should "write IO data to output" in {
    defaultDut() { c =>
      c.io.GPIOPort.writeValue.poke(true.B)
      c.io.GPIOPort.dataIn.poke("b11111111".U)
      c.clock.step()
      c.obs_GPIO.expect("b11111111".U)
      c.io.GPIOPort.writeDirection.poke(true.B)
      c.io.GPIOPort.dataIn.poke("b01010101".U)
      c.clock.step()
      c.io.GPIOPort.dataOut.expect("b01010101".U)
      c.clock.step(5)
    }
  }

  // Doesn't work since we can't poke the analog port
  //
  // it should "read IO data from input" in {
  //   defaultDut(){ c =>
  //     c.io.GPIOPort.dataOut.peek().litValue() should be(0)
  //     c.io.GPIOPort.dir.poke(0.U) // Read input
  //     // c.InOut.io.dataIO.poke(1.U) // Write to input pin
  //     c.clock.step()
  //     c.io.GPIOPort.dataOut.peek().litValue() should be(1)
  //   }
  // }

}
