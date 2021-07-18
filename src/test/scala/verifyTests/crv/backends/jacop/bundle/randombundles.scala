package verifyTests.crv.backends.jacop.bundle

import chisel3.Bundle
import chisel3.tester.ChiselScalatestTester
import chiselverify.crv.backends.jacop.experimental.RandBundle
import chiselverify.crv.backends.jacop.{Constraint, IfCon, RandVar, rand}
import org.scalatest.{FlatSpec, Matchers}

class RandomBunleTests extends FlatSpec with ChiselScalatestTester with Matchers {
  behavior.of("Random Bundles")

  class A extends Bundle with RandBundle {
    val x: RandVar = rand(0, (math.pow(2, 8) - 1).toInt)
    val y: RandVar = rand(0, (math.pow(2, 8) - 1).toInt)
    val greaterThen: Constraint = x > y
    val lessThen:    Constraint = x < y
    lessThen.disable()
  }

  class F extends Bundle with RandBundle {
    val x: RandVar = rand(0, (math.pow(2, 8) - 1).toInt)
    val y: RandVar = rand(0, (math.pow(2, 8) - 1).toInt)
    IfCon(x == 8) {
      y == 9
    }
    val c: Constraint = (x == 8)
    val o: Constraint = y \= 9
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
