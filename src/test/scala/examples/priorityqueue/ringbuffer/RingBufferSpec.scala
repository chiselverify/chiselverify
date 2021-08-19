package examples.priorityqueue.ringbuffer

import Chisel.Decoupled
import chisel3._
import chisel3.experimental.BundleLiterals.AddBundleLiteralConstructor
import chisel3.util.DecoupledIO
import chiseltest._
import org.scalatest.FlatSpec
import chiselverify.crv.backends.jacop._

import scala.collection.mutable
import scala.math.pow

import scala.util.Random._


class RingBufferSpec extends FlatSpec with ChiselScalatestTester {
  behavior of "RingBuffer"

  val depth = 4
  val dataType = UInt(8.W)

  it should "buffer all items and dequeue them in order" in {
    test(new RingBuffer(dataType)(depth)) { dut =>

      dut.io.enq.initSource().setSourceClock(dut.clock)
      dut.io.deq.initSink().setSinkClock(dut.clock)


      val testSeq = Seq.fill(100)(nextInt(pow(2,dataType.getWidth).toInt).U)

      fork {
        dut.io.enq.enqueueSeq(testSeq)
      }.fork {
        dut.clock.step(10)
        dut.io.deq.expectDequeueSeq(testSeq)
      }.join()

    }
  }

  it should "stop flow when full" in {
    test(new RingBuffer(dataType)(depth)) { dut =>

      dut.io.enq.initSource().setSourceClock(dut.clock)
      dut.io.deq.initSink().setSinkClock(dut.clock)

      val testSeq = Seq.fill(depth+1)(nextInt(pow(2,dataType.getWidth).toInt).U)

      testSeq.foreach(dut.io.enq.enqueueNow(_))
      dut.clock.step()
      dut.io.enq.ready.expect(0.B)

    }
  }

}
