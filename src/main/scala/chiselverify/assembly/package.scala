package chiselverify
import scala.collection.mutable.ArrayBuffer

package object assembly {

  trait Instruction extends InstructionFactory {

    implicit val addressFields = new ArrayBuffer[AddressField]
    implicit val registerFields = new ArrayBuffer[RegisterField]
    implicit val constantFields = new ArrayBuffer[ConstantField]
    implicit val addressOffsetFields = new ArrayBuffer[AddressOffsetField]
    implicit val branchOffsetFields = new ArrayBuffer[BranchOffsetField]

    val categories: Seq[Category]

    def toAsm: String

    def toWordArray: Array[Int]

    def toBinaryString: String = this.toByteArray.reverseMap(_.toInt & 0xFF).map(a => "%08d".format(a.toBinaryString.toInt)).mkString("", "_", "")

    override def toString: String = s"$toAsm [$toHexString]"

    def toHexString: String = this.toByteArray.reverseMap(_.toInt & 0xFF).map(a => "%02X".format(a)).mkString("0x", "_", "")

    def toByteArray: Array[Byte] = this.toWordArray.flatMap(word => Array.tabulate(4)(i => (word >> 8 * i) & 0xFF).map(_.toByte))
  }

  trait InstructionSet extends DummyEnum[Instruction] {
    type RegisterType <: Register
    type Registers <: RegisterEnum
    val registers: DummyEnum[Register]
    val memoryInstructions: Seq[Instruction]

    def memoryAccess(address: BigInt): Seq[Instruction]
  }

  trait PickableSeq[T] extends Seq[T] {
    def pick(): T = this.apply(scala.util.Random.nextInt(this.length))
  }

  //TODO: find a real enum which is easy to work with (polymorphism)
  trait DummyEnum[T] {
    val values: Seq[T]

    def pick(): T = values.apply(scala.util.Random.nextInt(values.length))
  }

  trait InstructionFactory {
    def apply(): Instruction
  }

  abstract class Register(val width: Int, name: String, encoding: Int) {
    override def toString: String = name

    def toInt: Int = encoding
  }

  abstract class RegisterEnum extends DummyEnum[Register]

}
