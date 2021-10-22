package chiselverify.assembly

import chiselverify.assembly.RandomHelpers.{BigRange, pow2, rand}

abstract class Register

abstract case class RegisterFile(registers: Register*)

sealed trait Width

case class Signed(w: Int) extends Width

case class Unsigned(w: Int) extends Width

object Constant {
  def apply(width: Width)(init: Option[BigInt]): BigInt = {
    init match {
      case Some(v) =>
        width match {
          case Signed(w) => if (v < -pow2(w - 1) || v > pow2(w - 1) - 1) throw new Exception("Constant out of width bounds")
          case Unsigned(w) => if (v < 0 || v > pow2(w) - 1) throw new Exception("Constant out of width bounds")
        }
        v
      case None => rand(width)
    }
  }
}

object Register {
  def apply(registerFile: RegisterFile)(init: Option[Register]): BigInt = {
    init match {
      case Some(reg) if !registerFile.registers.contains(reg) => throw new Exception("Initializer Register not contained in registerfile")
      case Some(reg) => registerFile.registers.indexOf(reg)
      case None => rand(BigRange(0,registerFile.registers.length-1))
    }
  }
}
