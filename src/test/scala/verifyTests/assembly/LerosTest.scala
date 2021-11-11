package verifyTests.assembly

import chiselverify.assembly.RandomHelpers.BigRange
import chiselverify.assembly.intToBigIntOption
import chiselverify.assembly.leros.Leros
import chiselverify.assembly.leros.Leros.{add, in, read, write}
import chiselverify.assembly.{Category, CategoryDistribution, IODistribution, Instruction, Label, MemoryDistribution, Pattern, ProgramGenerator}


object LerosTest extends App {

  val pg = ProgramGenerator(Leros)()

  val program = pg.generate(200)
  println(program.pretty)
}

object LerosTestPatternBased extends App {

  val pattern = Pattern(implicit c => Seq(
    Label("Hello"), add(), in(20), Instruction.fill(20), Instruction.fillWithCategory(10)(Category.Logical), read, write
  ))


  val pg = ProgramGenerator(Leros)(
    CategoryDistribution(
      Category.Arithmetic -> 6,
      Category.Logical -> 2,
      Category.Input -> 1,
      Category.Output -> 1
    ),
    MemoryDistribution(
      BigRange(100, 200) -> 1,
      BigRange(4000) -> 1
    ),
    IODistribution(
      BigRange(20, 30) -> 1,
      BigRange(0xFF) -> 1
    )
  )

  val program = pg.generate(pattern)
  println(program.pretty)
}


