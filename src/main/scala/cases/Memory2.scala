package cases

import chisel3._
import chisel3.util._
import chiselFv._

class Memory2(WIDTH: Int, PSIZE: Int) extends Module with Formal {
  val DEPTH = math.pow(2, PSIZE).toInt
  val io    = IO(new Bundle() {
    val in_wr      = Input(Bool())
    val in_rd      = Input(Bool())
    val in_data    = Input(UInt(WIDTH.W))
    val in_wr_addr = Input(UInt(PSIZE.W))
    val in_rd_addr = Input(UInt(PSIZE.W))
    val out_data   = Output(UInt(WIDTH.W))
  })

  val mem = RegInit(VecInit(Seq.fill(DEPTH)(0.U(WIDTH.W))))
  when(io.in_wr) {
    when(io.in_wr_addr < (DEPTH / 2).U) {
      mem(io.in_wr_addr) := io.in_data
    }.otherwise {
      mem(io.in_wr_addr) := Cat(io.in_data(WIDTH / 2 - 1, 0), io.in_data(WIDTH - 1, WIDTH / 2))
    }
  }

  val out_data = RegInit(0.U(WIDTH.W))
  io.out_data := out_data

  when(io.in_rd) {
    out_data := mem(io.in_rd_addr)
  }

  // assert
  val random_addr      = anyconst(PSIZE)
  val random_addr_fail = anyconst(PSIZE)
  val random_data      = RegInit(0.U(WIDTH.W))
  val random_data_fail = RegInit(0.U(WIDTH.W))

  assume((io.in_wr && io.in_rd) === false.B)

  when(io.in_wr && (io.in_wr_addr === random_addr) && (random_addr < (DEPTH / 2).U)) {
    random_data := io.in_data
  }.elsewhen(io.in_wr && (io.in_wr_addr === random_addr)) {
    random_data := Cat(io.in_data(WIDTH / 2 - 1, 0), io.in_data(WIDTH - 1, WIDTH / 2))
  }

  when(io.in_wr && (io.in_wr_addr === random_addr) && (random_addr === random_addr_fail)) {
    random_data_fail := Cat(io.in_data(WIDTH / 2 - 1, 0), io.in_data(WIDTH - 1, WIDTH / 2))
  }.elsewhen(io.in_wr && (io.in_wr_addr === random_addr) && (random_addr < (DEPTH / 2).U)) {
    random_data_fail := io.in_data
  }.elsewhen(io.in_wr && (io.in_wr_addr === random_addr)) {
    random_data_fail := Cat(io.in_data(WIDTH / 2 - 1, 0), io.in_data(WIDTH - 1, WIDTH / 2))
  }

  // pass assertion
//  val flag = RegInit(false.B)
//  when(io.in_rd && (io.in_rd_addr === random_addr)) {
//    flag := true.B
//  }.otherwise {
//    flag := false.B
//  }
//  when(flag) {
//    assert(out_data === random_data)
//  }
  assertNextStepWhen(io.in_rd && (io.in_rd_addr === random_addr), out_data === random_data) // A |=> B

  

  // fail assertion
//  val flag1 = RegInit(false.B)
//  when(io.in_rd && (io.in_rd_addr === random_addr)) {
//    flag1 := true.B
//  }.otherwise {
//    flag1 := false.B
//  }
//  when(flag1) {
//    assert(out_data === random_data_fail)
//  }

}

object Memory2 extends App {
//  Check.generateRTL(() => new Memory2(256, 5))
  Check.bmc(() => new Memory2(8, 4), 10)
}