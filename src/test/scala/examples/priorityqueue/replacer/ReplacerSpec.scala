package examples.priorityqueue.replacer

import chisel3._
import chisel3.experimental.BundleLiterals.AddBundleLiteralConstructor
import chiseltest._
import org.scalatest.FlatSpec
import examples.priorityqueue.{HeapReadRequestIO,QueueItem,HeapWriteRequestIO}

class ReplacerSpec extends FlatSpec with ChiselScalatestTester {
  behavior of "Replacer"

  it should "issue read request for the source item" in {
    test(new Replacer(32,4,UInt(5.W))) { dut =>

      implicit val params = dut.params

      dut.io.command.initSource()
      dut.io.command.setSourceClock(dut.clock)
      dut.io.heapAccess.read.request.initSink()
      dut.io.heapAccess.read.request.setSinkClock(dut.clock)

      fork {
        dut.io.command.enqueue((new CommandBundle).Lit(_.sourceIndex -> 2.U, _.destinationIndex -> 10.U))
      }.fork {
        dut.io.heapAccess.read.request.expectDequeue((new HeapReadRequestIO).Lit(_.index -> 2.U, _.withSiblings -> 0.B))
      }.join()
    }
  }

  it should "issue write request with source item" in {
    test(new Replacer(32,4,UInt(5.W))) { dut =>

      implicit val params = dut.params

      dut.io.command.initSource()
      dut.io.command.setSourceClock(dut.clock)
      dut.io.heapAccess.read.request.initSink()
      dut.io.heapAccess.read.request.setSinkClock(dut.clock)
      dut.io.heapAccess.write.request.initSink()
      dut.io.heapAccess.write.request.setSinkClock(dut.clock)

      dut.io.heapAccess.read.response.bits.items(0).poke((new QueueItem).Lit(
        _.id -> 5.U,
        _.value -> 25.U
      ))
      dut.io.heapAccess.read.response.valid.poke(1.B)
      dut.clock.step()

      fork {
        dut.io.command.enqueue((new CommandBundle).Lit(
          _.sourceIndex -> 2.U,
          _.destinationIndex -> 10.U
        ))
      }.fork {
        dut.io.heapAccess.read.request.expectDequeue((new HeapReadRequestIO).Lit(
          _.index -> 2.U,
          _.withSiblings -> 0.B
        ))
      }.fork {
        dut.io.heapAccess.write.request.expectDequeue((new HeapWriteRequestIO).Lit(
          _.index -> 10.U,
          _.item -> (new QueueItem).Lit(
            _.id -> 5.U,
            _.value -> 25.U
          )
        ))
      }.join()
    }
  }

  it should "raise error when source index is out of range" in {
    test(new Replacer(32,4,UInt(5.W))) { dut =>

      implicit val params = dut.params

      dut.io.command.bits.sourceIndex.poke((params.size+1).U)
      dut.io.command.valid.poke(1.B)

      dut.clock.step()

      dut.io.error.expect(1.B)
    }
  }

  it should "raise error when destination index is out of range" in {
    test(new Replacer(32,4,UInt(5.W))) { dut =>

      implicit val params = dut.params

      dut.io.command.bits.destinationIndex.poke((params.size+1).U)
      dut.io.command.valid.poke(1.B)

      dut.clock.step()

      dut.io.error.expect(1.B)
    }
  }

  it should "clear error flag after legal command" in {
    test(new Replacer(32,4,UInt(5.W))) { dut =>

      implicit val params = dut.params

      dut.io.command.bits.sourceIndex.poke((params.size+1).U)
      dut.io.command.bits.destinationIndex.poke((params.size+1).U)
      dut.io.command.valid.poke(1.B)

      dut.clock.step()

      dut.io.error.expect(1.B)

      dut.io.command.bits.sourceIndex.poke((params.size-1).U)
      dut.io.command.bits.destinationIndex.poke((params.size-2).U)

      dut.clock.step()

      dut.io.error.expect(0.B)

    }
  }

}
