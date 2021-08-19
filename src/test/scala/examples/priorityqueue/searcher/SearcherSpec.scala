package examples.priorityqueue.searcher

import chisel3._
import chisel3.experimental.BundleLiterals.AddBundleLiteralConstructor
import chiseltest._
import org.scalatest.FlatSpec
import examples.priorityqueue.QueueItem

class SearcherSpec extends FlatSpec with ChiselScalatestTester {
  behavior of "Searcher"

  it should "generate read requests for all nodes with their siblings" in {
    test(new Searcher(32,4,UInt(4.W))) { dut =>

      implicit val params = dut.params

      dut.io.command.initSource()
      dut.io.command.setSourceClock(dut.clock)
      dut.io.response.initSink()
      dut.io.response.setSinkClock(dut.clock)
      dut.io.heapAccess.read.request.initSink()
      dut.io.heapAccess.read.request.setSinkClock(dut.clock)
      dut.io.heapAccess.read.response.initSource()
      dut.io.heapAccess.read.response.setSinkClock(dut.clock)



      fork {
        dut.io.command.enqueue((new CommandBundle).Lit(_.id -> 2.U))
      }.fork {
        for(i <- 0 until 15) {
          println(s"${dut.io.response.valid.peek.litToBoolean} -> ${dut.io.response.bits.index.peek.litValue}")
          dut.clock.step()
        }
        //dut.io.response.expectDequeue((new ResponseBundle).Lit(_.index -> 2.U))
      }.fork {
        for(i <- 0 until 15) {
          if(dut.io.heapAccess.read.request.valid.peek.litToBoolean) {
            println("Read request")
            dut.io.heapAccess.read.response.valid.poke(1.B)
            val seq = Seq(0,0,2,0).map(i => (new QueueItem).Lit(_.id -> i.U, _.value -> 10.U))
            dut.io.heapAccess.read.response.bits.items.zip(seq).foreach { case (port,poke) => port.poke(poke)}
            dut.clock.step()
            dut.io.heapAccess.read.response.valid.poke(0.B)
          } else {
            dut.clock.step()
          }

        }
        //dut.io.response.expectDequeue((new ResponseBundle).Lit(_.index -> 2.U))
      }.join()



    }
  }
  it should "raise error when no match was found" in {
    test(new Searcher(32,4,UInt(4.W))) { dut =>

      implicit val params = dut.params

      dut.io.command.initSource()
      dut.io.command.setSourceClock(dut.clock)
      dut.io.response.initSink()
      dut.io.response.setSinkClock(dut.clock)
      dut.io.heapAccess.read.request.initSink()
      dut.io.heapAccess.read.request.setSinkClock(dut.clock)
      dut.io.heapAccess.read.response.initSource()
      dut.io.heapAccess.read.response.setSinkClock(dut.clock)



      fork {
        dut.io.command.enqueue((new CommandBundle).Lit(_.id -> 2.U))
      }.fork {
        for(i <- 0 until params.size) {
          dut.io.error.expect(0.B)
          dut.clock.step()
        }
        dut.io.error.expect(1.B)
      }.fork {
        for(i <- 0 until params.size+1) {
          if(dut.io.heapAccess.read.request.valid.peek.litToBoolean) {
            dut.io.heapAccess.read.response.valid.poke(1.B)
            dut.clock.step()
            dut.io.heapAccess.read.response.valid.poke(0.B)
          } else {
            dut.clock.step()
          }
        }
        //dut.io.response.expectDequeue((new ResponseBundle).Lit(_.index -> 2.U))
      }.join()
    }
  }
}
