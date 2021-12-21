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

  def arbitTwo(a: DecoupledIO[T], b: DecoupledIO[T]) = {

    val regData = Reg(gen)
    val regValid = RegInit(false.B)
    val out = Wire(new DecoupledIO(gen))

    val turnA = RegInit(true.B)
    turnA := !turnA
    a.ready := !regValid & turnA
    b.ready := !regValid & !turnA

    when (regValid) {
      when (out.ready) {
        regValid := false.B
      }
    } .otherwise {
      when (turnA & a.valid) {
        regData := a.bits
        regValid := true.B
      } .elsewhen(!turnA & b.valid) {
        regData := b.bits
        regValid := true.B
      }
    }

    out.bits := regData
    out.valid := regValid
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
  // io.out <> io.in.reduceTree(foo)
  // io.out <> io.in.reduce(foo)
  // io.out <> io.in.treeReduce(add)
  io.out <> myTreeFunctional(io.in, arbitTwo)
}

object Arbiter extends App {
  println((new chisel3.stage.ChiselStage).emitVerilog(new Arbiter(7, UInt(8.W))))
}

