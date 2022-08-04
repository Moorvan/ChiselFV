package chiselFv

import chisel3._
import chisel3.util.HasBlackBoxInline

class AnyConst(w: Int) extends BlackBox(Map("WIDTH" -> w)) with HasBlackBoxInline {
  val io = IO(new Bundle() {
    val out = Output(UInt(w.W))
  })

  setInline(s"AnyConst.sv",
    s"""
       |module AnyConst #(parameter WIDTH) (
       |    output [WIDTH-1:0] out
       |);
       |
       |(* anyconst *) reg [WIDTH-1:0] cst;
       |assign out = cst;
       |
       |endmodule
       |""".stripMargin)
}
