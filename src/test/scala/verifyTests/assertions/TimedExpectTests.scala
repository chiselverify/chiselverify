package verifyTests.assertions

import chisel3._
import chisel3.tester.{testableClock, testableData}
import chiseltest.ChiselScalatestTester
import chiselverify.assertions._
import chiselverify.timing._
import org.scalatest.{FlatSpec, Matchers}
import verifyTests.ToyDUT.AssertionsToyDUT

class TimedExpectTests extends FlatSpec with ChiselScalatestTester with Matchers {
  def toUInt(i: Int): UInt = (BigInt(i) & 0x00ffffffffL).asUInt(32.W)

  /** Tests timed assertions in a generic use case
    */
  def testGeneric[T <: AssertionsToyDUT](dut: T, et: EventType): Unit = {

    /** Basic test to see if we get the right amount of hits
      */
    def testAlways(): Unit = {
      dut.io.a.poke(10.U)
      dut.io.b.poke(10.U)
      dut.clock.step(1)
      ExpectTimed(dut, dut.io.aEqb, 1.U, "aEqb expected timing is wrong")(Always(9)).join()
    }

    def testEventually(): Unit = {
      dut.io.a.poke(10.U)
      dut.io.b.poke(10.U)
      dut.clock.step(1)
      ExpectTimed(dut, dut.io.aEvEqC, 1.U, "a never equals b within the first 11 cycles")(Eventually(11)).join()
    }

    def testExactly(): Unit = {
      dut.io.a.poke(7.U)
      dut.clock.step(1)
      ExpectTimed(dut, dut.io.aEvEqC, 1.U, "aEqb expected timing is wrong")(Exactly(6)).join()
    }

    def testNever(): Unit = {
      dut.io.a.poke(10.U)
      dut.io.b.poke(20.U)
      dut.clock.step(1)
      ExpectTimed(dut, dut.io.aNevEqb, 0.U, "a is equal to b at some point")(Never(10)).join()
    }

    et match {
      case Always     => testAlways()
      case Eventually => testEventually()
      case Exactly    => testExactly()
      case Never      => testNever()
    }
  }

  "Timed Expect Always" should "pass" in {
    test(new AssertionsToyDUT(32)) { dut => testGeneric(dut, Always) }
  }
  "Timed Expect Eventually" should "pass" in {
    test(new AssertionsToyDUT(32)) { dut => testGeneric(dut, Eventually) }
  }
  "Timed Expect Exactly" should "pass" in {
    test(new AssertionsToyDUT(32)) { dut => testGeneric(dut, Exactly) }
  }
  "Timed Expect Never" should "pass" in {
    test(new AssertionsToyDUT(32)) { dut => testGeneric(dut, Never) }
  }
}
