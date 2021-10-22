package chiselverify.assembly.tests

import chiselverify.assembly.rv32i.RV32I
import chiselverify.assembly.rv32i.RV32I.{LI, load}
import chiselverify.assembly.{Category, CategoryDistribution, Instruction, Pattern, ProgramGenerator}

object RiscvTest extends App {


  val pattern = Pattern(implicit c => Seq(
    Instruction.ofCategory(Category.Arithmetic), Instruction.fill(4), LI(), Instruction.ofCategory(Category.Load)
  ))


  val pg = ProgramGenerator(RV32I)(
    CategoryDistribution(
      Category.Arithmetic -> 0.9,
      Category.Load -> 0.1
    ),
    MemoryDistribution(
      (0x100 until 0x1000) -> 0.7,
      0xFFFF -> 0.3
    )
  )

  val programs = Seq.fill(2)(pg.generate(pattern))

  val programStrings = programs.map(p => p.map(_.toAsm).mkString("\n") + "\n" + Category.all.map { c =>
    (c.toString, p.count(_.categories.contains(c)))
  }.mkString("Dist: ", ", ", ""))
  println(programStrings.mkString(s"\n${"-" * 20}\n"))


  println(pg.generate(Pattern(implicit c => Seq(Pattern.repeat(10)(load)))).map(_.toAsm).mkString("\n"))


  /*
  val compilerPath = "/opt/riscv32/bin"
  val architecture = "riscv32"
  val buildDir = "build"

  Process("mkdir -p build").!!
  val writer = new PrintWriter(new File(s"$buildDir/p1.s"))
  writer.write(programStrings.head+"\n")
  writer.close()

  println(Process(s"$compilerPath/$architecture-unknown-elf-as $buildDir/p1.s -o $buildDir/a.out").!!)
  println(Process(s"$compilerPath/$architecture-unknown-elf-objdump $buildDir/a.out -d").!!)
  println(Process(s"$compilerPath/$architecture-unknown-elf-objcopy $buildDir/a.out --dump-section .text=$buildDir/a.bin").!!)
  println(loadProgram(s"$buildDir/a.bin").wordBinaries.map(_.toString(16)).mkString("\n"))
*/
}
