package verifyTests.assembly

import org.scalatest.flatspec.AnyFlatSpec
import org.scalatest.matchers.should.Matchers

import chiselverify.assembly.{GeneratorContext, Pattern}
import chiselverify.assembly.leros.Leros

class PatternTest extends AnyFlatSpec with Matchers {
  behavior of "pattern"

  def getEveryNthItem[T](start: Int, n: Int)(seq: Seq[T]): Seq[T] = {
    seq.zipWithIndex.filter(t => (t._2 - start) % n == 0).map(_._1)
  }

  it should "repeat a pattern 3 times" in {
    val gc = GeneratorContext(Leros, Seq())
    val seq = Pattern.repeat(3)(Leros.read).produce()(gc)
    seq.length should be (6 * 3)
    val mnemonics = seq.map(_.toAsm.split(" ").head)
    getEveryNthItem(0, 6)(mnemonics).forall(_ == mnemonics(0)) should be (true)
    getEveryNthItem(1, 6)(mnemonics).forall(_ == mnemonics(1)) should be (true)
    getEveryNthItem(2, 6)(mnemonics).forall(_ == mnemonics(2)) should be (true)
    getEveryNthItem(3, 6)(mnemonics).forall(_ == mnemonics(3)) should be (true)
    getEveryNthItem(4, 6)(mnemonics).forall(_ == mnemonics(4)) should be (true)
  }
}
