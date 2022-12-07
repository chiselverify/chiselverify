package verifyTests.assembly

import chiselverify.assembly.{GeneratorContext, Pattern}
import chiselverify.assembly.leros.Leros
import org.scalatest.flatspec.AnyFlatSpec

class PatternTest extends AnyFlatSpec {
  behavior of "pattern"

  def getEveryNthItem[T](start: Int, n: Int)(seq: Seq[T]): Seq[T] = {
    seq.zipWithIndex.filter(t => (t._2 - start) % n == 0).map(_._1)
  }

  it should "repeat a pattern 3 times" in {
    val gc = GeneratorContext(Leros,Seq())
    val seq = Pattern.repeat(3)(Leros.read).produce()(gc)
    assert(seq.length == 6*3)
    val mnemonics = seq.map(_.toAsm.split(" ").head)
    assert(getEveryNthItem(0,6)(mnemonics).forall(_ == mnemonics(0)))
    assert(getEveryNthItem(1,6)(mnemonics).forall(_ == mnemonics(1)))
    assert(getEveryNthItem(2,6)(mnemonics).forall(_ == mnemonics(2)))
    assert(getEveryNthItem(3,6)(mnemonics).forall(_ == mnemonics(3)))
    assert(getEveryNthItem(4,6)(mnemonics).forall(_ == mnemonics(4)))
  }
}
