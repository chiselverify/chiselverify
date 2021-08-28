package chiselverify
import scala.collection.mutable.ArrayBuffer

package object assembly {

  implicit def intDoubleToRangeDouble(x: (Int,Double)): (Range,Double) = (x._1 until x._1, x._2)

  trait Instruction extends InstructionFactory {

    implicit val fields = new ArrayBuffer[InstructionField]()

    val categories: Seq[Category]

    def toAsm: String

    def toWords: Seq[Int]

    def toBinaryString: String = toBytes.reverseMap(_.toInt & 0xFF).map(a => "%08d".format(a.toBinaryString.toInt)).mkString("", "_", "")

    override def toString: String = s"$toAsm [$toHexString]"

    def toHexString: String = toBytes.reverseMap(_.toInt & 0xFF).map(a => "%02X".format(a)).mkString("0x", "_", "")

    def toBytes: Seq[Byte] = toWords.flatMap(word => Array.tabulate(4)(i => (word >> 8 * i) & 0xFF).map(_.toByte))

    def setFields(fieldType: InstructionFieldType, values: BigInt*): Unit = {
      fields.filter(_.fieldType == fieldType).zip(values).foreach{ case (field,value) =>
        field.setValue(value)
      }
    }

  }

  // TODO: how to merge instruction sets?
  trait InstructionSet extends DummyEnum[Instruction] {
    type RegisterType <: Register
    type Registers <: RegisterEnum
    val registers: RegisterEnum
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
