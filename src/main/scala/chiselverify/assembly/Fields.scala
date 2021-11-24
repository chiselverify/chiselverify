package chiselverify.assembly

import chiselverify.assembly.Label.LabelRecord
import chiselverify.assembly.RandomHelpers.{BigRange, pow2, rand}

abstract class Register
abstract case class RegisterFile(registers: Register*)

sealed trait Width
case class Signed(w: Int) extends Width
case class Unsigned(w: Int) extends Width

class IllegalRegisterInitializer extends Exception("Initializer Register not contained in registerfile")
class WidthViolation extends Exception("Constant out of width bounds")

/**
  * Initializer for a constant. Either outputs a random value in the width range or the passed initializer if defined.
  */
object Constant {
  def apply(width: Width)(init: Option[BigInt]): BigInt = {
    init match {
      case Some(v) =>
        width match {
          case Signed(w) => if (v < -pow2(w - 1) || v > pow2(w - 1) - 1) throw new WidthViolation
          case Unsigned(w) => if (v < 0 || v > pow2(w) - 1) throw new WidthViolation
        }
        v
      case None => rand(width)
    }
  }
}

/**
  * Initializer for instruction fields containing label references. If no initializer is provided a random number
  * fitting the bit width is returned
  */
object LabelReference {
  def apply(width: Width)(init: Option[LabelRecord]): String = {
    init match {
      case Some(LabelRecord(lbl)) => lbl
      case None => rand(width).toString()
    }
  }
}

/**
  * Initializer for a register instruction field. Either a random register from the register file is chosen
  * or, if defined, the initializer is returned.
  */
object Register {
  def apply(registerFile: RegisterFile)(init: Option[Register]): BigInt = {
    init match {
      case Some(reg) if !registerFile.registers.contains(reg) => throw new IllegalRegisterInitializer
      case Some(reg) => registerFile.registers.indexOf(reg)
      case None => rand(BigRange(0, registerFile.registers.length - 1))
    }
  }
}
