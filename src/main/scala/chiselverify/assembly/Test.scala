package chiselverify.assembly

import chiselverify.assembly.instructionsets.rv32i.{RV32Category, RV32I2}

object Test extends App {
  val pg = ProgramGenerator(RV32I2) { c =>
    c.withRegisters(r =>
      Seq(r.zero, r.ra, r.a0)
    ).withMemoryAccessDistribution(
      (0x00 until 0x100) -> 0.5,
      (0x1000 until 0x2000) -> 0.1
    ).withInstructions(isa =>
      isa.values.filter(_.categories.contains(Category.ArithmeticInstruction))
    ).withCategoryDistribution(
      Category.ArithmeticInstruction -> 0.7,
      Category.LoadInstruction -> 0.1,
      RV32Category.SBtype -> 0.2
    )
  }
  println(pg.generate(100).map(_.toAsm).mkString("\n"))
}
