package examples

import chisel3._
import chisel3.util._
import java.lang.Math.{floor, log10, pow}

class Arbiter[T <: Data: Manifest](n: Int, private val gen: T) extends Module {
  val io = IO(new Bundle {
    val in = Flipped(Vec(n, new DecoupledIO(gen)))
    val out = new DecoupledIO(gen)
  })

  def myTreeFunctional[T](s: Seq[T], op: (T, T) => T): T = {
    val n = s.length
    require(n > 0, "Cannot apply reduction on a Seq of size 0")

    n match {
      case 1 => s(0)
      case 2 => op(s(0), s(1))
      case _ =>
        val m =  pow(2, floor(log10(n-1)/log10(2))).toInt // number of nodes in next level, will be a power of 2
        val k = 2 * (n - m) // number of nodes combined
        val p = n - k // number of nodes promoted

        val l =  s.take(p)
        val r = s.drop(p).grouped(2).map {
          case Seq(a, b) => op(a, b)
        }.toSeq

        myTreeFunctional(l ++ r, op)
    }
  }

  def arbitrateTwo(a: DecoupledIO[T], b: DecoupledIO[T]) = {
    val idleA :: idleB :: hasA :: hasB :: Nil = Enum(4)
    val regData = Reg(gen)
    val regState = RegInit(idleA)
    val out = Wire(new DecoupledIO(gen))

    a.ready := regState === idleA
    b.ready := regState === idleB
    out.valid := (regState === hasA || regState === hasB)

    switch(regState) {
      is (idleA) {
        when (a.valid) {
          regData := a.bits
          regState := hasA
        } otherwise {
          regState := idleB
        }
      }
      is (idleB) {
        when (b.valid) {
          regData := b.bits
          regState := hasB
        } otherwise {
          regState := idleA
        }
      }
      is (hasA) {
        when (out.ready) {
          regState := idleB
        }
      }
      is (hasB) {
        when (out.ready) {
          regState := idleA
        }
      }
    }

    out.bits := regData
    out
  }

  def add(a: DecoupledIO[T], b: DecoupledIO[T]) = {
    val out = Wire(new DecoupledIO(gen))
    out.bits := a.bits.asUInt() + b.bits.asUInt()
    a.ready := true.B
    b.ready := true.B
    out.valid := true.B
    out
  }
  io.out <> myTreeFunctional(io.in, arbitrateTwo)
}

object Arbiter extends App {
  println((new chisel3.stage.ChiselStage).emitVerilog(new Arbiter(7, UInt(8.W))))
}
