package examples.heappriorityqueue

import chisel3._
import chisel3.experimental.BundleLiterals.AddBundleLiteralConstructor
import chiseltest._
import chiselverify.coverage._
import chiselverify.crv.backends.jacop.{IfCon, Model, Rand, RandObj}
import chiselverify.timing.Eventually
import examples.heappriorityqueue.Helpers._
import examples.heappriorityqueue.Interfaces.{Event, QueryBundle}
import examples.heappriorityqueue.LocalHelpers._
import org.scalatest._


import scala.math.pow
import chiselverify.coverage.{Bins, CoverPoint, CoverageReporter, CrossBin}
import chiselverify.crv.ValueBinder


class PriorityQueueTransaction(seed: Int)(implicit params: PriorityQueueParameters) extends RandObj {
    import params._
    currentModel = new Model(seed)

    val refID = new Rand("refID",0, pow(2,referenceIdWidth).toInt - 1)
    val superCycle = new Rand("superCylce", 0, pow(2,superCycleWidth).toInt - 1)
    val cycle = new Rand("cycle", 0, pow(2,cycleWidth).toInt - 1)
    val valid = new Rand("valid", 0, 1)
    val op = new Rand("op",0,1)

    // produce more valid transactions than invalid ones
    val validDist = valid.dist(0 := 2, 1 := 8)

    val onlyRemoveConstr = op #= 0
    onlyRemoveConstr.disable()

    def toBundle: QueryBundle = {
        (new QueryBundle).Lit(
            _.valid -> valid.value().B,
            _.op -> op.value().B,
            _.refId -> refID.value().U,
            _.event -> (new Event).Lit(
                _.superCycle -> superCycle.value().U,
                _.cycle -> cycle.value().U
            )
        )
    }

    override def toString: String = {
        (if(valid.value() == 1) "valid" else "invalid") + " " + (if(op.value() == 1) {
            s"insertion of event at ${superCycle.value()}:${cycle.value()} with refID ${refID.value()}"
        }else{
            s"removal of event with refID ${refID.value()}"
        })
    }

    def isInsert: Boolean = op.value() == 1
    def isRemove: Boolean = op.value() == 0
    def onlyRemove(): Unit = onlyRemoveConstr.enable()
}

/**
  * contains test for the whole priority queue
  */
class PriorityQueueTest extends FreeSpec with ChiselScalatestTester {

