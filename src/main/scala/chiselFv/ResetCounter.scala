package chiselFv

import chisel3._
import chisel3.util.HasBlackBoxInline

class ResetCounter extends BlackBox with HasBlackBoxInline {
  val io = IO(new Bundle {
    val clk = Input(Clock())
    val reset = Input(Bool())
    val timeSinceReset = Output(UInt(32.W))
    val notChaos = Output(Bool())
  })

  setInline("ResetCounter.sv",
    s"""
      |module ResetCounter(
      |    input clk,
      |    input reset,
      |    output [31:0] timeSinceReset,
      |    output notChaos
      |);
      |
      |reg [31:0] count;
      |reg flag;
      |initial begin
      |  count = 0;
      |  flag = 0;
      |end
      |
      |assign timeSinceReset = count;
      |assign notChaos = flag;
      |
      |always @(posedge clk) begin
      |    if (reset) begin
      |        count <= 0;
      |        flag <= 1;
      |    end else if (flag) begin
      |        count <= count + 1;
      |    end
      |end
      |
      |endmodule
    """.stripMargin)
}
