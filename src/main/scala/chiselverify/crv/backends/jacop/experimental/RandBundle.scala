package chiselverify.crv.backends.jacop.experimental

import Chisel.{Bool, SInt}
import chisel3.stage.{ChiselGeneratorAnnotation, DesignAnnotation}
import chisel3.{Data, RawModule, UInt}
import chiselverify.crv.backends.jacop.{Rand, RandObj}
import scala.language.implicitConversions
import org.jacop.core.Var

import scala.math.pow
import scala.reflect.runtime.universe
import scala.reflect.runtime.universe.{Type, runtimeMirror, typeOf}

object RandBundle {

  object ModuleElaboration {
    def elaborate[M <: RawModule](gen: () => M): M = {
      val genAnno = ChiselGeneratorAnnotation(gen)
      val elaborationAnnos = genAnno.elaborate
      val dut = elaborationAnnos.collectFirst { case DesignAnnotation(d) => d }.get
      dut.asInstanceOf[M]
    }
  }
}

trait RandBundle extends RandObj {

  private val rm = runtimeMirror(getClass.getClassLoader)
  private val im = rm.reflect(this)
  private val members = im.symbol.typeSignature.members

  private def memberFilter(mType: Type)(member: universe.Symbol): Boolean = {
    member.typeSignature.resultType <:< mType && !member.isMacro &&
    !member.name.toString.contains("do_as")
  }

  private def getName[T <: Data](member: T, collection: Iterable[universe.Symbol]): String = {

    im.reflectField(collection.filter(x => im.reflectField(x.asTerm).get.asInstanceOf[T] == member).head.asTerm)
      .symbol
      .toString
      .drop(6)
  }


  private def searchNameInModel(name: String): Option[Var] = currentModel.vars.filter(_ != null).find(_.id() == name)

  private def uints: Iterable[universe.Symbol] = members.filter(memberFilter(typeOf[UInt]))
  private def bools: Iterable[universe.Symbol] = members.filter(memberFilter(typeOf[Bool]))
  private def sints: Iterable[universe.Symbol] = members.filter(memberFilter(typeOf[SInt]))

  implicit def dataToRand[T <: Data](d: T): Rand = {
    def composeRandMember(d: T): (String, Int, Int) = {
      d match {
        case _ : Bool => ("b_" + getName(d, bools), 0, 1)
        case _ : SInt => ("b_" + getName(d, sints), -pow(2, d.getWidth - 1).toInt, pow(2, d.getWidth - 1).toInt)
        case _ : UInt => ("b_" + getName(d, uints), 0, pow(2, d.getWidth).toInt)
        // This is not necessary
        case _ => throw chiselverify.crv.CRVException("ERROR: Type not deducted")
      }
    }
    // This is need because jacop currently supports only integer domain and not BigInteger domain
    require(d.getWidth < 30)

    val (name, min, max) = composeRandMember(d)
    searchNameInModel(name).getOrElse(new Rand(name, min, max)).asInstanceOf[Rand]
  }
}
