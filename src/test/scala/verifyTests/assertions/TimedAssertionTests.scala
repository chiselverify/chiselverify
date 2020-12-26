package verifyTests.assertions

import chisel3._
import chisel3.tester._
import chiseltest.ChiselScalatestTester
import chiselverify.assertions.{AssertTimed, ExpectTimed}
import chiselverify.timing._
import org.scalatest.{FlatSpec, Matchers}
import verifyTests.ToyDUT.AssertionsToyDUT

class TimedAssertionTests extends FlatSpec with ChiselScalatestTester with Matchers {
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
      println(s"aEqb is ${dut.io.aEqb.peek().litValue()}")
      AssertTimed(dut, () => dut.io.aEqb.peek().litValue() == 1, "aEqb timing is wrong")(Always(9)).join()
    }

    def testEventually(): Unit = {
      dut.io.a.poke(10.U)
      dut.io.b.poke(10.U)
      AssertTimed(dut, () => dut.io.aEvEqC.peek().litValue() == 1, "a eventually isn't c")(Eventually(11)).join()
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

  "Timed Assertions Always" should "pass" in {
    test(new AssertionsToyDUT(32)) { dut => testGeneric(dut, Always) }
  }
  "Timed Assertions Eventually" should "pass" in {
    test(new AssertionsToyDUT(32)) { dut => testGeneric(dut, Eventually) }
  }
  "Timed Assertions Exactly" should "pass" in {
    test(new AssertionsToyDUT(32)) { dut => testGeneric(dut, Exactly) }
  }
  "Timed Assertions Never" should "pass" in {
    test(new AssertionsToyDUT(32)) { dut => testGeneric(dut, Never) }
  }
}