    "HeapPriorityQueue should pass random test" in {

        test(new PriorityQueue(33, 4, 4, 256, true)) { dut =>
            import dut.parameters._
            setWidths(superCycleWidth, cycleWidth, referenceIdWidth)
            val model = new Behavioural(size, order)(superCycleWidth, cycleWidth, referenceIdWidth)

            val cr = new CoverageReporter(dut)
            cr.register(List(
                CoverPoint(dut.io.query.op, "operation")(List(
                    Bins("insertion",       0 to 0),
                    Bins("removal",         1 to 1)
                )),
                CoverPoint(dut.io.query.valid, "valid")(List(
                    Bins("true",            1 to 1),
                    Bins("false",           0 to 0)
                )),
                CoverPoint(dut.io.state.get, "state")(List(
                    Bins("headInsertion",   1 to 1),
                    Bins("normalInsertion", 3 to 3),
                    Bins("lastRemoval",     6 to 6),
                    Bins("headRemoval",     7 to 7),
                    Bins("tailRemoval",     8 to 8),
                    Bins("normalRemoval",   9 to 9)
                )),
                CoverPoint(dut.io.resp.done, "done")(List(
                    Bins("true",            1 to 1),
                    Bins("false",           0 to 0)
                )),
                CoverPoint(dut.io.resp.result, "result")(List(
                    Bins("succes",          0 to 0),
                    Bins("failure",         1 to 1)
                )),
                CoverPoint(dut.io.head.none, "empty")(List(
                    Bins("true",            1 to 1),
                    Bins("false",           0 to 0)
                )),
                CoverPoint(dut.io.head.valid, "valid head")(List(
                    Bins("true",            1 to 1),
                    Bins("false",           0 to 0)
                ))
                ),
                //Declare cross points
                List(
                    TimedCross("valid head and insertion","valid head","operation",Eventually(3))(List(
                        CrossBin("head invalid while inserting", 0 to 0, 1 to 1)
                    )),
                    TimedCross("timed valid","valid","valid",Eventually(10))(List(
                        CrossBin("revoked valid under operation",1 to 1, 0 to 0)
                    ))
                ))

            dut.clock.setTimeout(0)
            LocalHelpers.cr = Some(cr)

            val t = new PriorityQueueTransaction(2000)(dut.parameters)

            // wait for the memory to be initialized
            while (!dut.io.resp.done.peek.litToBoolean) dut.clock.step(1)

            for (_ <- 0 until 1000) {
                t.randomize
                applyPoke(dut,t.toBundle)
                if (t.isInsert) { // insert
                    if(t.valid.value() == 1){
                        assert(model.insert(t.superCycle.value(),t.cycle.value(),t.refID.value()) == getSucces(dut), t.toString)
                    }
                } else { // remove
                    if(t.valid.value() == 1){
                        assert(model.remove(t.refID.value()) == (getSucces(dut), getRmPrio(dut)), t.toString)
                    }

                }
                // compare head of the queue
                assert(dut.io.head.none.peek.litToBoolean == (model.heapSize==0),
                    s"\n${prioAndIdVecToString(model.getMem())}"
                )
                assert(model.getHead == getHead(dut),
                    s"\n${prioAndIdVecToString(model.getMem())}")
            }

            t.onlyRemove()
            while(!dut.io.head.none.peek.litToBoolean){
                t.randomize
                remove(dut,dut.io.head.refId.peek.litValue().toInt)
            }

          cr.printReport()
        }
    }
}

private object LocalHelpers {
    var cr: Option[CoverageReporter[PriorityQueue]] = None
    def applyPoke(dut: PriorityQueue, poke: QueryBundle): Unit = {
        dut.io.query.poke(poke)

        cr.get.step(1)
        cr.get.sample()

        while (!dut.io.resp.done.peek.litToBoolean) {
            cr.get.step(1)
            cr.get.sample()
        }

        dut.io.query.valid.poke(false.B)
        cr.get.step(1)
        cr.get.sample()

    }

    def remove(dut: PriorityQueue, id: Int): Unit = {
        dut.io.query.refId.poke(id.U)
        dut.io.query.op.poke(0.B)
        dut.io.query.valid.poke(true.B)

        dut.clock.step(1)

        while (!dut.io.resp.done.peek.litToBoolean) dut.clock.step(1)

        dut.io.query.valid.poke(false.B)
        while (!dut.io.resp.done.peek.litToBoolean) {
            cr.get.step(1)
            cr.get.sample()
        }

    }

    def getHead(dut: PriorityQueue): Seq[Int] = {
        Seq(dut.io.head.event.superCycle, dut.io.head.event.cycle, dut.io.head.refId).map(_.peek.litValue.toInt)
    }

    def getSucces(dut: PriorityQueue): Boolean = {
        !dut.io.resp.result.peek.litToBoolean
    }

    def getRmPrio(dut: PriorityQueue): Seq[Int] = {
        Seq(dut.io.resp.rmPrio.superCycle, dut.io.resp.rmPrio.cycle).map(_.peek.litValue.toInt)
    }

    def getState(dut: PriorityQueue): String = {
        val states = Array("idle", "headInsertion", "normalInsertion", "initSearch", "waitForSearch", "resetCell", "lastRemoval", "headRemoval", "tailRemoval", "removal", "waitForHeapifyUp", "waitForHeapifyDown")
        states(dut.io.state.get.peek.litValue.toInt)
    }
}