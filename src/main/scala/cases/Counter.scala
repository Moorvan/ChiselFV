package cases

import chisel3._
import chiselFv._

class Counter(max_val: Int) extends Module with Formal {
  val io = IO(new Bundle {
    val count = Output(UInt(8.W))
  })
  val count = RegInit(0.U(8.W))
  count := count + 1.U
  when(count === max_val.U) {
    count := 0.U
  }
  io.count := count

  assert(count <= max_val.U)
}

object CounterCheck1 extends App {
  Check.bmc(() => new Counter(10), 10)
  Check.kInduction(() => new Counter(10), 10)
  Check.pdr(() => new Counter(10), 10)
}



  //}


  //  assert(count < 11.U)
  //
  //  assertAt(0.U, count === 0.U) // when reset, count is 0
  //  assertAt(1.U, count === 1.U) // at cycle 1, count is 1
  //  assertAt(2.U, count === 2.U) // at cycle 2, count is 2
  //  assertAt(3.U, count === 3.U) // at cycle 3, count is 3
  //  assertAt(4.U, count === 4.U) // at cycle 4, count is 4
  //  assertAt((max_val + 1).U, count === 0.U) // at cycle max_val + 1, count is 0
  //  past(count, 1) { lastCount =>
  //    when(lastCount =/= max_val.U) {
  //      assert(lastCount + 1.U === count)
  //    }
  //  }
  //
  //  freeze(count, 1) { cnt =>
  //    assertNextStepWhen(cnt =/= max_val.U, cnt + 1.U === count)
  //  }

  //  val cnt = RegInit(count)
  //  cnt := count


  //  when(cnt =/= max_val.U && timeSinceReset > 1.U) {
  //    assert(cnt + 1.U === count)
  //  }
  //
//      assertNextStepWhen(count === max_val.U, count === 0.U) // |=>
  //    assertNextStepWhen(count =/= max_val.U, count =/= 0.U)
  //    assertAfterNStepWhen(count === max_val.U, 1, count === 0.U) // A -> ##n B
  //    assertAfterNStepWhen(count === max_val.U, 3, count === 2.U)
  //    assertAlwaysAfterNStepWhen(count === max_val.U, 1, count >= 0.U) // A -> ##[n:] B
//}


//object Counter1 extends App {
////  Check.generateRTL(() => new Counter1(10))
////  Check.generateAIG(() => new Counter(10))
//  Check.checkWith(() => new Counter, "mc", Check.modelChecker, Check.generateAIG)
//  Check.bmc(() => new Counter(10))
//}
