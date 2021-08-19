package chiselverify.assembly

import chiselverify.assembly.instructionsets.rv32i.RV32I2

object Test extends App {
  val pg = ProgramGenerator(RV32I2) { c =>
    c.withRegisters(r =>
      Seq(r.zero, r.ra)
    ).withMemoryAccessDistribution(
      (0x00 until 0x100) -> 0.5,
      (0x1000 until 0x2000) -> 0.1
    ).withInstructions(isa =>
      isa.values.filter(_.categories.contains(Category.ArithmeticInstruction))
    ).withCategoryDistribution(
      Category.ArithmeticInstruction -> 0.4,
      Category.LoadInstruction -> 0.6
    )
  }
  println(pg.generate(100).map(_.toAsm).mkString("\n"))
}
