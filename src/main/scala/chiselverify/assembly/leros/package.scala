package chiselverify.assembly

import chiselverify.assembly.RandomHelpers.{BigRange, pow2}
import chiselverify.assembly.leros.Leros._
import chiselverify.assembly.intToBigIntOption

package object leros {



  object QuickAccessMemory extends RegisterFile(
    Seq.fill(256)(new Register {}):_*
  )



  object Leros extends InstructionSet {

    override val memoryAddressSpace: BigRange = BigRange(0, pow2(16))
    override val inputOutputAddressSpace: BigRange = BigRange(0, pow2(8))


    val readAccess = Pattern(Category.Load)(implicit c => {
      val address = c.nextMemoryAddress(Seq())
      Seq(
        loadi((address & 0xFF)),
        loadhi(((address >> 8) & 0xFF)),
        loadh2i(((address >> 16) & 0xFF)),
        loadh3i(((address >> 24) & 0xFF)),
        ldaddr(),
        Instruction.select(ldind(0),ldindbu(0))
      )
    })

    val writeAccess = Pattern(Category.Store)(implicit c => {
      val address = c.nextMemoryAddress(Seq())
      Seq(
        loadi((address & 0xFF)),
        loadhi(((address >> 8) & 0xFF)),
        loadh2i(((address >> 16) & 0xFF)),
        loadh3i(((address >> 24) & 0xFF)),
        ldaddr(),
        Instruction.select(stind(0),stindb(0))
      )
    })

    val jump = Pattern(implicit c => {
      Seq()
    })



    override val instructions = Seq(
      add(),
      addi(),
      sub(),
      subi(),
      shr(),
      load(),
      loadi(),
      and(),
      andi(),
      or(),
      ori(),
      xor(),
      xori(),
      loadhi(),
      loadh2i(),
      loadh3i(),
      store(),
      out(),
      in(),
      jal(),
      ldaddr(),
      readAccess,
      writeAccess,
      br(),
      brz(),
      brnz(),
      brp(),
      brn(),
      scall()
    )



    case class nop() extends Instruction(Category.Nop) {
      override def apply(): Instruction = this
      override def toAsm = "nop"
    }

    case class add (rsIn: Option[Register] = None) extends Instruction(
      Category.Arithmetic
    ) {
      val rs = Register(QuickAccessMemory)(rsIn)
      override def apply(): Instruction = copy()
      override def toAsm: String = s"add r$rs"
    }

    case class addi (immIn: Option[BigInt] = None) extends Instruction(
      Category.Arithmetic
    ) {
      val imm = Constant(Unsigned(8))(immIn)
      override def apply(): Instruction = copy()
      override def toAsm: String = s"addi $imm"
    }

    case class sub (rsIn: Option[Register] = None) extends Instruction(
      Category.Arithmetic
    ) {
      val rs = Register(QuickAccessMemory)(rsIn)
      override def apply(): Instruction = copy()
      override def toAsm: String = s"sub r$rs"
    }

    case class subi (immIn: Option[BigInt] = None) extends Instruction(
      Category.Arithmetic
    ) {
      val imm = Constant(Unsigned(8))(immIn)
      override def apply(): Instruction = copy()
      override def toAsm: String = s"subi $imm"
    }

    case class shr (rsIn: Option[Register] = None) extends Instruction(
      Category.Arithmetic
    ) {
      val rs = Register(QuickAccessMemory)(rsIn)
      override def apply(): Instruction = copy()
      override def toAsm: String = s"sra r$rs"
    }

    case class load (rsIn: Option[Register] = None) extends Instruction(
      Category.Load
    ) {
      val rs = Register(QuickAccessMemory)(rsIn)
      override def apply(): Instruction = copy()
      override def toAsm: String = s"load r$rs"
    }

    case class loadi (immIn: Option[BigInt] = None) extends Instruction(
      Category.Load
    ) {
      val imm = Constant(Unsigned(8))(immIn)
      override def apply(): Instruction = copy()
      override def toAsm: String = s"loadi $imm"
    }

    case class loadhi (immIn: Option[BigInt] = None) extends Instruction(
      Category.Load
    ) {
      val imm = Constant(Unsigned(8))(immIn)
      override def apply(): Instruction = copy()
      override def toAsm: String = s"loadhi $imm"
    }

    case class loadh2i (immIn: Option[BigInt] = None) extends Instruction(
      Category.Load
    ) {
      val imm = Constant(Unsigned(8))(immIn)
      override def apply(): Instruction = copy()
      override def toAsm: String = s"loadh2i $imm"
    }

    case class loadh3i (immIn: Option[BigInt] = None) extends Instruction(
      Category.Load
    ) {
      val imm = Constant(Unsigned(8))(immIn)
      override def apply(): Instruction = copy()
      override def toAsm: String = s"loadh3i $imm"
    }

    case class and (rsIn: Option[Register] = None) extends Instruction(
      Category.Logical
    ) {
      val rs = Register(QuickAccessMemory)(rsIn)
      override def apply(): Instruction = copy()
      override def toAsm: String = s"and r$rs"
    }

    case class andi (immIn: Option[BigInt] = None) extends Instruction(
      Category.Logical
    ) {
      val imm = Constant(Unsigned(8))(immIn)
      override def apply(): Instruction = copy()
      override def toAsm: String = s"andi $imm"
    }

    case class or (rsIn: Option[Register] = None) extends Instruction(
      Category.Logical
    ) {
      val rs = Register(QuickAccessMemory)(rsIn)
      override def apply(): Instruction = copy()
      override def toAsm: String = s"or r$rs"
    }

    case class ori (immIn: Option[BigInt] = None) extends Instruction(
      Category.Logical
    ) {
      val imm = Constant(Unsigned(8))(immIn)
      override def apply(): Instruction = copy()
      override def toAsm: String = s"ori $imm"
    }

    case class xor (rsIn: Option[Register] = None) extends Instruction(
      Category.Logical
    ) {
      val rs = Register(QuickAccessMemory)(rsIn)
      override def apply(): Instruction = copy()
      override def toAsm: String = s"xor r$rs"
    }

    case class xori (immIn: Option[BigInt] = None) extends Instruction(
      Category.Logical
    ) {
      val imm = Constant(Unsigned(8))(immIn)
      override def apply(): Instruction = copy()
      override def toAsm: String = s"xori $imm"
    }

    case class store (rsIn: Option[Register] = None) extends Instruction(
      Category.Store
    ) {
      val rs = Register(QuickAccessMemory)(rsIn)
      override def apply(): Instruction = copy()
      override def toAsm: String = s"store r$rs"
    }

    case class in (immIn: Option[BigInt] = None) extends Instruction(
      Category.Input
    ) {
      val imm = Constant(Unsigned(8))(immIn)
      override def apply(): Instruction = copy()
      override def toAsm: String = s"in $imm"
    }

    case class out (immIn: Option[BigInt] = None) extends Instruction(
      Category.Output
    ) {
      val imm = Constant(Unsigned(8))(immIn)
      override def apply(): Instruction = copy()
      override def toAsm: String = s"out $imm"
    }

    case class jal (rsIn: Option[Register] = None) extends Instruction(
      Category.Jump
    ) {
      val rs = Register(QuickAccessMemory)(rsIn)
      override def apply(): Instruction = copy()
      override def toAsm: String = s"jal r$rs"
    }

    case class ldaddr() extends Instruction(Category.Load) {
      override def apply(): Instruction = this
      override def toAsm = "ldaddr"
    }

    case class ldind (immIn: Option[BigInt] = None) extends Instruction(
      Category.Load
    ) {
      val imm = Constant(Unsigned(8))(immIn)
      override def apply(): Instruction = copy()
      override def toAsm: String = s"ldind $imm"
    }

    case class ldindbu (immIn: Option[BigInt] = None) extends Instruction(
      Category.Load
    ) {
      val imm = Constant(Unsigned(8))(immIn)
      override def apply(): Instruction = copy()
      override def toAsm: String = s"ldindbu $imm"
    }

    case class stind (immIn: Option[BigInt] = None) extends Instruction(
      Category.Store
    ) {
      val imm = Constant(Unsigned(8))(immIn)
      override def apply(): Instruction = copy()
      override def toAsm: String = s"stind $imm"
    }

    case class stindb (immIn: Option[BigInt] = None) extends Instruction(
      Category.Store
    ) {
      val imm = Constant(Unsigned(8))(immIn)
      override def apply(): Instruction = copy()
      override def toAsm: String = s"stindb $imm"
    }

    case class br (immIn: Option[BigInt] = None) extends Instruction(
      Category.Branch
    ) {
      val imm = Constant(Unsigned(12))(immIn)
      override def apply(): Instruction = copy()
      override def toAsm: String = s"br $imm"
    }

    case class brz (immIn: Option[BigInt] = None) extends Instruction(
      Category.Branch
    ) {
      val imm = Constant(Unsigned(12))(immIn)
      override def apply(): Instruction = copy()
      override def toAsm: String = s"brz $imm"
    }

    case class brnz (immIn: Option[BigInt] = None) extends Instruction(
      Category.Branch
    ) {
      val imm = Constant(Unsigned(12))(immIn)
      override def apply(): Instruction = copy()
      override def toAsm: String = s"brnz $imm"
    }

    case class brp (immIn: Option[BigInt] = None) extends Instruction(
      Category.Branch
    ) {
      val imm = Constant(Unsigned(12))(immIn)
      override def apply(): Instruction = copy()
      override def toAsm: String = s"brp $imm"
    }

    case class brn (immIn: Option[BigInt] = None) extends Instruction(
      Category.Branch
    ) {
      val imm = Constant(Unsigned(12))(immIn)
      override def apply(): Instruction = copy()
      override def toAsm: String = s"brn $imm"
    }

    case class scall() extends Instruction(Category.EnvironmentCall) {
      override def apply(): Instruction = this
      override def toAsm = "ldaddr"
    }


  }

}
