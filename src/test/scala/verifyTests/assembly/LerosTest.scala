package chiselverify.assembly.tests

import chiselverify.assembly.leros.Leros
import chiselverify.assembly.leros.Leros.{add, readAccess, writeAccess}
import chiselverify.assembly.{Category, CategoryDistribution, Instruction, Label, Pattern, ProgramGenerator}

object LerosTest extends App {

  val pattern = Pattern(implicit c => Seq(
    Label("Hello"), add(), Instruction.fill(20), Instruction.fillWithCategory(10)(Category.Logical), readAccess, writeAccess
  ))


  val pg = ProgramGenerator(Leros)(
    CategoryDistribution(
      Category.Arithmetic -> 0.6,
      Category.Logical -> 0.2,
      Category.Input -> 0.1,
      Category.Output -> 0.1
    )
  )

  val program = pg.generate(pattern)
  println("Program1:\n" + program.map(i => s"%04d: ${i.toAsm}".format(i.addr)).mkString("\n"))

  println(Category.all.map { c =>
    (c.toString, program.count(_.categories.contains(c)))
  }.mkString("Dist: ", ", ", ""))

  val program2 = pg.generate(7)
  println("Program2:\n" + program2.map(i => s"%04d: ${i.toAsm}".format(i.addr)).mkString("\n"))

  println(Category.all.map { c =>
    (c.toString, program2.count(_.categories.contains(c)))
  }.mkString("Dist: ", ", ", ""))

}
