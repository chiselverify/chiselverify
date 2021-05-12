package examples.newpriorityqueue

import chiseltest._
import examples.newpriorityqueue.heapifier.{HeapifierDatapath, MemoryLoad}
import org.scalatest.FlatSpec
import chisel3._
import chisel3.experimental.BundleLiterals.AddBundleLiteralConstructor
import chiseltest.experimental.TestOptionBuilder.ChiselScalatestOptionBuilder
import chiseltest.internal.WriteVcdAnnotation
import examples.newpriorityqueue.util.OrderedData


class TestData extends Bundle with OrderedData[TestData] {
  val value = UInt(8.W)

  override def isSmallerThan(that: TestData): Bool = ???
}

object TestData {
  def apply(value: Int): TestData = (new TestData).Lit(_.value -> value.U)
}

class HeapifierDatapathTest extends FlatSpec with ChiselScalatestTester{
  "Heapifier" should "pass" in{
    test(new HeapifierDatapath[TestData](32,2)).withAnnotations(Seq(WriteVcdAnnotation)){ dut =>
      dut.fsmChannel.ctrl.loadOp.poke(MemoryLoad.toParentReg)
      dut.mem.rd.data(0).poke(TestData(12))
      dut.mem.rd.data(1).poke(TestData(2))
      dut.clock.step(10)
    }
  }
}
