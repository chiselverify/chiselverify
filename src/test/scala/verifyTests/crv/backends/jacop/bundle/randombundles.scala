package verifyTests.crv.backends.jacop.bundle

import chisel3.tester.ChiselScalatestTester
import chisel3.{Bundle, UInt, _}
import chiselverify.crv.backends.jacop.{Constraint, IfCon, rand}
import chiselverify.crv.backends.jacop.experimental.RandBundle
import org.scalatest.{FlatSpec, Matchers}

class RandomBunleTests extends FlatSpec with ChiselScalatestTester with Matchers {
  behavior.of("Random Bundles")

  class A extends Bundle with RandBundle {
    val x = rand(0, (math.pow(2, 8) - 1).toInt)
    val y = rand(0, (math.pow(2, 8) - 1).toInt)
    val greaterThen: Constraint = x > y
    val lessThen:    Constraint = x < y
    lessThen.disable()
  }

  class F extends Bundle with RandBundle {
    val x = rand(0, (math.pow(2, 8) - 1).toInt)
    val y = rand(0, (math.pow(2, 8) - 1).toInt)
    IfCon(x == 8) {
      y == 9
    }
    val c = (x == 8)
    val o = y \= 9
    o.disable()
  }

  it should "Randomize with conditional constraints" in {
    val z = new F()
    val o = z.randomBundle()
    assert(o.x.value == 8)
    assert(o.y.value == 9)
    z.c.disable()
    z.o.enable()
    val t = z.randomBundle()
    assert(t.y.value != 9)
  }

  it should "Randomize Bundles and enable disable constraints" in {
    val z = new A()
    val o = z.randomBundle()
    assert(o.x.value() > o.y.value())
    z.greaterThen.disable()
    z.lessThen.enable()
    val t = z.randomBundle()
    assert(t.x.value() < t.y.value())
  }
}
