package examples.priorityqueue.inserter


import chisel3._
import chisel3.experimental.BundleLiterals.AddBundleLiteralConstructor
import chiseltest._
import org.scalatest.FlatSpec
import examples.priorityqueue.{HeapWriteRequestIO, QueueItem}

import scala.math.pow


class InserterSpec extends FlatSpec with ChiselScalatestTester{

  behavior of "Inserter"

  it should "Generate valid write requests" in {
    test(new Inserter(32,4, UInt(3.W))) { dut =>

      implicit val params = dut.params
      val r = scala.util.Random

      dut.io.command.initSource()
      dut.io.command.setSourceClock(dut.clock)
      dut.io.heapAccess.write.request.initSink()
      dut.io.heapAccess.write.request.setSinkClock(dut.clock)


      val testValues = Seq.tabulate(10){ i => (
        r.nextInt(params.size),
        r.nextInt(pow(2,dut.io.command.bits.item.value.getWidth).toInt),
        r.nextInt(params.size)
      )}
      val inputSeq = testValues.map { case (index, value, id) =>
        (new CommandBundle).Lit(_.index -> index.U, _.item -> (new QueueItem).Lit(_.value -> value.U, _.id -> id.U))
      }
      val writeReqestSeq = testValues.map { case (index, value, id) =>
        (new HeapWriteRequestIO).Lit(_.index -> index.U, _.item -> (new QueueItem).Lit(_.value -> value.U, _.id -> id.U))
      }

      fork {
        dut.io.command.enqueueSeq(inputSeq)
      }.fork {
        dut.io.heapAccess.write.request.expectDequeueSeq(writeReqestSeq)
      }.join()

    }
  }

  it should "indicate an error for an out of range insertion" in {
    test(new Inserter(32,4, UInt(3.W))) { dut =>
      dut.io.command.bits.index.poke((dut.params.size+1).U)
      dut.io.command.valid.poke(1.B)

      dut.clock.step()

      dut.io.error.expect(1.B)
    }
  }

  it should "clear the error flag when a legal insertion is issued" in {
    test(new Inserter(32,4, UInt(3.W))) { dut =>
      dut.io.command.bits.index.poke((dut.params.size+1).U)
      dut.io.command.valid.poke(1.B)

      dut.clock.step()

      dut.io.error.expect(1.B)

      dut.io.command.bits.index.poke((dut.params.size-1).U)

      dut.clock.step()

      dut.io.error.expect(0.B)
    }
  }

  it should "be immediately ready when an out of range insertion was issued" in {
    test(new Inserter(32,4, UInt(3.W))) { dut =>
      dut.io.command.bits.index.poke(33.U)
      dut.io.command.valid.poke(1.B)

      dut.clock.step()

      dut.io.command.ready.expect(1.B)
    }
  }

  it should "deassert ready while processing a request" in {
    test(new Inserter(32,4, UInt(3.W))) { dut =>
      dut.io.command.bits.index.poke(13.U)
      dut.io.command.valid.poke(1.B)

      dut.clock.step()

      dut.io.command.valid.poke(0.B)

      dut.io.command.ready.expect(0.B)

      dut.clock.step()

      dut.io.command.ready.expect(1.B)
    }
  }



}
