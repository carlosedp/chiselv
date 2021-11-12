package chiselv

import chisel3._
import chiseltest._
import org.scalatest._
import treadle.WriteVcdAnnotation

import flatspec._
import matchers._

/*
 * This file has been provided by Anton Blanchard's Chiselwatt repository at:
 * https://github.com/antonblanchard/chiselwatt
 */

class UartSpec extends AnyFlatSpec with ChiselScalatestTester with should.Matchers {
  behavior of "Uart"

  val rxOverclock = 16
  val fpgaClock   = 15000000
  val baudRate    = 115200
  val divider     = Math.round((1.0f * fpgaClock / (baudRate * rxOverclock)) - 1)

  def clockSerial(clk: Clock) = clk.step(fpgaClock / baudRate)

  private def rxOne(u: Uart, c: UInt) = {
    /* Start bit */
    u.io.serialPort.rx.poke(0.U)
    clockSerial(u.clock)

    /*
     * This doesn't work:
     * c.pad(8).asBools.foreach({cc =>
     */
    c.asBools.foreach { cc =>
      u.io.serialPort.rx.poke(cc)
      clockSerial(u.clock)
    }

    /* Have to do this instead: */
    u.io.serialPort.rx.poke(0.U)
    clockSerial(u.clock)

    /* Stop bit */
    u.io.serialPort.rx.poke(1.U)
    clockSerial(u.clock)
  }

  it should "pass a unit test" in {
    test(new Uart(64, rxOverclock)).withAnnotations(Seq(WriteVcdAnnotation)) { u =>
      u.clock.setTimeout(10000)

      u.io.dataPort.clockDivisor.valid.poke(true.B)
      u.io.dataPort.clockDivisor.bits.poke(divider.U)

      u.io.dataPort.rxQueue.ready.poke(false.B)

      val testChars = Seq("h41".U, "h6E".U, "h74".U, "h6F".U, "h6E".U)

      testChars.foreach(c => rxOne(u, c))

      u.io.dataPort.rxQueue.ready.poke(true.B)
      testChars.foreach { c =>
        u.io.dataPort.rxQueue.valid.expect(true.B)
        u.io.dataPort.rxQueue.bits.expect(c)
        u.clock.step()
      }
      u.io.dataPort.rxQueue.ready.poke(false.B)

      u.io.dataPort.rxFull.expect(false.B)
      u.io.dataPort.txFull.expect(false.B)

      rxOne(u, "h5a".U)
      u.io.dataPort.rxFull.expect(false.B)
      (0 to 64).foreach(_ => rxOne(u, "h5a".U))
      u.io.dataPort.rxFull.expect(true.B)

      u.io.dataPort.txQueue.bits.poke("h75".U)
      u.io.dataPort.txQueue.valid.poke(true.B)
      u.clock.step()
      u.io.dataPort.txFull.expect(false.B)
      (0 to 64).foreach(_ => u.clock.step())
      u.io.dataPort.txFull.expect(true.B)
    }
  }
}
