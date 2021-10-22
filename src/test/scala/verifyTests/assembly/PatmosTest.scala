package chiselverify.assembly.tests

import chiselverify.assembly.patmos.Patmos.ADD
import chiselverify.assembly.patmos.{Bundle, Patmos}
import chiselverify.assembly.{Category, CategoryDistribution, Pattern, ProgramGenerator}

object PatmosTest extends App {


  val pattern = Pattern(implicit c => Seq(
    ADD(), Bundle(ADD(), ADD()), ADD()
  ))

  val pg = ProgramGenerator(Patmos)(
    CategoryDistribution(
      Category.Arithmetic -> 0.5,
      Category.Logical -> 0.5
    ),
    MemoryDistribution(
      (100 until 200) -> 0.5,
      4000 -> 0.5
    )
  )

  val programs = Seq.fill(2)(pg.generate(pattern))

  val programStrings = programs.map(_.map(_.toAsm).mkString("\n"))
  println(programStrings.mkString(s"\n${"-" * 20}\n"))

  println(Category.all.map { c =>
    (c.toString, programs.head.count(_.categories.contains(c)))
  }.mkString("Dist: ", ", ", ""))

}
