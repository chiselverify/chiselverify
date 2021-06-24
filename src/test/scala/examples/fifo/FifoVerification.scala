package examples.fifo

import chisel3._
import chiseltest.ChiselScalatestTester
import org.scalatest.FlatSpec
import chisel.lib.fifo.Fifo
import chiselverify.coverage.CoverageReporter
import chiselverify.coverage._
import chiselverify.crv.RangeBinder
import chiselverify.crv.backends.jacop._

class FifoVerification extends FlatSpec with ChiselScalatestTester {

    def testFifo[T <: Fifo[UInt]](dut: T): Unit = {
        //Define RandObj for CRV
        class FifoGen(seed: Int) extends RandObj {
            currentModel = new Model(seed)
            val randIn: Rand = new Rand("enqRand", 0, 0xFFFF)

            //Constrain input
            val inDist = randIn dist (
                0 to 0xF := 1,
                0xF to 0xFF := 1,
                0xFF to 0xFFF := 1,
                0xFFF to 0xFFFF := 2
            )
        }

        //Create randObj
        val fifoCRV = new FifoGen(42)
        assert(fifoCRV.randomize)

        //Create Verification plan
        val cr = new CoverageReporter(dut)
        cr.register(
            CoverPoint("DeqCoverage", dut.io.deq.bits)(
                Bins("bitsCov", 0 to 0xFFFF)
            ),
            CoverPoint("DeqValidCoverage", dut.io.deq.valid)(
                Bins("validCov", 0 to 1)
            )
        )
    }
}
