package cases

import chisel3._
import chiselFv._

/**
 * @author Morvan X.
 */

/**
 * Live Value Table Multi-Port RAMs
 * based on https://tomverbeure.github.io/2019/08/03/Multiport-Memories.html
 */

class Memory1(c: Int, w: Int) extends Module {
  val nw  = (math.log(c) / math.log(2)).toInt;
  val io  = IO(new Bundle {
    val rdAddr = Input(UInt(nw.W))
    val rdData = Output(UInt(w.W))
    val wrEna  = Input(Bool())
    val wrData = Input(UInt(w.W))
    val wrAddr = Input(UInt(nw.W))
  })
  val mem = Mem(c, UInt(w.W))
  when(io.wrEna) {
    mem.write(io.wrAddr, io.wrData)
  }
  io.rdData := mem.read(io.rdAddr)
}

class LiveValueTable(m: Int, n: Int, size: Int, addrW: Int) extends Module {
  val io = IO(new Bundle {
    val wrAddr = Input(Vec(m, UInt(3.W)))
    val wrEna  = Input(Vec(m, Bool()))

    val rdAddr = Input(Vec(n, UInt(3.W)))
    val rdIdx  = Output(Vec(n, UInt(math.ceil(math.log(m) / math.log(2)).toInt.W)))
  })

  // initialization
  val lvtInitArray = new Array[Int](size)
  for (i <- 0 until size) {
    lvtInitArray(i) = 0
  }
  val lvtReg = RegInit(VecInit(lvtInitArray.map(_.U(math.ceil(math.log(m) / math.log(2)).toInt.W))))

  for (i <- 0 until m) {
    when(io.wrEna(i) === true.B) {
      lvtReg(io.wrAddr(i)) := i.U
    }
  }

  for (i <- 0 until n) {
    io.rdIdx(i) := lvtReg(io.rdAddr(i))
  }
}


class LVTMultiPortRams(m: Int, n: Int, size: Int, w: Int) extends Module {
  val addrW: Int = math.ceil(math.log(size) / math.log(2)).toInt
  val io         = IO(new Bundle {
    val wrAddr = Input(Vec(m, UInt(addrW.W)))
    val wrData = Input(Vec(m, UInt(w.W)))
    val wrEna  = Input(Vec(m, Bool()))

    val rdAddr = Input(Vec(n, UInt(addrW.W)))
    val rdData = Output(Vec(n, UInt(w.W)))
  })
  val mems       = VecInit(Seq.fill(m * n)(Module(new Memory1(size, w)).io))
  val lvt        = Module(new LiveValueTable(m, n, size, addrW))

  for (i <- 0 until m) {
    for (j <- 0 until n) {
      mems(i * n + j).wrEna := io.wrEna(i)
      mems(i * n + j).wrAddr := io.wrAddr(i)
      mems(i * n + j).wrData := io.wrData(i)
      mems(i * n + j).rdAddr := io.rdAddr(j)
    }
  }
  for (i <- 0 until m) {
    lvt.io.wrEna(i) := io.wrEna(i)
    lvt.io.wrAddr(i) := io.wrAddr(i)
  }
  for (i <- 0 until n) {
    lvt.io.rdAddr(i) := io.rdAddr(i)
  }
  for (i <- 0 until n) {
    val sel = lvt.io.rdIdx(i)
    io.rdData(i) := mems(sel * n.U + i.U).rdData
  }

  // Formal verification
//  val addr = anyconst(addrW)
//  val hasWritten = initialReg(1, 0)
//  val data = Reg(UInt(w.W))
//  hasWritten.io.in := 0.U
//  for (i <- 0 until m) {
//    when(io.wrAddr(i) === addr && io.wrEna(i) === true.B) {
//      data := io.wrData(i)
//      hasWritten.io.in := 1.U
//      for(j <- 0 until m) {
//        if (i != j) {
//          assume(io.wrAddr(j) =/= addr || io.wrEna(j) === false.B)
//        }
//      }
//    }
//  }
//  for (i <- 0 until n) {
//    when(io.rdAddr(i) === addr && hasWritten.io.out === 1.U) {
//      assert(io.rdData(i) === data)
//    }
//  }

}

object LVTMultiPortRams extends App {
  Check.generateRTL(() => new LVTMultiPortRams(m=2, n=2, size = 4, w = 8))
//  Check.pdr(() => new LVTMultiPortRams(m = 4, n = 4, size = 4, w = 8))
}