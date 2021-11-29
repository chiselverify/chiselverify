package chiselverify.assembly

import chiselverify.assembly.Label.LabelRecord
import chiselverify.assembly.RandomHelpers.{BigRange, pow2, randSplit}

package object leros {

  // leros uses the lower part of main memory as a register file
  object QuickAccessMemory extends RegisterFile(
    Seq.fill(256)(new Register {}): _*
  )


  object Leros extends InstructionSet {

    // the read pattern sets the accumulator to a random base address and executes the load with offset
    val read = Pattern(Category.Load)(implicit c => {
      val address = c.nextMemoryAddress(Seq())
      val (base,offset) = randSplit(address)(Unsigned(32),Signed(8))
      Seq(
        loadi((base & 0xFF)),
        loadhi(((base >> 8) & 0xFF)),
        loadh2i(((base >> 16) & 0xFF)),
        loadh3i(((base >> 24) & 0xFF)),
        ldaddr(),
        Instruction.select(ldind(offset), ldindbu(offset))
      )
    })

    def simulationWrapper(body: InstructionFactory): Pattern = Pattern(implicit c => Seq(body,scall(0)))
    def simulationWrapper(n: Int): Pattern = Pattern(implicit c => Seq(Instruction.fill(n),scall(0)))

    // the write pattern sets the accumulator to a random address and executes the store with offset
    val write = Pattern(Category.Store)(implicit c => {
      val address = c.nextMemoryAddress(Seq())
      val (base,offset) = randSplit(address)(Unsigned(32),Signed(8))
      Seq(
        loadi((base & 0xFF)),
        loadhi(((base >> 8) & 0xFF)),
        loadh2i(((base >> 16) & 0xFF)),
        loadh3i(((base >> 24) & 0xFF)),
        ldaddr(),
        Instruction.select(stind(offset), stindb(offset))
      )
    })

    // the jump and link pattern requests a jump target, sets the accumulator to the target address and executes the jump
    val jumpAndLink = Pattern(Category.JumpAndLink)(implicit c => {
      val target = c.nextJumpTarget()
      Seq(loadLbl(target),loadhLbl(target),jal())
    })

    // branches rely on the generator context to deliver a target label
    val branch = Pattern(Category.Branch)(implicit c => {
      val target = c.nextJumpTarget()
      Seq(
        Instruction.select(br(target),brz(target),brnz(target),brp(target),brn(target))
      )
    })

    // leros has full 16-bit address space
    override val memoryAddressSpace: BigRange = BigRange(0, pow2(16))

    // leros has a 8-bit IO port address space
    override val inputOutputAddressSpace: BigRange = BigRange(0, pow2(8))

    // all instructions
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
      jumpAndLink,
      ldaddr(),
      read,
      write,
      branch,
      scall()
    )


    case class nop() extends Instruction(Category.Nop) {
      override def apply(): Instruction = nop()

      override def toAsm = "nop"
    }

    case class add(rsIn: Option[Register] = None) extends Instruction(
      Category.Arithmetic
    ) {
      val rs = Register(QuickAccessMemory)(rsIn)

      override def apply(): Instruction = copy()

      override def toAsm: String = s"add r$rs"
    }

    case class addi(immIn: Option[BigInt] = None) extends Instruction(
      Category.Arithmetic, Category.Immediate
    ) {
      val imm = Constant(Unsigned(8))(immIn)

      override def apply(): Instruction = copy()

      override def toAsm: String = s"addi $imm"
    }

    case class sub(rsIn: Option[Register] = None) extends Instruction(
      Category.Arithmetic
    ) {
      val rs = Register(QuickAccessMemory)(rsIn)

      override def apply(): Instruction = copy()

      override def toAsm: String = s"sub r$rs"
    }

    case class subi(immIn: Option[BigInt] = None) extends Instruction(
      Category.Arithmetic, Category.Immediate
    ) {
      val imm = Constant(Unsigned(8))(immIn)

      override def apply(): Instruction = copy()

      override def toAsm: String = s"subi $imm"
    }

    case class shr(rsIn: Option[Register] = None) extends Instruction(
      Category.Arithmetic
    ) {
      val rs = Register(QuickAccessMemory)(rsIn)

      override def apply(): Instruction = copy()

      override def toAsm: String = s"sra r$rs"
    }

    case class load(rsIn: Option[Register] = None) extends Instruction(
      Category.Load
    ) {
      val rs = Register(QuickAccessMemory)(rsIn)

      override def apply(): Instruction = copy()

      override def toAsm: String = s"load r$rs"
    }

    case class loadLbl(immIn: Option[LabelRecord] = None) extends Instruction(
      Category.Load
    ) {
      val imm = LabelReference(Signed(8))(immIn)

      override def apply(): Instruction = copy()

      override def toAsm: String = s"load <$imm"
    }

    case class loadhLbl(immIn: Option[LabelRecord] = None) extends Instruction(
      Category.Load
    ) {
      val imm = LabelReference(Signed(8))(immIn)

      override def apply(): Instruction = copy()

      override def toAsm: String = s"loadh >$imm"
    }

    case class loadi(immIn: Option[BigInt] = None) extends Instruction(
      Category.Load, Category.Immediate
    ) {
      val imm = Constant(Unsigned(8))(immIn)

      override def apply(): Instruction = copy()

      override def toAsm: String = s"loadi $imm"
    }

    case class loadhi(immIn: Option[BigInt] = None) extends Instruction(
      Category.Load, Category.Immediate
    ) {
      val imm = Constant(Unsigned(8))(immIn)

      override def apply(): Instruction = copy()

      override def toAsm: String = s"loadhi $imm"
    }

    case class loadh2i(immIn: Option[BigInt] = None) extends Instruction(
      Category.Load, Category.Immediate
    ) {
      val imm = Constant(Unsigned(8))(immIn)

      override def apply(): Instruction = copy()

      override def toAsm: String = s"loadh2i $imm"
    }

    case class loadh3i(immIn: Option[BigInt] = None) extends Instruction(
      Category.Load, Category.Immediate
    ) {
      val imm = Constant(Unsigned(8))(immIn)

      override def apply(): Instruction = copy()

      override def toAsm: String = s"loadh3i $imm"
    }

    case class and(rsIn: Option[Register] = None) extends Instruction(
      Category.Logical
    ) {
      val rs = Register(QuickAccessMemory)(rsIn)

      override def apply(): Instruction = copy()

      override def toAsm: String = s"and r$rs"
    }

    case class andi(immIn: Option[BigInt] = None) extends Instruction(
      Category.Logical, Category.Immediate
    ) {
      val imm = Constant(Unsigned(8))(immIn)

      override def apply(): Instruction = copy()

      override def toAsm: String = s"andi $imm"
    }

    case class or(rsIn: Option[Register] = None) extends Instruction(
      Category.Logical
    ) {
      val rs = Register(QuickAccessMemory)(rsIn)

      override def apply(): Instruction = copy()

      override def toAsm: String = s"or r$rs"
    }

    case class ori(immIn: Option[BigInt] = None) extends Instruction(
      Category.Logical, Category.Immediate
    ) {
      val imm = Constant(Unsigned(8))(immIn)

      override def apply(): Instruction = copy()

      override def toAsm: String = s"ori $imm"
    }

    case class xor(rsIn: Option[Register] = None) extends Instruction(
      Category.Logical
    ) {
      val rs = Register(QuickAccessMemory)(rsIn)

      override def apply(): Instruction = copy()

      override def toAsm: String = s"xor r$rs"
    }

    case class xori(immIn: Option[BigInt] = None) extends Instruction(
      Category.Logical, Category.Immediate
    ) {
      val imm = Constant(Unsigned(8))(immIn)

      override def apply(): Instruction = copy()

      override def toAsm: String = s"xori $imm"
    }

    case class store(rsIn: Option[Register] = None) extends Instruction(
      Category.Store
    ) {
      val rs = Register(QuickAccessMemory)(rsIn)

      override def apply(): Instruction = copy()

      override def toAsm: String = s"store r$rs"
    }

    case class in(immIn: Option[BigInt] = None) extends Instruction(
      Category.Input, Category.Immediate
    ) {
      val imm = Constant(Unsigned(8))(immIn)

      override def apply(): Instruction = copy()

      override def toAsm: String = s"in $imm"
    }

    case class out(immIn: Option[BigInt] = None) extends Instruction(
      Category.Output, Category.Immediate
    ) {
      val imm = Constant(Unsigned(8))(immIn)

      override def apply(): Instruction = copy()

      override def toAsm: String = s"out $imm"
    }

    case class jal(rsIn: Option[Register] = None) extends Instruction(
      Category.JumpAndLink
    ) {
      val rs = Register(QuickAccessMemory)(rsIn)

      override def apply(): Instruction = copy()

      override def toAsm: String = s"jal r$rs"
    }

    case class ldaddr() extends Instruction(Category.Load) {
      override def apply(): Instruction = ldaddr()

      override def toAsm = "ldaddr"
    }

    case class ldind(immIn: Option[BigInt] = None) extends Instruction(
      Category.Load, Category.Immediate
    ) {
      val imm = Constant(Unsigned(8))(immIn)

      override def apply(): Instruction = copy()

      override def toAsm: String = s"ldind $imm"
    }

    case class ldindbu(immIn: Option[BigInt] = None) extends Instruction(
      Category.Load, Category.Immediate
    ) {
      val imm = Constant(Unsigned(8))(immIn)

      override def apply(): Instruction = copy()

      override def toAsm: String = s"ldindbu $imm"
    }

    case class stind(immIn: Option[BigInt] = None) extends Instruction(
      Category.Store, Category.Immediate
    ) {
      val imm = Constant(Unsigned(8))(immIn)

      override def apply(): Instruction = copy()

      override def toAsm: String = s"stind $imm"
    }

    case class stindb(immIn: Option[BigInt] = None) extends Instruction(
      Category.Store, Category.Immediate
    ) {
      val imm = Constant(Unsigned(8))(immIn)

      override def apply(): Instruction = copy()

      override def toAsm: String = s"stindb $imm"
    }

    case class br(immIn: Option[LabelRecord] = None) extends Instruction(
      Category.Branch, Category.Immediate
    ) {
      val imm = LabelReference(Signed(12))(immIn)

      override def apply(): Instruction = copy()

      override def toAsm: String = s"br $imm"
    }

    case class brz(immIn: Option[LabelRecord] = None) extends Instruction(
      Category.Branch
    ) {
      val imm = LabelReference(Signed(12))(immIn)

      override def apply(): Instruction = copy()

      override def toAsm: String = s"brz $imm"
    }

    case class brnz(immIn: Option[LabelRecord] = None) extends Instruction(
      Category.Branch
    ) {
      val imm = LabelReference(Signed(12))(immIn)

      override def apply(): Instruction = copy()

      override def toAsm: String = s"brnz $imm"
    }

    case class brp(immIn: Option[LabelRecord] = None) extends Instruction(
      Category.Branch
    ) {
      val imm = LabelReference(Signed(12))(immIn)

      override def apply(): Instruction = copy()

      override def toAsm: String = s"brp $imm"
    }

    case class brn(immIn: Option[LabelRecord] = None) extends Instruction(
      Category.Branch
    ) {
      val imm = LabelReference(Signed(12))(immIn)

      override def apply(): Instruction = copy()

      override def toAsm: String = s"brn $imm"
    }

    case class scall(immIn: Option[BigInt] = None) extends Instruction(Category.EnvironmentCall) {
      val imm = Constant(Unsigned(8))(immIn)
      override def apply(): Instruction = copy()

      override def toAsm = s"scall $imm"
    }


  }

}
