package verifyTests.assembly

import org.scalatest.flatspec.AnyFlatSpec
import org.scalatest.matchers.should.Matchers

import chiselverify.assembly.Label.LabelRecord
import chiselverify.assembly.leros.{Leros, QuickAccessMemory}
import chiselverify.assembly.rv32i.IntegerRegister
import chiselverify.assembly.{Constant, IllegalRegisterInitializer, LabelReference, Register, Signed, Unsigned, WidthViolation, leros, rv32i}

class FieldsTest extends AnyFlatSpec with Matchers {
  behavior of "Constant field"

  it should "generate a random constant (unsigned)" in {
    val constant = Constant(Unsigned(8))(None)
    (constant >= 0 && constant < 256) should be (true)
  }

  it should "generate a random constant (signed)" in {
    val constant = Constant(Signed(8))(None)
    (constant > -129 && constant < 127) should be (true)
  }

  it should "accept initializer (unsigned)" in {
    Constant(Unsigned(8))(Some(BigInt(10))) should equal (10)
  }

  it should "accept initializer (signed)" in {
    Constant(Signed(8))(Some(BigInt(-10))) should equal (-10)
  }

  it should "not allow an out of bounds initializer (unsigned)" in {
    a [WidthViolation] should be thrownBy(Constant(Unsigned(8))(Some(BigInt(256))))
  }

  it should "not allow an out of bounds initializer (signed)" in {
    a [WidthViolation] should be thrownBy(Constant(Signed(8))(Some(BigInt(-129))))
  }

  behavior of "Label reference field"

  it should "accept a label" in {
    val ref = "HelloWorld"
    LabelReference(Unsigned(8))(Some(LabelRecord(ref))) should equal (ref)
  }

  behavior of "Register field"

  it should "produce a random register" in {
    QuickAccessMemory.registers.indices should contain (Register(QuickAccessMemory)(None))
  }

  it should "accept an initializer" in {
    Register(QuickAccessMemory)(Some(QuickAccessMemory.registers(10))) should equal (10)
  }

  it should "not allow an illegal initializer" in {
    a [IllegalRegisterInitializer] should be thrownBy(Register(leros.QuickAccessMemory)(Some(rv32i.IntegerRegister.zero)))
  }
}
