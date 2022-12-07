package verifyTests.assembly

import chiselverify.assembly.{Category, CategoryWhiteList, GeneratorContext, Instruction}
import chiselverify.assembly.leros.Leros
import org.scalatest.flatspec.AnyFlatSpec

class InstructionTest extends AnyFlatSpec {
  behavior of "Instruction producers"

  it should "select randomly between different instructions" in {
    val gc = GeneratorContext(Leros,Seq())
    val options = Seq(Leros.add(),Leros.jal(),Leros.br())
    val selected = Instruction.select(options:_*)(gc)
    val produced = selected.produce()(gc).head
    assert(produced.isInstanceOf[Leros.add] || produced.isInstanceOf[Leros.jal] || produced.isInstanceOf[Leros.br])
    assert(!produced.isInstanceOf[Leros.or])
  }

  it should "pick a random instruction of the correct category" in {
    val gc = GeneratorContext(Leros,Seq())
    val instr = Instruction.ofCategory(Category.Arithmetic)(gc).produce()(gc).head
    assert(instr.isOfCategory(Category.Arithmetic))
  }

  it should "produce a random sequence of instructions" in {
    val gc = GeneratorContext(Leros,Seq(CategoryWhiteList(Category.Arithmetic,Category.Logical)))
    val instr = Instruction.fill(10)(gc).produce()(gc)
    assert(instr.length == 10)
  }

  it should "produce a random sequence of instructions of the correct category" in {
    val gc = GeneratorContext(Leros,Seq(CategoryWhiteList(Category.Arithmetic,Category.Logical)))
    val instr = Instruction.fillWithCategory(10)(Category.Arithmetic)(gc).produce()(gc)
    assert(instr.foldLeft(true){ case (acc,i) => acc && i.isOfCategory(Category.Arithmetic) })
    assert(instr.length == 10)
  }
}
