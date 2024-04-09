package memory_0

import chisel3._
import chisel3.util._
import chiselFv._

class Memory(c: Int, w: Int) extends Module with Formal {
  val nw = log2Ceil(c)
  val io = IO(new Bundle {
    val wrEna = Input(Bool())
    val wrData = Input(UInt(w.W))
    val wrAddr = Input(UInt(nw.W))
    val rdAddr = Input(UInt(nw.W))
    val rdData = Output(UInt(w.W))
  })
  val mem = Mem(c, UInt(w.W))
  val a = true.B

  when(io.wrEna) {
    mem.write(io.wrAddr, io.wrData)
  }

  io.rdData := mem.read(io.rdAddr)


  // Formal verification
  val flag_value = WireInit(0.U(1.W))
  val addr = anyconst(nw)
  val flag = initialReg(1, 0)
  val data = Reg(UInt(w.W))

  flag.io.in := flag_value
  when(io.wrAddr === addr & io.wrEna) {
    data := io.wrData
  }
  when(io.rdAddr === addr && flag.io.out === 1.U) {
    assert(data === io.rdData)
  }
}


object Memory extends App {
  //  Check.generateRTL(() => new Memory(16, 8))
  Check.kInduction(() => new Memory(1024, 8))
  //  Check.generateBtor(() => new Memory(32, 8))
  //  Check.kInduction(() => new Memory(32, 8))
  //  Check.kInduction(() => new Memory(2048, 8))
}