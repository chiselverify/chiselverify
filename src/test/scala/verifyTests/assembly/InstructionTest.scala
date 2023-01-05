package verifyTests.assembly

import org.scalatest.flatspec.AnyFlatSpec
import org.scalatest.matchers.should.Matchers

import chiselverify.assembly.{Category, CategoryWhiteList, GeneratorContext, Instruction}
import chiselverify.assembly.leros.Leros

class InstructionTest extends AnyFlatSpec with Matchers {
  behavior of "Instruction producers"

  it should "select randomly between different instructions" in {
    val gc = GeneratorContext(Leros, Seq())
    val options = Seq(Leros.add(), Leros.jal(), Leros.br())
    val selected = Instruction.select(options:_*)(gc)
    val produced = selected.produce()(gc).head
    (produced.isInstanceOf[Leros.add] || produced.isInstanceOf[Leros.jal] || produced.isInstanceOf[Leros.br]) should be (true)
    produced.isInstanceOf[Leros.or] should be (false)
  }

  it should "pick a random instruction of the correct category" in {
    val gc = GeneratorContext(Leros, Seq())
    val instr = Instruction.ofCategory(Category.Arithmetic)(gc).produce()(gc).head
    instr.isOfCategory(Category.Arithmetic) should be (true)
  }

  it should "produce a random sequence of instructions" in {
    val gc = GeneratorContext(Leros, Seq(CategoryWhiteList(Category.Arithmetic, Category.Logical)))
    val instr = Instruction.fill(10)(gc).produce()(gc)
    instr.forall(i => i.isOfCategory(Category.Arithmetic) || i.isOfCategory(Category.Logical)) should be (true)
    instr.length should be (10)
  }

  it should "produce a random sequence of instructions of the correct category" in {
    val gc = GeneratorContext(Leros, Seq(CategoryWhiteList(Category.Arithmetic, Category.Logical)))
    val instr = Instruction.fillWithCategory(10)(Category.Arithmetic)(gc).produce()(gc)
    instr.forall(_.isOfCategory(Category.Arithmetic)) should be (true)
    instr.length should be (10)
  }
}
