package chiselFv

import chisel3._
import chisel3.util.HasBlackBoxInline


class InitialReg(w: Int, value: Int) extends BlackBox with HasBlackBoxInline {
  val io = IO(new Bundle {
    val clk = Input(Clock())
    val reset = Input(Bool())
    val in = Input(UInt(w.W))
    val out = Output(UInt(w.W))
  })

  setInline("InitialReg.sv",
    s"""
       |module InitialReg(
       |    input  clk,
       |    input reset,
       |    input [${w-1}:0] in,
       |    output [${w-1}:0] out
       |);
       |
       |reg [${w-1}:0] reg_;
       |assign out = reg_;
       |initial reg_ = $value;
       |
       |always @(posedge clk) begin
       |  if (reset) reg_ <= $value;
       |  else reg_ <= in;
       |end
       |endmodule
       |""".stripMargin)
}
