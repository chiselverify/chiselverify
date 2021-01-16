package verifyTests.crv.backends.jacop.bundle

import Chisel.fromIntToWidth
import chisel3.{Bundle, Data, UInt}

class AluInput(val size: Int) extends Bundle {
  val a = UInt(size.W)
  val b = UInt(size.W)
  val fn = UInt(2.W)

  // Ugly
  override def typeEquivalent(data: Data): Boolean = {
    data match {
      case _: (AluInput@AluInputConstraint(size)) => true
      case _ => false
    }
  }
}
