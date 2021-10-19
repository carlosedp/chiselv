import chisel3._
import chiseltest._
import org.scalatest._

import flatspec._
import matchers._

class GPIOWrapper(bitWidth: Int = 1) extends GPIO(bitWidth) with Observer {
  // val observe_InOut_dataOut = observe(InOut.io.dataOut)
}
class GPIOSpec extends AnyFlatSpec with ChiselScalatestTester with should.Matchers {
  behavior of "GPIO"

  def defaultDut() =
    test(new GPIO(1)).withAnnotations(
      Seq(
        WriteVcdAnnotation,
        VerilatorBackendAnnotation,
      )
    )

  it should "read IO when as 0 when initialized" in {
    defaultDut() { c =>
      c.io.GPIOPort.dataOut.peek().litValue() should be(0)
      c.io.GPIOPort.dir.peek().litValue() should be(0)
    }
  }

  it should "write direction" in {
    defaultDut() { c =>
      c.io.GPIOPort.writeEnable.poke(true.B)
      c.io.GPIOPort.dir.poke(1.U)
      c.clock.step()
      c.io.GPIOPort.dir.peek().litValue() should be(1)
    }
  }

  it should "write IO data to output" in {
    defaultDut() { c =>
      c.io.GPIOPort.writeEnable.poke(true.B)
      c.io.GPIOPort.dataIn.poke(1.U)
      c.io.GPIOPort.dir.poke(1.U)
      c.clock.step()
      c.io.GPIOPort.dataOut.peek().litValue() should be(1)
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
