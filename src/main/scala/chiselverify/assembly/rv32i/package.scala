package chiselverify.assembly
import chiselverify.assembly.RandomHelpers.{BigRange, pow2, randSplit, randomSelect}

import scala.collection.immutable
import scala.reflect.runtime.universe.{TypeRef, typeOf}

package object rv32i {

  object IntegerRegister {
    object zero extends Register
    object ra extends Register
    object sp extends Register
    object gp extends Register
    object tp extends Register
    object t0 extends Register
    object t1 extends Register
    object t2 extends Register
    object s0 extends Register
    object s1 extends Register
    object a0 extends Register
    object a1 extends Register
    object a2 extends Register
    object a3 extends Register
    object a4 extends Register
    object a5 extends Register
    object a6 extends Register
    object a7 extends Register
    object s2 extends Register
    object s3 extends Register
    object s4 extends Register
    object s5 extends Register
    object s6 extends Register
    object s7 extends Register
    object s8 extends Register
    object s9 extends Register
    object s10 extends Register
    object s11 extends Register
    object t3 extends Register
    object t4 extends Register
    object t5 extends Register
    object t6 extends Register
  }

  import IntegerRegister._
  object IntegerRegisterFile extends RegisterFile(
    zero,ra,sp,gp,tp,t0,t1,t2,a0,a1,a2,a3,a4,a5,a6,a7,s2,s3,s4,s5,s6,s7,s8,s9,s10,s11,t3,t4,t5,t6
  )


  object RV32I extends InstructionSet {

    override val memoryAddressSpace: BigRange = BigRange(0, pow2(32))
    override val inputOutputAddressSpace: BigRange = BigRange(0, 0)

    val load = Pattern(implicit c => {
      val (base,offset) = randSplit(c.nextMemoryAddress(Seq()).toInt)(Unsigned(32),Signed(12))
      val reg = randomSelect(rv32i.IntegerRegisterFile.registers)
      Seq(
        LI(reg,base), LW(None,reg,offset)
      )
    })

    override val instructions = Seq(ADDI(),load.giveCategory(Category.Load),LI())



    case class ADDI (
                      rdIn: Option[Register] = None,
                      rs1In: Option[Register] = None,
                      immIn: Option[BigInt] = None
                    ) extends Instruction(Category.Arithmetic) {

      val rd = Register(IntegerRegisterFile)(rdIn)
      val rs1 = Register(IntegerRegisterFile)(rs1In)
      val imm = Constant(Signed(12))(immIn)

      override def apply(): Instruction = copy()
      override def toAsm: String = s"addi x$rd, x$rs1, $imm"
    }

    case class LW (
                    rdIn: Option[Register] = None,
                    rs1In: Option[Register] = None,
                    immIn: Option[BigInt] = None
                  ) extends Instruction(Category.Load) {
      val rd = Register(IntegerRegisterFile)(rdIn)
      val rs1 = Register(IntegerRegisterFile)(rs1In)
      val imm = Constant(Signed(12))(immIn)
      override def apply(): Instruction = copy()
      override def toAsm: String = s"lw x$rd, $imm(x$rs1)"
    }

    case class LI (
                    rdIn: Option[Register] = None,
                    immIn: Option[BigInt] = None
                  ) extends Instruction(Category.Load) {
      val rd = Register(IntegerRegisterFile)(rdIn)
      val imm = Constant(Unsigned(32))(immIn)
      override def apply(): Instruction = copy()
      override def toAsm: String = s"li x$rd, $imm"
    }

  }

}
