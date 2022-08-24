package cases

import chisel3._
import chiselFv._

class Counter(max_val: Int) extends Module with Formal {
  val io = IO(new Bundle {
    val count = Output(UInt(8.W))
  })
  val count = RegInit(0.U(8.W))
  count := count + 1.U
  when (count === max_val.U) {
    count := 0.U
  }
  io.count := count
  assert(count < 11.U)
//  past(count, 1) { lastCount =>
//    when(lastCount =/= max_val.U) {
//      assert(lastCount + 1.U === count)
//    }
//  }
//
//  freeze(count, 1) { cnt =>
//    assertNextStepWhen(cnt =/= max_val.U, cnt + 1.U === count)
//  }

  val cnt = RegInit(count)
  cnt := count
  when(cnt =/= max_val.U && timeSinceReset > 1.U) {
    assert(cnt + 1.U === count)
  }

//  assertNextStepWhen(count === max_val.U, count === 0.U) // |=>
//  assertNextStepWhen(count =/= max_val.U, count =/= 0.U)
//  assertAfterNStepWhen(count === max_val.U, 1, count === 0.U) // A -> ##n B
//  assertAfterNStepWhen(count === max_val.U, 3, count === 2.U)
//  assertAlwaysAfterNStepWhen(count === max_val.U, 1, count >= 0.U) // A -> ##[n:] B
}

object Counter extends App {
  Check.kInduction(() => new Counter(10))
}
