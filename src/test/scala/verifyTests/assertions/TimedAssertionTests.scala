package verifyTests.assertions

import chisel3._
import chisel3.tester._
import chiseltest.ChiselScalatestTester
import chiselverify.assertions.AssertTimed
import chiselverify.timing._
import org.scalatest.{FlatSpec, Matchers}
import verifyTests.ToyDUT.AssertionsToyDUT

class TimedAssertionTests extends FlatSpec with ChiselScalatestTester with Matchers {
    def toUInt(i: Int): UInt = (BigInt(i) & 0x00ffffffffL).asUInt(32.W)

    /**
      * Tests timed assertions in a generic use case
      */
    def testGeneric[T <: AssertionsToyDUT](dut: T, et : EventType): Unit = {

        /**
          * Basic test to see if we get the right amount of hits
          */
        def testAlways(): Unit = {
            dut.io.a.poke(10.U)
            dut.io.b.poke(10.U)
            AssertTimed(dut, () => (dut.io.aEqb === 1.U).litToBoolean, "aEqb timing is wrong")(Always(10)).join()
        }

        def testAlwaysFail(): Unit = {
            dut.io.a.poke(10.U)
            dut.io.b.poke(10.U)
            assertThrows[AssertionError](AssertTimed(dut, () => (dut.io.aEqb === 1.U).litToBoolean, "aEqb timing is wrong")(Always(15)).join())
        }

        def testEventually(): Unit = {
            dut.io.a.poke(10.U)
            dut.io.b.poke(10.U)
            AssertTimed(dut, () => (dut.io.aEvEqC === 1.U).litToBoolean, "a eventually is c")(Eventually(11)).join()
        }

        et match {
            case Always =>
                testAlways()
                testAlwaysFail()
            case Eventually => testEventually()
        }
        testAlways()
        testEventually()
        testAlwaysFail()
    }

    "Timed Assertions" should "pass" in {
        test(new AssertionsToyDUT(32)){ dut => {
            testGeneric(dut, Always)
            testGeneric(dut, Eventually)
        } }
    }
}
