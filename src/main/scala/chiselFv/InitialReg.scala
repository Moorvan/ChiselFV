package chiselFv

import chisel3._
import chisel3.util.HasBlackBoxInline


class InitialReg(w: Int, value: Int) extends BlackBox(Map("WIDTH" -> w, "Value" -> value)) with HasBlackBoxInline {
  val io = IO(new Bundle {
    val clk = Input(Clock())
    val reset = Input(Bool())
    val in = Input(UInt(w.W))
    val out = Output(UInt(w.W))
  })

  setInline("InitialReg.sv",
    s"""
       |module InitialReg #(parameter WIDTH, parameter Value) (
       |    input  clk,
       |    input reset,
       |    input [WIDTH-1:0] in,
       |    output [WIDTH-1:0] out
       |);
       |
       |reg [WIDTH-1:0] reg_;
       |assign out = reg_;
       |initial reg_ = Value;
       |
       |always @(posedge clk) begin
       |  if (reset) reg_ <= Value;
       |  else reg_ <= in;
       |end
       |endmodule
       |""".stripMargin)
}
