package chiselverify.assembly.instructionsets

import chiselverify.assembly._


package object rv32i {

  abstract class RV32Instruction extends Instruction {
    val opcode: Int
    val mnemonic: String
  }

  abstract class RV32Register(nr: Int) extends Register(32, s"x$nr", nr)

  object RV32Instruction {
    abstract class Rtype extends RV32Instruction {
      val funct3: Int
      val funct7: Int
      val rd = RegisterField(5)
      val rs1 = RegisterField(5)
      val rs2 = RegisterField(5)

      def toWords: Seq[Int] = Seq(opcode | (rd.value.get.toInt << 7) | (funct3 << 12) | (rs1.value.get.toInt << 15) | (rs2.value.get.toInt << 20) | (funct7 << 25)).map(_.toInt)

      def toAsm: String = s"$mnemonic $rd, $rs1, $rs2"
    }

    abstract class Itype extends RV32Instruction {
      val funct3: Int
      val imm: InstructionField
      val rd = RegisterField(5)
      val rs1 = RegisterField(5)

      def toWords: Seq[Int] = Seq(opcode | (rd.value.get.toInt << 7) | (funct3 << 12) | (rs1.value.get.toInt << 15) | (imm.value.get << 20)).map(_.toInt)
    }

    abstract class Stype extends RV32Instruction {
      val funct3: Int
      val imm = AddressOffsetField(12)
      val rs1 = RegisterField(5)
      val rs2 = RegisterField(5)

      def toWords: Seq[Int] = Seq(opcode | ((imm.value.get & 0x1F) << 7) | (funct3 << 12) | (rs1.value.get.toInt << 15) | (rs2.value.get.toInt << 20) | ((imm.value.get >> 5) << 25)).map(_.toInt)

      def toAsm: String = s"$mnemonic $rs1, $imm($rs2)"
    }

    //TODO: should the field widths be real widths or indicate the value range (if some lower bits are dropped)?
    abstract class SBtype extends RV32Instruction {
      val funct3: Int
      val imm = BranchOffsetField(13)
      val rs1 = RegisterField(5)
      val rs2 = RegisterField(5)

      def toWords: Seq[Int] = Seq(opcode | ((imm.value.get & 0x800) >> (11 - 7)) | ((imm.value.get & 0x1D) << (8 - 1)) | (funct3 << 12) | (rs1.value.get.toInt << 15) | (rs2.value.get.toInt << 20) | ((imm.value.get & 0x7D0) << (25 - 5)) | ((imm.value.get & 0x1000) << (31 - 12))).map(_.toInt)

      def toAsm: String = s"$mnemonic $rs1, $rs2, $imm"
    }

    abstract class Utype extends RV32Instruction {
      val rd = RegisterField(5)
      val imm = ConstantField(12)
    }
  }

  /*
    object RV32I extends InstructionSet with Enum[RV32Instruction] {
      val values = findValues
      override type RegisterType = RV32Register.type
      override type InstructionType = RV32Instruction

      object ADD extends rv32i.Instruction.Rtype {
        val mnemonic = "add"
        val opcode   = 0x33
        val funct3   = 0x00
        val funct7   = 0x00
      }

      class SUB extends rv32i.Instruction.Rtype {
        val mnemonic = "sub"
        val opcode   = 0x33
        val funct3   = 0x00
        val funct7   = 0x20
      }

    }
    */
  object RV32Category {
    case object Rtype extends Category

    case object Itype extends Category

    case object Stype extends Category

    case object SBtype extends Category
  }

  object RV32I2 extends InstructionSet {
    override type RegisterType = RV32Register
    override type Registers = RV32Register.type
    val registers = RV32Register
    val values = Seq(ADD, SUB, LW, ADDI, SW, BLT, JALR)
    val memoryInstructions = Seq(LW)
    val reachableMemory = Domain(0,scala.math.pow(2,32).toInt)

    def memoryAccess(address: BigInt): Seq[Instruction] = {
      val regSet = ADDI()
      val offset = 0 // this is a field of a pattern!
      regSet.setFields(InstructionField.ConstantField)
      Seq(regSet)
    }

    object ADD extends ADD
    class ADD extends RV32Instruction.Rtype {
      val mnemonic = "add"
      val opcode = 0x33
      val funct3 = 0x00
      val funct7 = 0x00
      val categories = Seq(
        Category.ArithmeticInstruction,
        RV32Category.Rtype
      )

      def apply() = new ADD
    }

    object SUB extends SUB
    class SUB extends RV32Instruction.Rtype {
      val mnemonic = "sub"
      val opcode = 0x33
      val funct3 = 0x00
      val funct7 = 0x20
      val categories = Seq(
        Category.ArithmeticInstruction,
        RV32Category.Rtype
      )

      def apply() = new SUB
    }

    object LW extends LW
    class LW extends RV32Instruction.Itype {
      val mnemonic = "lw"
      val opcode = 0x03
      val funct3 = 0x02
      val imm = AddressOffsetField(12)
      val categories = Seq(
        Category.LoadInstruction,
        RV32Category.Itype
      )

      def toAsm: String = s"$mnemonic $rd, $imm($rs1)"

      def apply() = new LW
    }

    object ADDI extends ADDI
    class ADDI extends RV32Instruction.Itype {
      val mnemonic = "addi"
      val opcode = 0x23
      val funct3 = 0x00
      val imm = ConstantField(12)
      val categories = Seq(
        Category.ArithmeticInstruction,
        RV32Category.Itype
      )

      def toAsm: String = s"$mnemonic $rd, $rs1, $imm"

      def apply() = new ADDI
    }

    object SW extends SW
    class SW extends RV32Instruction.Stype {
      val mnemonic = "sw"
      val opcode = 0x23
      val funct3 = 0x02
      val categories = Seq(
        Category.StoreInstruction,
        RV32Category.Stype
      )

      def apply() = new SW
    }

    object BLT extends BLT
    class BLT extends RV32Instruction.SBtype {
      val mnemonic = "blt"
      val opcode = 0xC3
      val funct3 = 0x04
      val categories = Seq(
        Category.BranchInstruction,
        RV32Category.SBtype
      )

      def apply() = new BLT
    }

    object JALR extends JALR
    class JALR extends RV32Instruction.Itype {
      val mnemonic = "jalr"
      val opcode = 0xC7
      val funct3 = 0x00
      val imm = AddressOffsetField(12)
      val categories = Seq(
        Category.JumpInstruction,
        RV32Category.Itype
      )

      def toAsm: String = s"$mnemonic $rd, $imm($rs1)"

      def apply() = new JALR
    }

  }

  object RV32Register extends RegisterEnum {
    val values = Seq(zero, ra, sp, gp, tp, t0, t1, t2, s0, s1, a0, a1, a2, a3, a4, a5, a6, a7, s2, s3, s4, s5, s6, s7, s8, s9, s10, s11, t3, t4, t5, t6)

    case object zero extends RV32Register(0)

    case object ra extends RV32Register(1)

    case object sp extends RV32Register(2)

    case object gp extends RV32Register(3)

    case object tp extends RV32Register(4)

    case object t0 extends RV32Register(5)

    case object t1 extends RV32Register(6)

    case object t2 extends RV32Register(7)

    case object s0 extends RV32Register(8)

    case object s1 extends RV32Register(9)

    case object a0 extends RV32Register(10)

    case object a1 extends RV32Register(11)

    case object a2 extends RV32Register(12)

    case object a3 extends RV32Register(13)

    case object a4 extends RV32Register(14)

    case object a5 extends RV32Register(15)

    case object a6 extends RV32Register(16)

    case object a7 extends RV32Register(17)

    case object s2 extends RV32Register(18)

    case object s3 extends RV32Register(19)

    case object s4 extends RV32Register(20)

    case object s5 extends RV32Register(21)

    case object s6 extends RV32Register(22)

    case object s7 extends RV32Register(23)

    case object s8 extends RV32Register(24)

    case object s9 extends RV32Register(25)

    case object s10 extends RV32Register(26)

    case object s11 extends RV32Register(27)

    case object t3 extends RV32Register(28)

    case object t4 extends RV32Register(29)

    case object t5 extends RV32Register(30)

    case object t6 extends RV32Register(31)

  }


}
