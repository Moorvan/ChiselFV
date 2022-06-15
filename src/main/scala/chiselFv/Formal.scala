package chiselFv

import chisel3.internal.sourceinfo.SourceInfo
import chisel3.{assert => cassert, _}


trait Formal {
  this: Module =>

  private val resetCounter = Module(new ResetCounter)
  resetCounter.io.clk := clock
  resetCounter.io.reset := reset
  val timeSinceReset = resetCounter.io.timeSinceReset
  val notChaos = resetCounter.io.notChaos

  def assert(cond: Bool, msg: String = "")
            (implicit sourceInfo: SourceInfo,
             compileOptions: CompileOptions): Unit = {
    when (notChaos) {
      cassert(cond, msg)
    }
  }

  def past[T <: Data](value: T, n: Int)(block: T => Any)
                     (implicit sourceInfo: SourceInfo,
                      compileOptions: CompileOptions): Unit = {
    when (notChaos & timeSinceReset >= n.U) {
      block(Delay(value, n))
    }
  }

  def initialReg(w: Int, v: Int): InitialReg = {
    val reg = Module(new InitialReg(w, v))
    reg.io.clk := clock
    reg.io.reset := reset
    reg
  }

  def anyconst(w: Int): UInt = {
    val cst = Module(new AnyConst(w))
    cst.io.out
  }
}
