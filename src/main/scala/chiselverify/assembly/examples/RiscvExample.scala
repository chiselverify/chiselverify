package chiselverify.assembly.examples

import chiselverify.assembly.Label.LabelRecord
import chiselverify.assembly.RandomHelpers.{BigRange}
import chiselverify.assembly.rv32i.IntegerRegister.{t1, t2}
import chiselverify.assembly.rv32i.RV32I
import chiselverify.assembly.rv32i.RV32I.{ADDI, BLT, LI, load}
import chiselverify.assembly.{Category, CategoryBlackList, CategoryDistribution, Instruction, Label, MemoryDistribution, Pattern, ProgramGenerator, someToOption, intToBigIntOption}



object RiscvExample extends App {


  val pattern = Pattern(implicit c => Seq(
    Instruction.ofCategory(Category.Arithmetic), Instruction.fill(4), LI(), Instruction.ofCategory(Category.Load), load
  ))

  val forLoop = Pattern(implicit c => Seq(
      LI(t1,0),
      LI(t2),
      Label("ForLoopStart"),
      Instruction.fillWithCategory(10)(Category.Arithmetic),
      ADDI(t1,t1,1),
      BLT(t1,t2,LabelRecord("ForLoopStart")),
      Instruction.fill(10)
    ))


  val pg = ProgramGenerator(RV32I)(
    CategoryDistribution(
      Category.Arithmetic -> 0.9,
      Category.Load -> 0.1
    ),
    MemoryDistribution(
      BigRange(0x100, 0x1000) -> 0.7,
      BigRange(0xFFFF) -> 0.3
    )
  )

  println(Seq.fill(2)(pg.generate(pattern).pretty).mkString("\n"))


  println(pg.generate(Pattern(implicit c => Seq(Pattern.repeat(10)(load)))).pretty)

  println(pg.generate(forLoop).pretty)

  println(ProgramGenerator(RV32I)(
    CategoryBlackList(Category.Synchronization,Category.StateRegister,Category.EnvironmentCall,Category.JumpAndLink)
  ).generate(400).pretty)


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
