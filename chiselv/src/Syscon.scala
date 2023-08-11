package chiselv

import chisel3._
import chisel3.util.{is, switch}

class SysconPort(val bitWidth: Int) extends Bundle {
  val Address = Input(UInt(12.W))
  val DataOut = Output(UInt(bitWidth.W))
}

class Syscon(
    bitWidth:  Int = 32,
    clockFreq: Long,
    numGPIO0:  Int,
    bootAddr:  Long,
    romSize:   Int,
    ramSize:   Int,
  ) extends Module {
  val io = IO(new Bundle {
    val SysconPort = new SysconPort(bitWidth)
  })

  val dataOut = WireDefault(0.U(bitWidth.W))

  switch(io.SysconPort.Address) {
    is(0x0L.U)(dataOut := 0xbaad_cafeL.U)
    // Clock frequency - (0x0000_1008)
    is(0x8L.U)(dataOut := clockFreq.asUInt)
    // Has UART0 - (0x0000_1010)
    is(0x10L.U)(dataOut := 1.U)
    // Has GPIO0 - (0x0000_1018)
    is(0x18L.U)(dataOut := 1.U)
    // Has PWM0 - (0x0000_1020)
    is(0x20L.U)(dataOut := 0.U)
    // Has Timer0 - (0x0000_1024)
    is(0x24L.U)(dataOut := 1.U)
    // Num GPIOs in GPIO0 - (0x0000_1028)
    is(0x28L.U)(dataOut := numGPIO0.asUInt)
    // Boot io.address - (0x0000_102c)
    is(0x2cL.U)(dataOut := bootAddr.asUInt)
    // ROM Size - (0x0000_1030)
    is(0x30L.U)(dataOut := romSize.asUInt)
    // RAM Size - (0x0000_1034)
    is(0x34L.U)(dataOut := ramSize.asUInt)
  }

  io.SysconPort.DataOut := dataOut
}
