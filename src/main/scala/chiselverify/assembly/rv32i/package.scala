package chiselverify.assembly
import chiselverify.assembly.Label.LabelRecord
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

    // riscv has full 32-bit addressing
    override val memoryAddressSpace: BigRange = BigRange(0, pow2(32))
    // riscv has no dedicated IO port space
    override val inputOutputAddressSpace: BigRange = BigRange(0, 0)

    // the load pattern generates a new address, splits it, sets a register to the base address and the executes the load
    val load = Pattern(Category.Load)(implicit c => {
      val (base,offset) = randSplit(c.nextMemoryAddress(Seq()))(Unsigned(32),Signed(12))
      val reg = randomSelect(rv32i.IntegerRegisterFile.registers)
      Seq(
        LI(reg,base.toInt),
        Instruction.select(
          LB(None,reg,offset),
          LBU(None,reg,offset),
          LH(None,reg,offset),
          LHU(None,reg,offset),
          LW(None,reg,offset)
        )
      )
    })

    // the store pattern sets a register to a random address and executes the store with offset
    val store = Pattern(Category.Store)(implicit c => {
      val (base,offset) = randSplit(c.nextMemoryAddress(Seq()))(Unsigned(32),Signed(12))
      val reg = randomSelect(rv32i.IntegerRegisterFile.registers)
      Seq(
        LI(reg,base.toInt),
        Instruction.select(
          SB(reg,None,offset),
          SH(reg,None,offset),
          SW(reg,None,offset)
        )
      )
    })

    // the jump and link pattern requests a jump target, sets the accumulator to the target address and executes the jump
    val jumpAndLink = Pattern(Category.JumpAndLink)(implicit c => {
      val target = c.nextJumpTarget()
      val reg = randomSelect(rv32i.IntegerRegisterFile.registers)
      Seq(
        LA(reg,target),
        Instruction.select(JAL(None,target),JALR(None,reg,0))
      )
    })

    // branches rely on the generator context to deliver a target label
    val branch = Pattern(Category.Branch)(implicit c => {
      val target = c.nextJumpTarget()
      Seq(
        Instruction.select(
          BEQ(None,None,target),
          BGE(None,None,target),
          BGEU(None,None,target),
          BLT(None,None,target),
          BLTU(None,None,target),
          BNE(None,None,target)
        )
      )
    })

    // all instructions
    override val instructions = Seq(
      ADD(),
      ADDI(),
      AND(),
      ANDI(),
      AUIPC(),
      CSRRC(),
      CSRRCI(),
      CSRRS(),
      CSRRSI(),
      CSRRW(),
      CSRRWI(),
      EBREAK(),
      ECALL(),
      FENCE(),
      FENCE_I(),
      OR(),
      ORI(),
      SLL(),
      SLLI(),
      SLT(),
      SLTI(),
      SLTIU(),
      SLTU(),
      SRA(),
      SRAI(),
      SRL(),
      SRLI(),
      SUB(),
      XOR(),
      XORI(),
      load,
      store,
      jumpAndLink,
      branch
    )

    case class ADD (
                     rdIn: Option[Register] = None,
                     rs1In: Option[Register] = None,
                     rs2In: Option[Register] = None
                   ) extends Instruction(Category.Arithmetic) {

      val rd = Register(IntegerRegisterFile)(rdIn)
      val rs1 = Register(IntegerRegisterFile)(rs1In)
      val rs2 = Register(IntegerRegisterFile)(rs2In)

      override def apply(): Instruction = copy()
      override def toAsm: String = s"add x$rd, x$rs1, x$rs2"
    }

    case class AND (
                     rdIn: Option[Register] = None,
                     rs1In: Option[Register] = None,
                     rs2In: Option[Register] = None
                   ) extends Instruction(Category.Logical) {

      val rd = Register(IntegerRegisterFile)(rdIn)
      val rs1 = Register(IntegerRegisterFile)(rs1In)
      val rs2 = Register(IntegerRegisterFile)(rs2In)

      override def apply(): Instruction = copy()
      override def toAsm: String = s"and x$rd, x$rs1, x$rs2"
    }

    case class OR (
                    rdIn: Option[Register] = None,
                    rs1In: Option[Register] = None,
                    rs2In: Option[Register] = None
                  ) extends Instruction(Category.Logical) {

      val rd = Register(IntegerRegisterFile)(rdIn)
      val rs1 = Register(IntegerRegisterFile)(rs1In)
      val rs2 = Register(IntegerRegisterFile)(rs2In)

      override def apply(): Instruction = copy()
      override def toAsm: String = s"or x$rd, x$rs1, x$rs2"
    }

    case class SLL (
                     rdIn: Option[Register] = None,
                     rs1In: Option[Register] = None,
                     rs2In: Option[Register] = None
                   ) extends Instruction(Category.Logical) {

      val rd = Register(IntegerRegisterFile)(rdIn)
      val rs1 = Register(IntegerRegisterFile)(rs1In)
      val rs2 = Register(IntegerRegisterFile)(rs2In)

      override def apply(): Instruction = copy()
      override def toAsm: String = s"sll x$rd, x$rs1, x$rs2"
    }

    case class SLT (
                     rdIn: Option[Register] = None,
                     rs1In: Option[Register] = None,
                     rs2In: Option[Register] = None
                   ) extends Instruction(Category.Compare) {

      val rd = Register(IntegerRegisterFile)(rdIn)
      val rs1 = Register(IntegerRegisterFile)(rs1In)
      val rs2 = Register(IntegerRegisterFile)(rs2In)

      override def apply(): Instruction = copy()
      override def toAsm: String = s"slt x$rd, x$rs1, x$rs2"
    }

    case class SLTU (
                      rdIn: Option[Register] = None,
                      rs1In: Option[Register] = None,
                      rs2In: Option[Register] = None
                    ) extends Instruction(Category.Compare) {

      val rd = Register(IntegerRegisterFile)(rdIn)
      val rs1 = Register(IntegerRegisterFile)(rs1In)
      val rs2 = Register(IntegerRegisterFile)(rs2In)

      override def apply(): Instruction = copy()
      override def toAsm: String = s"sltu x$rd, x$rs1, x$rs2"
    }

    case class SRA (
                     rdIn: Option[Register] = None,
                     rs1In: Option[Register] = None,
                     rs2In: Option[Register] = None
                   ) extends Instruction(Category.Arithmetic) {

      val rd = Register(IntegerRegisterFile)(rdIn)
      val rs1 = Register(IntegerRegisterFile)(rs1In)
      val rs2 = Register(IntegerRegisterFile)(rs2In)

      override def apply(): Instruction = copy()
      override def toAsm: String = s"sra x$rd, x$rs1, x$rs2"
    }

    case class SRL (
                     rdIn: Option[Register] = None,
                     rs1In: Option[Register] = None,
                     rs2In: Option[Register] = None
                   ) extends Instruction(Category.Logical) {

      val rd = Register(IntegerRegisterFile)(rdIn)
      val rs1 = Register(IntegerRegisterFile)(rs1In)
      val rs2 = Register(IntegerRegisterFile)(rs2In)

      override def apply(): Instruction = copy()
      override def toAsm: String = s"srl x$rd, x$rs1, x$rs2"
    }

    case class SUB (
                     rdIn: Option[Register] = None,
                     rs1In: Option[Register] = None,
                     rs2In: Option[Register] = None
                   ) extends Instruction(Category.Arithmetic) {

      val rd = Register(IntegerRegisterFile)(rdIn)
      val rs1 = Register(IntegerRegisterFile)(rs1In)
      val rs2 = Register(IntegerRegisterFile)(rs2In)

      override def apply(): Instruction = copy()
      override def toAsm: String = s"sub x$rd, x$rs1, x$rs2"
    }

    case class XOR (
                     rdIn: Option[Register] = None,
                     rs1In: Option[Register] = None,
                     rs2In: Option[Register] = None
                   ) extends Instruction(Category.Logical) {

      val rd = Register(IntegerRegisterFile)(rdIn)
      val rs1 = Register(IntegerRegisterFile)(rs1In)
      val rs2 = Register(IntegerRegisterFile)(rs2In)

      override def apply(): Instruction = copy()
      override def toAsm: String = s"xor x$rd, x$rs1, x$rs2"
    }

    case class ADDI (
                      rdIn: Option[Register] = None,
                      rs1In: Option[Register] = None,
                      immIn: Option[BigInt] = None
                    ) extends Instruction(Category.Arithmetic,Category.Immediate) {

      val rd = Register(IntegerRegisterFile)(rdIn)
      val rs1 = Register(IntegerRegisterFile)(rs1In)
      val imm = Constant(Signed(12))(immIn)

      override def apply(): Instruction = copy()
      override def toAsm: String = s"addi x$rd, x$rs1, $imm"
    }

    case class ANDI (
                      rdIn: Option[Register] = None,
                      rs1In: Option[Register] = None,
                      immIn: Option[BigInt] = None
                    ) extends Instruction(Category.Logical,Category.Immediate) {

      val rd = Register(IntegerRegisterFile)(rdIn)
      val rs1 = Register(IntegerRegisterFile)(rs1In)
      val imm = Constant(Signed(12))(immIn)

      override def apply(): Instruction = copy()
      override def toAsm: String = s"andi x$rd, x$rs1, $imm"
    }

    case class CSRRC (
                       rdIn: Option[Register] = None,
                       rs1In: Option[Register] = None,
                       immIn: Option[BigInt] = None
                     ) extends Instruction(Category.StateRegister,Category.Immediate) {

      val rd = Register(IntegerRegisterFile)(rdIn)
      val rs1 = Register(IntegerRegisterFile)(rs1In)
      val imm = Constant(Signed(12))(immIn)

      override def apply(): Instruction = copy()
      override def toAsm: String = s"csrrc x$rd, x$rs1, $imm"
    }

    case class CSRRCI (
                        rdIn: Option[Register] = None,
                        rs1In: Option[Register] = None,
                        immIn: Option[BigInt] = None
                      ) extends Instruction(Category.StateRegister,Category.Immediate) {

      val rd = Register(IntegerRegisterFile)(rdIn)
      val rs1 = Register(IntegerRegisterFile)(rs1In)
      val imm = Constant(Signed(12))(immIn)

      override def apply(): Instruction = copy()
      override def toAsm: String = s"csrrci x$rd, x$rs1, $imm"
    }

    case class CSRRS (
                       rdIn: Option[Register] = None,
                       rs1In: Option[Register] = None,
                       immIn: Option[BigInt] = None
                     ) extends Instruction(Category.StateRegister,Category.Immediate) {

      val rd = Register(IntegerRegisterFile)(rdIn)
      val rs1 = Register(IntegerRegisterFile)(rs1In)
      val imm = Constant(Signed(12))(immIn)

      override def apply(): Instruction = copy()
      override def toAsm: String = s"csrrs x$rd, x$rs1, $imm"
    }

    case class CSRRSI (
                        rdIn: Option[Register] = None,
                        rs1In: Option[Register] = None,
                        immIn: Option[BigInt] = None
                      ) extends Instruction(Category.StateRegister,Category.Immediate) {

      val rd = Register(IntegerRegisterFile)(rdIn)
      val rs1 = Register(IntegerRegisterFile)(rs1In)
      val imm = Constant(Signed(12))(immIn)

      override def apply(): Instruction = copy()
      override def toAsm: String = s"csrrsi x$rd, x$rs1, $imm"
    }

    case class CSRRW (
                       rdIn: Option[Register] = None,
                       rs1In: Option[Register] = None,
                       immIn: Option[BigInt] = None
                     ) extends Instruction(Category.StateRegister,Category.Immediate) {

      val rd = Register(IntegerRegisterFile)(rdIn)
      val rs1 = Register(IntegerRegisterFile)(rs1In)
      val imm = Constant(Signed(12))(immIn)

      override def apply(): Instruction = copy()
      override def toAsm: String = s"csrrw x$rd, x$rs1, $imm"
    }

    case class CSRRWI (
                        rdIn: Option[Register] = None,
                        rs1In: Option[Register] = None,
                        immIn: Option[BigInt] = None
                      ) extends Instruction(Category.StateRegister,Category.Immediate) {

      val rd = Register(IntegerRegisterFile)(rdIn)
      val rs1 = Register(IntegerRegisterFile)(rs1In)
      val imm = Constant(Signed(12))(immIn)

      override def apply(): Instruction = copy()
      override def toAsm: String = s"csrrwi x$rd, x$rs1, $imm"
    }

    case class EBREAK() extends Instruction(Category.EnvironmentCall) {
      override def apply(): Instruction = EBREAK()
      override def toAsm: String = s"ebreak"
    }

    case class ECALL() extends Instruction(Category.EnvironmentCall) {
      override def apply(): Instruction = ECALL()
      override def toAsm: String = s"ecall"
    }

    case class FENCE() extends Instruction(Category.Synchronization) {
      override def apply(): Instruction = FENCE()
      override def toAsm: String = s"fence"
    }

    case class FENCE_I() extends Instruction(Category.Synchronization) {
      override def apply(): Instruction = FENCE_I()
      override def toAsm: String = s"fence.i"
    }

    case class JALR (
                      rdIn: Option[Register] = None,
                      rs1In: Option[Register] = None,
                      immIn: Option[BigInt] = None
                    ) extends Instruction(Category.JumpAndLink,Category.Immediate) {

      val rd = Register(IntegerRegisterFile)(rdIn)
      val rs1 = Register(IntegerRegisterFile)(rs1In)
      val imm = Constant(Signed(12))(immIn)

      override def apply(): Instruction = copy()
      override def toAsm: String = s"jalr x$rd, x$rs1, $imm"
    }

    case class LB (
                    rdIn: Option[Register] = None,
                    rs1In: Option[Register] = None,
                    immIn: Option[BigInt] = None
                  ) extends Instruction(Category.Load,Category.Immediate) {

      val rd = Register(IntegerRegisterFile)(rdIn)
      val rs1 = Register(IntegerRegisterFile)(rs1In)
      val imm = Constant(Signed(12))(immIn)

      override def apply(): Instruction = copy()
      override def toAsm: String = s"lb x$rd, $imm(x$rs1)"
    }

    case class LBU (
                     rdIn: Option[Register] = None,
                     rs1In: Option[Register] = None,
                     immIn: Option[BigInt] = None
                   ) extends Instruction(Category.Load,Category.Immediate) {

      val rd = Register(IntegerRegisterFile)(rdIn)
      val rs1 = Register(IntegerRegisterFile)(rs1In)
      val imm = Constant(Signed(12))(immIn)

      override def apply(): Instruction = copy()
      override def toAsm: String = s"lbu x$rd, $imm(x$rs1)"
    }

    case class LH (
                    rdIn: Option[Register] = None,
                    rs1In: Option[Register] = None,
                    immIn: Option[BigInt] = None
                  ) extends Instruction(Category.Load,Category.Immediate) {

      val rd = Register(IntegerRegisterFile)(rdIn)
      val rs1 = Register(IntegerRegisterFile)(rs1In)
      val imm = Constant(Signed(12))(immIn)

      override def apply(): Instruction = copy()
      override def toAsm: String = s"lh x$rd, $imm(x$rs1)"
    }

    case class LHU (
                     rdIn: Option[Register] = None,
                     rs1In: Option[Register] = None,
                     immIn: Option[BigInt] = None
                   ) extends Instruction(Category.Load,Category.Immediate) {

      val rd = Register(IntegerRegisterFile)(rdIn)
      val rs1 = Register(IntegerRegisterFile)(rs1In)
      val imm = Constant(Signed(12))(immIn)

      override def apply(): Instruction = copy()
      override def toAsm: String = s"lhu x$rd, $imm(x$rs1)"
    }

    case class LW (
                    rdIn: Option[Register] = None,
                    rs1In: Option[Register] = None,
                    immIn: Option[BigInt] = None
                  ) extends Instruction(Category.Load,Category.Immediate) {
      val rd = Register(IntegerRegisterFile)(rdIn)
      val rs1 = Register(IntegerRegisterFile)(rs1In)
      val imm = Constant(Signed(12))(immIn)
      override def apply(): Instruction = copy()
      override def toAsm: String = s"lw x$rd, $imm(x$rs1)"
    }

    case class ORI (
                     rdIn: Option[Register] = None,
                     rs1In: Option[Register] = None,
                     immIn: Option[BigInt] = None
                   ) extends Instruction(Category.Logical,Category.Immediate) {

      val rd = Register(IntegerRegisterFile)(rdIn)
      val rs1 = Register(IntegerRegisterFile)(rs1In)
      val imm = Constant(Signed(12))(immIn)

      override def apply(): Instruction = copy()
      override def toAsm: String = s"ori x$rd, x$rs1, $imm"
    }

    case class SLLI (
                      rdIn: Option[Register] = None,
                      rs1In: Option[Register] = None,
                      immIn: Option[BigInt] = None
                    ) extends Instruction(Category.Logical,Category.Immediate) {

      val rd = Register(IntegerRegisterFile)(rdIn)
      val rs1 = Register(IntegerRegisterFile)(rs1In)
      val imm = Constant(Unsigned(5))(immIn)

      override def apply(): Instruction = copy()
      override def toAsm: String = s"slli x$rd, x$rs1, $imm"
    }

    case class SLTI (
                      rdIn: Option[Register] = None,
                      rs1In: Option[Register] = None,
                      immIn: Option[BigInt] = None
                    ) extends Instruction(Category.Compare,Category.Immediate) {

      val rd = Register(IntegerRegisterFile)(rdIn)
      val rs1 = Register(IntegerRegisterFile)(rs1In)
      val imm = Constant(Signed(12))(immIn)

      override def apply(): Instruction = copy()
      override def toAsm: String = s"slti x$rd, x$rs1, $imm"
    }

    case class SLTIU (
                       rdIn: Option[Register] = None,
                       rs1In: Option[Register] = None,
                       immIn: Option[BigInt] = None
                     ) extends Instruction(Category.Compare,Category.Immediate) {

      val rd = Register(IntegerRegisterFile)(rdIn)
      val rs1 = Register(IntegerRegisterFile)(rs1In)
      val imm = Constant(Signed(12))(immIn)

      override def apply(): Instruction = copy()
      override def toAsm: String = s"sltiu x$rd, x$rs1, $imm"
    }

    case class SRAI (
                      rdIn: Option[Register] = None,
                      rs1In: Option[Register] = None,
                      immIn: Option[BigInt] = None
                    ) extends Instruction(Category.Arithmetic,Category.Immediate) {

      val rd = Register(IntegerRegisterFile)(rdIn)
      val rs1 = Register(IntegerRegisterFile)(rs1In)
      val imm = Constant(Unsigned(5))(immIn)

      override def apply(): Instruction = copy()
      override def toAsm: String = s"srai x$rd, x$rs1, $imm"
    }

    case class SRLI (
                      rdIn: Option[Register] = None,
                      rs1In: Option[Register] = None,
                      immIn: Option[BigInt] = None
                    ) extends Instruction(Category.Logical,Category.Immediate) {

      val rd = Register(IntegerRegisterFile)(rdIn)
      val rs1 = Register(IntegerRegisterFile)(rs1In)
      val imm = Constant(Unsigned(5))(immIn)

      override def apply(): Instruction = copy()
      override def toAsm: String = s"srli x$rd, x$rs1, $imm"
    }

    case class XORI (
                      rdIn: Option[Register] = None,
                      rs1In: Option[Register] = None,
                      immIn: Option[BigInt] = None
                    ) extends Instruction(Category.Logical,Category.Immediate) {

      val rd = Register(IntegerRegisterFile)(rdIn)
      val rs1 = Register(IntegerRegisterFile)(rs1In)
      val imm = Constant(Signed(12))(immIn)

      override def apply(): Instruction = copy()
      override def toAsm: String = s"xori x$rd, x$rs1, $imm"
    }

    case class BEQ (
                     rdIn: Option[Register] = None,
                     rs1In: Option[Register] = None,
                     immIn: Option[LabelRecord] = None
                   ) extends Instruction(Category.Branch,Category.Immediate) {

      val rd = Register(IntegerRegisterFile)(rdIn)
      val rs1 = Register(IntegerRegisterFile)(rs1In)
      val imm = LabelReference(Signed(12))(immIn)

      override def apply(): Instruction = copy()
      override def toAsm: String = s"beq x$rd, x$rs1, $imm"
    }

    case class BGE (
                     rdIn: Option[Register] = None,
                     rs1In: Option[Register] = None,
                     immIn: Option[LabelRecord] = None
                   ) extends Instruction(Category.Branch,Category.Immediate) {

      val rd = Register(IntegerRegisterFile)(rdIn)
      val rs1 = Register(IntegerRegisterFile)(rs1In)
      val imm = LabelReference(Signed(12))(immIn)

      override def apply(): Instruction = copy()
      override def toAsm: String = s"bge x$rd, x$rs1, $imm"
    }

    case class BGEU (
                      rdIn: Option[Register] = None,
                      rs1In: Option[Register] = None,
                      immIn: Option[LabelRecord] = None
                    ) extends Instruction(Category.Branch,Category.Immediate) {

      val rd = Register(IntegerRegisterFile)(rdIn)
      val rs1 = Register(IntegerRegisterFile)(rs1In)
      val imm = LabelReference(Signed(12))(immIn)

      override def apply(): Instruction = copy()
      override def toAsm: String = s"bgeu x$rd, x$rs1, $imm"
    }

    case class BLT (
                     rdIn: Option[Register] = None,
                     rs1In: Option[Register] = None,
                     immIn: Option[LabelRecord] = None
                   ) extends Instruction(Category.Branch,Category.Immediate) {

      val rd = Register(IntegerRegisterFile)(rdIn)
      val rs1 = Register(IntegerRegisterFile)(rs1In)
      val imm = LabelReference(Signed(12))(immIn)

      override def apply(): Instruction = copy()
      override def toAsm: String = s"blt x$rd, x$rs1, $imm"
    }

    case class BLTU (
                      rdIn: Option[Register] = None,
                      rs1In: Option[Register] = None,
                      immIn: Option[LabelRecord] = None
                    ) extends Instruction(Category.Branch,Category.Immediate) {

      val rd = Register(IntegerRegisterFile)(rdIn)
      val rs1 = Register(IntegerRegisterFile)(rs1In)
      val imm = LabelReference(Signed(12))(immIn)

      override def apply(): Instruction = copy()
      override def toAsm: String = s"bltu x$rd, x$rs1, $imm"
    }

    case class BNE (
                     rdIn: Option[Register] = None,
                     rs1In: Option[Register] = None,
                     immIn: Option[LabelRecord] = None
                   ) extends Instruction(Category.Branch,Category.Immediate) {

      val rd = Register(IntegerRegisterFile)(rdIn)
      val rs1 = Register(IntegerRegisterFile)(rs1In)
      val imm = LabelReference(Signed(12))(immIn)

      override def apply(): Instruction = copy()
      override def toAsm: String = s"bne x$rd, x$rs1, $imm"
    }

    case class AUIPC (
                       rdIn: Option[Register] = None,
                       immIn: Option[BigInt] = None
                     ) extends Instruction(Category.Load,Category.Immediate) {

      val rd = Register(IntegerRegisterFile)(rdIn)
      val imm = Constant(Unsigned(20))(immIn)

      override def apply(): Instruction = copy()
      override def toAsm: String = s"auipc x$rd, $imm"
    }

    case class LUI (
                     rdIn: Option[Register] = None,
                     immIn: Option[BigInt] = None
                   ) extends Instruction(Category.Load,Category.Immediate) {

      val rd = Register(IntegerRegisterFile)(rdIn)
      val imm = Constant(Signed(20))(immIn)

      override def apply(): Instruction = copy()
      override def toAsm: String = s"lui x$rd, $imm"
    }

    case class JAL (
                     rdIn: Option[Register] = None,
                     immIn: Option[LabelRecord] = None
                   ) extends Instruction(Category.JumpAndLink,Category.Immediate) {

      val rd = Register(IntegerRegisterFile)(rdIn)
      val imm = LabelReference(Signed(12))(immIn)

      override def apply(): Instruction = copy()
      override def toAsm: String = s"jal x$rd, $imm"
    }

    case class SB (
                    rs1In: Option[Register] = None,
                    rs2In: Option[Register] = None,
                    immIn: Option[BigInt] = None
                  ) extends Instruction(Category.Store,Category.Immediate) {

      val rs1 = Register(IntegerRegisterFile)(rs1In)
      val rs2 = Register(IntegerRegisterFile)(rs2In)
      val imm = Constant(Signed(12))(immIn)

      override def apply(): Instruction = copy()
      override def toAsm: String = s"sb x$rs2, $imm(x$rs1)"
    }

    case class SH (
                    rs1In: Option[Register] = None,
                    rs2In: Option[Register] = None,
                    immIn: Option[BigInt] = None
                  ) extends Instruction(Category.Store,Category.Immediate) {

      val rs1 = Register(IntegerRegisterFile)(rs1In)
      val rs2 = Register(IntegerRegisterFile)(rs2In)
      val imm = Constant(Signed(12))(immIn)

      override def apply(): Instruction = copy()
      override def toAsm: String = s"sh x$rs2, $imm(x$rs1)"
    }

    case class SW (
                    rs1In: Option[Register] = None,
                    rs2In: Option[Register] = None,
                    immIn: Option[BigInt] = None
                  ) extends Instruction(Category.Store,Category.Immediate) {

      val rs1 = Register(IntegerRegisterFile)(rs1In)
      val rs2 = Register(IntegerRegisterFile)(rs2In)
      val imm = Constant(Signed(12))(immIn)

      override def apply(): Instruction = copy()
      override def toAsm: String = s"sw x$rs2, $imm(x$rs1)"
    }


    case class LI (
                    rdIn: Option[Register] = None,
                    immIn: Option[BigInt] = None
                  ) extends Instruction(Category.Load,Category.Immediate) {
      val rd = Register(IntegerRegisterFile)(rdIn)
      val imm = Constant(Signed(32))(immIn)
      override def apply(): Instruction = copy()
      override def toAsm: String = s"li x$rd, $imm"
    }

    case class LA (
                    rdIn: Option[Register] = None,
                    immIn: Option[LabelRecord] = None
                  ) extends Instruction(Category.Load,Category.Immediate) {
      val rd = Register(IntegerRegisterFile)(rdIn)
      val imm = LabelReference(Unsigned(32))(immIn)
      override def apply(): Instruction = copy()
      override def toAsm: String = s"la x$rd, $imm"
    }

  }

}
