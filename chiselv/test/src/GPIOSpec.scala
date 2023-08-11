package chiselv

import chiseltest._
import chiseltest.experimental.expose
import com.carlosedp.riscvassembler.ObjectUtils._
import org.scalatest._

import flatspec._
import matchers._

class GPIOWrapper(bitWidth: Int = 32, numGPIO: Int = 8) extends GPIO(bitWidth, numGPIO) {
  val obs_GPIO      = expose(GPIO)
  val obs_DIRECTION = expose(direction)
}
class GPIOSpec extends AnyFlatSpec with ChiselScalatestTester with should.Matchers {
  behavior of "GPIO"

  def defaultDut =
    test(new GPIOWrapper(32, 8)).withAnnotations(
      Seq(
        WriteVcdAnnotation,
        VerilatorBackendAnnotation,
      )
    )

  it should "read GPIO when as 0 when initialized" in {
    defaultDut { c =>
      c.io.GPIOPort.valueOut.peekInt() should be(0)
      c.obs_GPIO.peekInt() should be(0)
      c.obs_DIRECTION.peekInt() should be(0)
    }
  }

  it should "write direction then read" in {
    defaultDut { c =>
      c.io.GPIOPort.writeDirection.poke(true)
      c.io.GPIOPort.dataIn.poke("10101010".b)
      c.clock.step()
      c.obs_DIRECTION.peekInt() should be("10101010".b)
      c.io.GPIOPort.directionOut.peekInt() should be("10101010".b)
    }
  }

  it should "write IO data then read" in {
    defaultDut { c =>
      c.io.GPIOPort.writeValue.poke(true)
      c.io.GPIOPort.dataIn.poke("11111111".b)
      c.clock.step()
      c.obs_GPIO.peekInt() should be("11111111".b)
      c.io.GPIOPort.writeDirection.poke(true)
      c.io.GPIOPort.dataIn.poke("01010101".b)
      c.clock.step()
      c.io.GPIOPort.valueOut.peekInt() should be("01010101".b)
      c.clock.step(5)
    }
  }

  // Doesn't work since we can't poke the analog port
  //
  // it should "read IO data from input" in {
  //   defaultDut(){ c =>
  //     c.io.GPIOPort.dataOut.peekInt() should be(0)
  //     c.io.GPIOPort.dir.poke(0.U) // Read input
  //     // c.InOut.io.dataIO.poke(1.U) // Write to input pin
  //     c.clock.step()
  //     c.io.GPIOPort.dataOut.peekInt() should be(1)
  //   }
  // }

}
