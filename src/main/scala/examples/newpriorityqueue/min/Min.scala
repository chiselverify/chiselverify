package examples.newpriorityqueue.min

import chisel3._
import chisel3.experimental.BundleLiterals.AddBundleLiteralConstructor
import examples.newpriorityqueue.util.OrderedData

/**
 * Output bundle for minimum module
 *
 * @param gen factory function for [[T]] type
 * @tparam T compared type (must extend chisel base type [[Data]])
 */
class MinBundle[T <: Data](gen: => T) extends Bundle {
  val item = gen
  val index = UInt()

  override def toString(): String = s"$item (index = ${index.litValue})"

  override def cloneType: MinBundle.this.type = (new MinBundle[T](gen)).asInstanceOf[this.type]
}

/**
 * Chisel Module for determining the minimum between [[n]] items of type [[T]]
 *
 * @param n     number of items
 * @param gen   factory function for [[T]] type
 * @tparam T compared type (must be [[Data]] and implement [[OrderedData]] interface)
 */
class Min[T <: Data with OrderedData[T]](n: Int, gen: => T) extends Module {
  val io = IO(new Bundle {
    val items = Input(Vec(n, gen))
    val min = Output(new MinBundle(gen))
  })

  io.min := io.items.zipWithIndex.map { case (item, index) =>
    val bundle = Wire(new MinBundle(gen)) // pair items with their index
    bundle.item := item
    bundle.index := index.U
    bundle
  }.reduce { (left, right) => // produce reduction tree
    Mux(left.item isSmallerThan right.item, left, right) // select the smaller of the two items
  }
}

object Min {
  /**
   * Determines the minimum in the passed sequence of items
   *
   * @param items   the set to find the minimum of
   * @tparam T the type of the set variables (must be [[Data]] and implement [[OrderedData]] interface)
   * @return        the index and value of the minimum as a [[MinBundle]]
   */
  def apply[T <: Data with OrderedData[T]](items: T*): MinBundle[T] = {
    val minModule = Module(new Min(items.length, items.head.cloneType))
    items.zip(minModule.io.items).foreach { case (input, port) => port := input }
    minModule.io.min
  }
}