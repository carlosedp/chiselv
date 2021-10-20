package chiselv

import chisel3._
import chisel3.util._

class PLL0(board: String) extends BlackBox with HasBlackBoxResource {
  val io = IO(new Bundle() {
    val clki = Input(Clock())
    val clko = Output(Clock());
    val lock = Output(Clock())
  })

  addResource("/pll_" + board + ".v")
}
