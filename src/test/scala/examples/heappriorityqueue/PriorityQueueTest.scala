package examples.heappriorityqueue

import chisel3._
import chiseltest._
import chiselverify.coverage._
import chiselverify.coverage.{cover => ccover}
import examples.heappriorityqueue.Helpers._
import examples.heappriorityqueue.LocalHelpers._
import org.scalatest._


/**
  * contains test for the whole priority queue
  */
class PriorityQueueTest extends FreeSpec with ChiselScalatestTester {

    "HeapPriorityQueue should pass random test" in {
        val size = 33
        val order = 4
        val superCycleRes = 4
        val cyclesPerSuperCycle = 256
        test(new PriorityQueue(size, order, superCycleRes, cyclesPerSuperCycle, true)) { dut =>

            val model = new Behavioural(size, order)(cWid, nWid, rWid)

            val cr = new CoverageReporter(dut)
            cr.register(
                ccover("operation", dut.io.cmd.op)(
                    bin("insertion", 0 to 0),
                    bin("removal", 1 to 1)),
                ccover("cmd.prio.cycl", dut.io.cmd.prio.superCycle)(
                    bin("cyclic", 0 to 3)),
                ccover("cmd.prio.norm", dut.io.cmd.prio.cycle)(
                    bin("lower half", 0 to (Math.pow(2, nWid) / 2 - 1).toInt),
                    bin("upper half", (Math.pow(2, nWid) / 2 - 1).toInt to (Math.pow(2, nWid) - 1).toInt)),
                ccover("head.prio.cycl", dut.io.head.prio.superCycle)(
                    bin("cyclic", 0 to 3)),
                ccover("head.prio.norm", dut.io.head.prio.cycle)(
                    bin("lower half", 0 to (Math.pow(2, nWid) / 2 - 1).toInt),
                    bin("upper half", (Math.pow(2, nWid) / 2 - 1).toInt to (Math.pow(2, nWid) - 1).toInt)),
                //Declare cross points
                ccover("cyclics at ops", dut.io.cmd.op, dut.io.cmd.prio.cycle)(
                    cross("insertion", Seq(0 to 0, 0 to 3)),
                    cross("removal", Seq(1 to 1, 0 to 3))),
                ccover("normals at ops", dut.io.cmd.op, dut.io.head.prio.cycle)(
                    cross("insertion lower half", Seq(0 to 0, 0 to (Math.pow(2, nWid) / 2 - 1).toInt)),
                    cross("insertion upper half", Seq(0 to 0, (Math.pow(2, nWid) / 2 - 1).toInt to (Math.pow(2, nWid) - 1).toInt)),
                    cross("removal lower half", Seq(1 to 1, 0 to (Math.pow(2, nWid) / 2 - 1).toInt)),
                    cross("removal upper half", Seq(1 to 1, (Math.pow(2, nWid) / 2 - 1).toInt to (Math.pow(2, nWid) - 1).toInt)))
            )

            dut.clock.setTimeout(0)

            // wait for the memory to be initialized
            while (!dut.io.cmd.done.peek.litToBoolean) dut.clock.step(1)

            for (_ <- 0 until 1000) {

                val poke = randomPoke(cWid, nWid, rWid)

                if (poke.head == 1) { // insert
                    insert(dut, poke)
                    assert(model.insert(poke) == getSucces(dut),
                        s"tried to insert ${poke.slice(1, 4).mkString(":")}"
                    )
                } else { // remove
                    remove(dut, poke(3))
                    assert(model.remove(poke(3)) == (getSucces(dut), getRmPrio(dut)),
                        s"removing ${poke(3)}"
                    )
                }
                cr.sample()
                // compare head of the queue
                assert(dut.io.head.none.peek.litToBoolean == (model.heapSize==0),
                    s"\n${prioAndIdVecToString(model.getMem())}"
                )
                /*assert(model.getHead == getHead(dut),
                    s"\n${prioAndIdVecToString(model.getMem())}")*/
            }
          cr.printReport()
        }
    }
}

private object LocalHelpers {
    def insert(dut: PriorityQueue, poke: Seq[Int]): Unit = {
        dut.io.cmd.refID.poke(poke(3).U)
        dut.io.cmd.prio.superCycle.poke(poke(1).U)
        dut.io.cmd.prio.cycle.poke(poke(2).U)
        dut.io.cmd.op.poke(true.B) // 1=insert
        dut.io.cmd.valid.poke(true.B)

        dut.clock.step(1)

        while (!dut.io.cmd.done.peek.litToBoolean) dut.clock.step(1)

        dut.io.cmd.valid.poke(false.B)
        dut.clock.step(1)

    }

    def remove(dut: PriorityQueue, id: Int): Unit = {
        dut.io.cmd.refID.poke(id.U)
        dut.io.cmd.op.poke(false.B) // 0=remove
        dut.io.cmd.valid.poke(true.B)

        dut.clock.step(1)

        while (!dut.io.cmd.done.peek.litToBoolean) dut.clock.step(1)

        dut.io.cmd.valid.poke(false.B)

    }

    def randomPoke(cWid: Int, nWid: Int, rWid: Int): Seq[Int] = {
        val rand = scala.util.Random
        Seq(rand.nextInt(2), rand.nextInt(math.pow(2, cWid).toInt), rand.nextInt(math.pow(2, nWid).toInt), rand.nextInt(math.pow(2, rWid).toInt - 1))
    }

    def getHead(dut: PriorityQueue): Seq[Int] = {
        Seq(dut.io.head.prio.superCycle, dut.io.head.prio.cycle, dut.io.head.refID).map(_.peek.litValue.toInt)
    }

    def getSucces(dut: PriorityQueue): Boolean = {
        !dut.io.cmd.result.peek.litToBoolean
    }

    def getRmPrio(dut: PriorityQueue): Seq[Int] = {
        Seq(dut.io.cmd.rm_prio.superCycle, dut.io.cmd.rm_prio.cycle).map(_.peek.litValue.toInt)
    }

    def getState(dut: PriorityQueue): String = {
        val states = Array("idle", "headInsertion", "normalInsertion", "initSearch", "waitForSearch", "resetCell", "lastRemoval", "headRemoval", "tailRemoval", "removal", "waitForHeapifyUp", "waitForHeapifyDown")
        states(dut.io.state.get.peek.litValue.toInt)
    }
}