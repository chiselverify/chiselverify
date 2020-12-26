package examples.heappriorityqueue

import chisel3._
import chiseltest._
import examples.heappriorityqueue.Interfaces.{Priority, PriorityAndID}

/** contains useful conversion as well as poke and peek methods for user defined bundles
  */
object Helpers {
  var cWid = 2
  var nWid = 8
  var rWid = 3

  def setWidths(c: Int, n: Int, r: Int): Unit = {
    cWid = c
    nWid = n
    rWid = r
  }

  def pokePrioAndID(port: PriorityAndID, poke: Seq[Int] = null): Seq[Int] = {
    if (poke != null) {
      port.prio.cycl.poke(poke(0).U)
      port.prio.norm.poke(poke(1).U)
      port.id.poke(poke(2).U)
      return poke
    } else {
      val rand = scala.util.Random
      val poke = Seq(
        rand.nextInt(math.pow(2, cWid).toInt),
        rand.nextInt(math.pow(2, nWid).toInt),
        rand.nextInt(math.pow(2, rWid).toInt)
      )
      pokePrioAndID(port, poke)
    }
  }

  def pokePrioAndIDVec(port: Vec[PriorityAndID], poke: Seq[Seq[Int]] = null): Seq[Seq[Int]] = {
    if (poke != null) Seq.tabulate(port.length)(i => pokePrioAndID(port(i), poke(i)))
    else Seq.tabulate(port.length)(i => pokePrioAndID(port(i)))
  }

  def peekPrioAndId(port: PriorityAndID): Seq[Int] = {
    Seq(port.prio.cycl, port.prio.norm, port.id).map(_.peek.litValue.toInt)
  }

  def peekPrio(port: Priority): Seq[Int] = {
    Seq(port.cycl, port.norm).map(_.peek.litValue.toInt)
  }

  def peekPrioAndIdVec(port: Vec[PriorityAndID]): Seq[Seq[Int]] = {
    Seq.tabulate(port.length)(i => peekPrioAndId(port(i)))
  }

  def prioAndIdToString(data: Seq[Int]): String = {
    data.mkString(":")
  }

  def prioAndIdVecToString(data: Seq[Seq[Int]]): String = {
    data.map(_.mkString(":")).mkString(", ")
  }
}
