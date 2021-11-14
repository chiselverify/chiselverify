package verifyTests.assembly

import chiselverify.assembly.RandomHelpers.BigRange
import chiselverify.assembly.{IODistribution, MemoryDistribution, ProgramGenerator}
import chiselverify.assembly.leros.Leros
import org.scalatest.FlatSpec
import verifyTests.assembly.ProgramGeneratorTest.expectedProgramString

class ProgramGeneratorTest extends FlatSpec {
  behavior of "Program generator"

  it should "generate the correct program" in {

    val pg = ProgramGenerator(Leros)(
      MemoryDistribution(
        BigRange(0, 0x1000) -> 1,
        BigRange(0x7FFF0,0xFFFFF) -> 1
      ),
      IODistribution(
        BigRange(0, 0xF) -> 99,
        BigRange(0xFF) -> 1
      )
    )

    val prog = pg.generate(300,0xdeadbeef)

    println(prog)

    prog.toString.split("\n")
      .zip(expectedProgramString.split("\n"))
      .zipWithIndex
      .foreach { case ((got,should),i) =>
        assert(got == should, s"$should was $got at line $i")
    }


  }

}


object ProgramGeneratorTest {
  val expectedProgramString = """  load <RANDOM_LABEL_0
                                |  loadh >RANDOM_LABEL_0
                                |  jal r89
                                |  or r18
                                |  subi 1
                                |  subi 0
                                |  xor r137
                                |  loadi 250
                                |  loadhi 2
                                |  loadh2i 0
                                |  loadh3i 0
                                |  ldaddr
                                |  ldind 87
                                |RANDOM_LABEL_0:
                                |  and r98
                                |  and r66
                                |  addi 1
                                |  subi 0
                                |  load <RANDOM_LABEL_0
                                |  loadh >RANDOM_LABEL_0
                                |  jal r99
                                |  ori 1
                                |  loadh3i 0
                                |  sub r69
                                |  loadh3i 1
                                |  xori 0
                                |  addi 1
                                |  xori 0
                                |  addi 1
                                |  in 0
                                |  sra r175
                                |  addi 0
                                |  loadh3i 1
                                |  loadh2i 1
                                |  scall 0
                                |  add r15
                                |  loadi 246
                                |  loadhi 255
                                |  loadh2i 7
                                |  loadh3i 0
                                |  ldaddr
                                |  stind 5
                                |  andi 0
                                |  loadi 1
                                |  ori 1
                                |  load r34
                                |  ori 1
                                |  brp RANDOM_LABEL_0
                                |  load <RANDOM_LABEL_0
                                |  loadh >RANDOM_LABEL_0
                                |  jal r93
                                |  loadhi 0
                                |  loadh2i 1
                                |RANDOM_LABEL_1:
                                |  loadi 1
                                |  ldaddr
                                |  loadhi 1
                                |  load <RANDOM_LABEL_0
                                |  loadh >RANDOM_LABEL_0
                                |  jal r201
                                |  in 0
                                |  scall 0
                                |  ldaddr
                                |  loadi 204
                                |  loadhi 255
                                |  loadh2i 7
                                |  loadh3i 0
                                |  ldaddr
                                |  stind 36
                                |  out 0
                                |  store r184
                                |RANDOM_LABEL_2:
                                |  addi 0
                                |  addi 1
                                |  out 0
                                |  loadi 128
                                |  loadhi 255
                                |  loadh2i 7
                                |  loadh3i 0
                                |  ldaddr
                                |  stind 114
                                |  loadi 0
                                |  xori 0
                                |  loadi 0
                                |  loadh3i 1
                                |  loadh3i 1
                                |  brp RANDOM_LABEL_1
                                |  sra r196
                                |  scall 1
                                |  loadi 25
                                |  loadhi 1
                                |  loadh2i 0
                                |  loadh3i 0
                                |  ldaddr
                                |  ldindbu 71
                                |  sub r121
                                |  scall 0
                                |  subi 0
                                |RANDOM_LABEL_3:
                                |  store r113
                                |  addi 1
                                |  add r121
                                |  add r35
                                |  addi 0
                                |  ori 1
                                |  xor r148
                                |  addi 0
                                |  store r46
                                |  sra r234
                                |  in 1
                                |  and r31
                                |  sub r27
                                |  subi 1
                                |  andi 1
                                |  brz RANDOM_LABEL_0
                                |  xori 0
                                |  load r254
                                |  add r82
                                |  load <RANDOM_LABEL_0
                                |  loadh >RANDOM_LABEL_0
                                |  jal r238
                                |  in 1
                                |  brz RANDOM_LABEL_1
                                |  loadi 0
                                |  loadhi 1
                                |  sub r42
                                |  loadi 202
                                |  loadhi 10
                                |  loadh2i 0
                                |  loadh3i 0
                                |  ldaddr
                                |  ldind 116
                                |  sra r95
                                |  loadh3i 0
                                |  subi 1
                                |  brp RANDOM_LABEL_0
                                |RANDOM_LABEL_4:
                                |  load r92
                                |  loadi 134
                                |  loadhi 255
                                |  loadh2i 7
                                |  loadh3i 0
                                |  ldaddr
                                |  ldind 114
                                |  loadi 200
                                |  loadhi 255
                                |  loadh2i 7
                                |  loadh3i 0
                                |  ldaddr
                                |  ldind 50
                                |  load <RANDOM_LABEL_0
                                |  loadh >RANDOM_LABEL_0
                                |  jal r205
                                |  loadh3i 0
                                |  in 0
                                |  store r48
                                |  or r210
                                |  loadh2i 0
                                |  xori 0
                                |  store r180
                                |  subi 1
                                |  in 1
                                |  subi 1
                                |  and r153
                                |  loadh3i 0
                                |RANDOM_LABEL_5:
                                |  sra r118
                                |RANDOM_LABEL_6:
                                |  and r62
                                |  add r104
                                |  ori 0
                                |  in 1
                                |  loadh3i 0
                                |RANDOM_LABEL_7:
                                |  xor r66
                                |  loadh3i 0
                                |  add r98
                                |  and r52
                                |  loadi 210
                                |  loadhi 255
                                |  loadh2i 7
                                |  loadh3i 0
                                |  ldaddr
                                |  stind 36
                                |  load r179
                                |  ori 1
                                |  loadhi 0
                                |  loadi 6
                                |  loadhi 15
                                |  loadh2i 0
                                |  loadh3i 0
                                |  ldaddr
                                |  ldind 85
                                |  loadi 163
                                |  loadhi 3
                                |  loadh2i 0
                                |  loadh3i 0
                                |  ldaddr
                                |  ldindbu 83
                                |  ori 1
                                |  andi 1
                                |  loadi 202
                                |  loadhi 255
                                |  loadh2i 7
                                |  loadh3i 0
                                |  ldaddr
                                |  ldind 39
                                |  loadi 201
                                |  loadhi 255
                                |  loadh2i 7
                                |  loadh3i 0
                                |  ldaddr
                                |  ldind 51
                                |  in 0
                                |  br RANDOM_LABEL_3
                                |  loadi 6
                                |  loadhi 4
                                |  loadh2i 0
                                |  loadh3i 0
                                |  ldaddr
                                |  ldindbu 34
                                |  or r219
                                |  load <RANDOM_LABEL_3
                                |  loadh >RANDOM_LABEL_3
                                |  jal r180
                                |  loadhi 1
                                |  load <RANDOM_LABEL_7
                                |  loadh >RANDOM_LABEL_7
                                |  jal r127
                                |  brz RANDOM_LABEL_2
                                |  loadh2i 1
                                |  scall 0
                                |  loadi 39
                                |  loadhi 5
                                |  loadh2i 0
                                |  loadh3i 0
                                |  ldaddr
                                |  ldindbu 61
                                |  scall 0
                                |  andi 0
                                |  and r112
                                |  addi 0
                                |  loadi 0
                                |  in 1
                                |  loadh2i 1
                                |  or r20
                                |  sra r94
                                |  scall 1
                                |RANDOM_LABEL_8:
                                |  ldaddr
                                |  br RANDOM_LABEL_8
                                |  add r7
                                |  loadh3i 1
                                |  andi 0
                                |  xor r234
                                |  or r120
                                |  scall 0
                                |  loadh2i 0
                                |  loadi 0
                                |  brp RANDOM_LABEL_5
                                |  andi 0
                                |  sra r143
                                |  sub r158
                                |  ori 1
                                |  sra r32
                                |  subi 0
                                |  andi 1
                                |  subi 0
                                |  sub r30
                                |RANDOM_LABEL_9:
                                |  load r42
                                |  addi 0
                                |  add r99
                                |  or r246
                                |  or r136
                                |  add r87
                                |  brz RANDOM_LABEL_1
                                |  out 0
                                |  loadhi 1
                                |  load <RANDOM_LABEL_5
                                |  loadh >RANDOM_LABEL_5
                                |  jal r12
                                |  or r117
                                |  scall 1
                                |  loadi 169
                                |  loadhi 255
                                |  loadh2i 7
                                |  loadh3i 0
                                |  ldaddr
                                |  stindb 79
                                |  out 1
                                |  out 1
                                |RANDOM_LABEL_10:
                                |  loadi 59
                                |  loadhi 9
                                |  loadh2i 0
                                |  loadh3i 0
                                |  ldaddr
                                |  ldindbu 5
                                |  xor r101
                                |  loadi 127
                                |  loadhi 255
                                |  loadh2i 7
                                |  loadh3i 0
                                |  ldaddr
                                |  stindb 114
                                |  sub r91
                                |  ldaddr
                                |  loadi 194
                                |  loadhi 7
                                |  loadh2i 0
                                |  loadh3i 0
                                |  ldaddr
                                |  ldind 78
                                |  out 1
                                |  sub r20
                                |  loadh2i 1
                                |  loadhi 0
                                |  loadh3i 0
                                |  in 0
                                |  loadh3i 1
                                |  and r7
                                |  loadi 0
                                |  ori 0
                                |  sub r230
                                |  load r145
                                |  xori 0
                                |  scall 1
                                |  sra r47
                                |  or r81
                                |  loadh2i 0
                                |  loadi 69
                                |  loadhi 14
                                |  loadh2i 0
                                |  loadh3i 0
                                |  ldaddr
                                |  stind 3
                                |  scall 1
                                |  or r29
                                |  loadh2i 1
                                |  loadi 161
                                |  loadhi 255
                                |  loadh2i 7
                                |  loadh3i 0
                                |  ldaddr
                                |  ldindbu 82
                                |  store r155
                                |  loadi 121
                                |  loadhi 255
                                |  loadh2i 7
                                |  loadh3i 0
                                |  ldaddr
                                |  ldind 125
                                |  out 0
                                |  loadh3i 1
                                |  sub r68
                                |  subi 0
                                |  loadi 175
                                |  loadhi 12
                                |  loadh2i 0
                                |  loadh3i 0
                                |  ldaddr
                                |  stind 68
                                |  scall 0
                                |  brp RANDOM_LABEL_0
                                |  out 0
                                |  store r18
                                |  store r162
                                |  xori 1
                                |  loadi 0
                                |  loadhi 1
                                |  xor r154
                                |  scall 1
                                |  ori 1
                                |  ldaddr
                                |  loadi 116
                                |  loadhi 15
                                |  loadh2i 0
                                |  loadh3i 0
                                |  ldaddr
                                |  ldindbu 66
                                |RANDOM_LABEL_11:
                                |  loadi 135
                                |  loadhi 1
                                |  loadh2i 0
                                |  loadh3i 0
                                |  ldaddr
                                |  ldindbu 78
                                |  xor r237
                                |  loadi 1
                                |  and r122
                                |  xori 1
                                |  sub r240
                                |  load r42
                                |  sra r26
                                |  store r244
                                |  loadi 221
                                |  loadhi 255
                                |  loadh2i 7
                                |  loadh3i 0
                                |  ldaddr
                                |  stindb 18
                                |  xor r14
                                |  ori 1
                                |  load <RANDOM_LABEL_3
                                |  loadh >RANDOM_LABEL_3
                                |  jal r254
                                |  store r190
                                |  ldaddr
                                |  store r150
                                |  andi 0
                                |  ldaddr
                                |  sub r135
                                |  load r71
                                |  loadi 193
                                |  loadhi 14
                                |  loadh2i 0
                                |  loadh3i 0
                                |  ldaddr
                                |  stind 126
                                |  load r197
                                |RANDOM_LABEL_12:
                                |  and r37
                                |  xori 0
                                |RANDOM_LABEL_13:
                                |  loadh3i 1
                                |  loadi 143
                                |  loadhi 3
                                |  loadh2i 0
                                |  loadh3i 0
                                |  ldaddr
                                |  ldindbu 96
                                |  subi 0
                                |  loadi 1
                                |  load <RANDOM_LABEL_11
                                |  loadh >RANDOM_LABEL_11
                                |  jal r102
                                |  add r234
                                |  loadh3i 0
                                |  xor r249
                                |  loadi 1
                                |  and r216
                                |  loadh3i 0
                                |  andi 0
                                |  scall 1
                                |  xori 1
                                |  loadh2i 0
                                |  br RANDOM_LABEL_13
                                |  xor r251
                                |  out 0
                                |  loadh2i 0
                                |  load <RANDOM_LABEL_6
                                |  loadh >RANDOM_LABEL_6
                                |  jal r22
                                |  ldaddr
                                |  loadh2i 0
                                |  loadh2i 0
                                |  loadh3i 0
                                |RANDOM_LABEL_14:
                                |  loadi 1
                                |  loadi 1
                                |  load r36
                                |  store r136
                                |  out 1""".stripMargin
}