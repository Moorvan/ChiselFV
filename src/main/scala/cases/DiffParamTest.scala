package cases

import chisel3._
import chiselFv._

class DiffParamTest extends Module with Formal {
  val a = anyconst(8)
  val b = anyconst(16)
  when (a === b) {
    assert(a === b)
  }

  val c = initialReg(8, 1)
  val d = initialReg(16, 1)

  c.io := DontCare
  d.io := DontCare
  c.io.in := 3.U
  d.io.in := 3.U
  assert(c.io.out === d.io.out)
}

object AnyconstTest extends App {
  Check.kInduction(() => new DiffParamTest)
}
