package cases

import chisel3._
import chiselFv._

class Buffer(width: Int, psize: Int) extends Module with Formal {
  val depth = math.pow(2, psize).toInt
  val io    = IO(new Bundle() {
    val in_wr     = Input(Bool())
    val in_wdata  = Input(UInt(width.W))
    val in_rd     = Input(Bool())
    val out_rdata = Output(UInt(width.W))
    val out_empty = Output(Bool())
    val out_full  = Output(Bool())

    val rnd_mark = Input(Bool())
  })

  val out_rdata = RegInit(0.U(width.W))
  io.out_rdata := out_rdata

  val out_full = RegInit(false.B)
  io.out_full := out_full

  val buffer = Mem(depth, UInt(width.W))
  val wrptr  = RegInit(0.U(psize.W))
  val rdptr  = RegInit(0.U(psize.W))

  val pdiff = rdptr === (wrptr + 1.U)

  io.out_empty := (wrptr === rdptr) & ~out_full

  when(pdiff && io.in_wr && !io.in_rd) {
    out_full := true.B
  }.elsewhen(!io.in_wr && io.in_rd) {
    out_full := false.B
  }.otherwise {
    out_full := out_full
  }

  when(io.in_wr) {
    when(wrptr < (depth - 1).U) {
      wrptr := wrptr + 1.U
    }.otherwise {
      wrptr := 0.U
    }
  }.otherwise {
    wrptr := wrptr
  }

  when(io.in_rd) {
    when(rdptr < (depth - 1).U) {
      rdptr := rdptr + 1.U
    }.otherwise {
      rdptr := 0.U
    }
  }.otherwise {
    rdptr := rdptr
  }

  when(io.in_wr) {
    buffer.write(wrptr, io.in_wdata);
  }

  when(io.in_rd) {
    out_rdata := buffer.read(rdptr)
  }.otherwise {
    out_rdata := out_rdata
  }

  // Formal Assertions

  // Assert Pass
  when(out_full) {
    assume(!io.in_wr)
  }
  when(io.out_empty) {
    assume(!io.in_rd)
  }

  val in_rd_ff1 = RegInit(false.B)
  val flag      = RegInit(false.B)
  val rnd_mark  = Wire(Bool())
  rnd_mark := io.rnd_mark

  val mark_vld  = io.in_wr & rnd_mark
  val check_vld = in_rd_ff1 & (out_rdata + 1.U === 0.U)

  in_rd_ff1 := io.in_rd


  when(check_vld) {
    flag := false.B
  }.elsewhen(mark_vld) {
    flag := true.B
  }

  when(mark_vld) {
    assume(io.in_wdata + 1.U === 0.U)
  }
  when(!mark_vld && io.in_wr) {
    assume(io.in_wdata === 0.U)
  }
  when(flag) {
    assume(!mark_vld)
  }

  when(check_vld) {
    assert(flag)
  }


  // Assert Fail
  val flag_fail      = RegInit(false.B)
  val check_vld_fail = in_rd_ff1 & (out_rdata + 2.U === 0.U)

  when(check_vld_fail) {
    flag_fail := false.B
  }.elsewhen(mark_vld) {
    flag_fail := true.B
  }


//  when(flag_fail) {
//    assert(!mark_vld)
//  }

}


object Buffer extends App {
  //  Check.generateRTL(() => new Buffer(512, 2))
  Check.bmc(() => new Buffer(4, 2))
}