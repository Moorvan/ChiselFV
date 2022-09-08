package cases

import Chisel.log2Ceil
import chisel3._
import chiselFv._

class Memory(c: Int, w: Int) extends Module with Formal {
  val nw  = log2Ceil(c)
  val io  = IO(new Bundle {
    val wrEna  = Input(Bool())
    val wrData = Input(UInt(w.W))
    val wrAddr = Input(UInt(nw.W))
    val rdAddr = Input(UInt(nw.W))
    val rdData = Output(UInt(w.W))
  })
  val mem = Mem(c, UInt(w.W))
  val a   = true.B

  when(io.wrEna) {
    mem.write(io.wrAddr, io.wrData)
  }

  io.rdData := mem.read(io.rdAddr)

  // Formal verification
  val flag_value = WireInit(0.U(1.W))
  val addr       = anyconst(nw)
  val flag       = initialReg(1, 0)
  val data       = Reg(UInt(w.W))

  flag.io.in := flag_value
  when(io.wrAddr === addr & io.wrEna) {
    flag_value := 1.U
    data := io.wrData
  }
  when(io.rdAddr === addr && flag.io.out === 1.U) {
    assert(data === io.rdData)
  }

}

object Memory extends App {
  Check.kInduction(() => new Memory(1024, 8))
//  Check.kInduction(() => new Memory(2048, 8))
}