package examples.leros

import chisel3._
import chisel3.util._

import examples.leros.Types._

object Types {
  val nop :: add :: sub :: and :: or :: xor :: ld :: shr :: Nil = Enum(8)
}

/** Leros ALU including the accumulator register.
  *
  * @param size
  */
class AluAccuChisel(size: Int) extends AluAccu(size) {

  val accuReg = RegInit(0.U(size.W))

  val op = io.op
  val a = accuReg
  val b = io.din
  val res = WireDefault(a)

  switch(op) {
    is(nop) {
      res := a
    }
    is(add) {
      res := a + b
    }
    is(sub) {
      res := a - b
    }
    is(and) {
      res := a & b
    }
    is(or) {
      res := a | b
    }
    is(xor) {
      res := a ^ b
    }
    is(shr) {
      res := a >> 1
    }
    is(ld) {
      res := b
    }
  }

  when(io.ena) {
    accuReg := res
  }

  io.accu := accuReg
}
