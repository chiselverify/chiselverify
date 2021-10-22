package chiselverify.assembly

import chiselverify.assembly.RandomHelpers.{BigRange, pow2}

package object patmos {

  object GeneralPurposeRegister {
    object r0 extends Register
    object r1 extends Register
    object r2 extends Register
    object r3 extends Register
    object r4 extends Register
    object r5 extends Register
    object r6 extends Register
    object r7 extends Register
    object r8 extends Register
    object r9 extends Register
    object r10 extends Register
    object r11 extends Register
    object r12 extends Register
    object r13 extends Register
    object r14 extends Register
    object r15 extends Register
    object r16 extends Register
    object r17 extends Register
    object r18 extends Register
    object r19 extends Register
    object r20 extends Register
    object r21 extends Register
    object r22 extends Register
    object r23 extends Register
    object r24 extends Register
    object r25 extends Register
    object r26 extends Register
    object r27 extends Register
    object r28 extends Register
    object r29 extends Register
    object r30 extends Register
    object r31 extends Register
  }
  import GeneralPurposeRegister._
  object GeneralPurposeRegisterFile extends RegisterFile(
    r0,r1,r2,r3,r4,r5,r6,r7,r8,r9,r10,r11,r12,r13,r14,r15,r16,r17,r17,r18,r19,r20,r21,r22,r23,r24,r25,r26,r27,r28,r29,r30,r31
  )

  object PredicateRegister {
    object p0 extends Register
    object p1 extends Register
    object p2 extends Register
    object p3 extends Register
    object p4 extends Register
    object p5 extends Register
    object p6 extends Register
    object p7 extends Register
  }
  import PredicateRegister._
  object PredicateRegisterFile extends RegisterFile(
    p0,p1,p2,p3,p4,p5,p6,p7
  )

  object SpecialRegister {
    object s0 extends Register
    object s1 extends Register
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
    object s12 extends Register
    object s13 extends Register
    object s14 extends Register
    object s15 extends Register
  }
  import SpecialRegister._
  object SpecialRegisterFile extends RegisterFile(
    s0,s1,s2,s3,s4,s5,s6,s7,s8,s9,s10,s11,s12,s13,s14,s15
  )

  object OpenBundleBracket extends Instruction() {
    override def apply(): Instruction = OpenBundleBracket
    override def toAsm: String = "{"
  }

  object CloseBundleBracket extends Instruction() {
    override def apply(): Instruction = CloseBundleBracket
    override def toAsm: String = "}"
  }

  object Bundle {
    def apply(i1: Instruction, i2: Instruction): InstructionFactory = {
      Pattern(implicit c => Seq(OpenBundleBracket,i1,i2,CloseBundleBracket))
    }
  }


  object Patmos extends InstructionSet {
    override val instructions = Seq(ADD())


    case class ADD (
                   rdIn: Option[Register] = None,
                   rs1In: Option[Register] = None,
                   rs2In: Option[Register] = None,
                   predIn: Option[Register] = None
                   ) extends Instruction(Category.Arithmetic) {
      val rd = Register(GeneralPurposeRegisterFile)(rdIn)
      val rs1 = Register(GeneralPurposeRegisterFile)(rs1In)
      val rs2 = Register(GeneralPurposeRegisterFile)(rs2In)
      val pred = Register(PredicateRegisterFile)(predIn)

      override def apply(): Instruction = copy()

      override def toAsm: String = s"($$p$pred) add $$r$rd = $$r$rs1, $$r$rs2"
    }

    override val memoryAddressSpace: BigRange = BigRange(0, pow2(32))
    override val inputOutputAddressSpace: BigRange = BigRange(0, 0)
  }

}
