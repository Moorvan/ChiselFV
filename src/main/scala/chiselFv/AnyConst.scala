package chiselFv

import chisel3._
import chisel3.util.HasBlackBoxInline

class AnyConst(w: Int) extends BlackBox with HasBlackBoxInline {
  val io = IO(new Bundle() {
    val out = Output(UInt(w.W))
  })

  setInline("AnyConst.sv",
    s"""
       |module AnyConst(
       |    output [${w-1}:0] out
       |);
       |
       |(* anyconst *) reg [${w-1}:0] cst;
       |assign out = cst;
       |
       |endmodule
       |""".stripMargin)
}
