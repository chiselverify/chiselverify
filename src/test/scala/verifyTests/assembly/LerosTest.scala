package verifyTests.assembly

import chiselverify.assembly.RandomHelpers.BigRange
import chiselverify.assembly.leros.Leros
import chiselverify.assembly.leros.Leros.{add, read, write}
import chiselverify.assembly.{Category, CategoryDistribution, IODistribution, Instruction, Label, MemoryDistribution, Pattern, ProgramGenerator}

object LerosTest extends App {

  val pattern = Pattern(implicit c => Seq(
    Label("Hello"), add(), Instruction.fill(20), Instruction.fillWithCategory(10)(Category.Logical), read, write
  ))


  val pg = ProgramGenerator(Leros)(
    CategoryDistribution(
      Category.Arithmetic -> 0.6,
      Category.Logical -> 0.2,
      Category.Input -> 0.1,
      Category.Output -> 0.1
    ),
    MemoryDistribution(
      BigRange(100, 200) -> 0.5,
      BigRange(4000) -> 0.5
    ),
    IODistribution(
      BigRange(20, 30) -> 0.5,
      BigRange(0xFF) -> 0.5
    )
  )

  val program = pg.generate(pattern)
  println("Program1:\n" + program.map(_.toAsm).mkString("\n"))

  println(Category.all.map { c =>
    (c.toString, program.count(_.categories.contains(c)))
  }.mkString("Dist: ", ", ", ""))

  val program2 = pg.generate(7)
  println("Program2:\n" + program2.map(_.toAsm).mkString("\n"))

  println(Category.all.map { c =>
    (c.toString, program2.count(_.categories.contains(c)))
  }.mkString("Dist: ", ", ", ""))

}
