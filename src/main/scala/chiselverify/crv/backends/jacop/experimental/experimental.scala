package chiselverify.crv.backends.jacop

import chisel3.experimental.DataMirror
import chisel3._
import chiselverify.crv.backends.jacop.experimental.RandBundle.ModuleElaboration
import chisel3.experimental.BundleLiterals._

package object experimental {

  class RawModuleWrapper[T <: Bundle](bundle: T) extends RawModule {
    val clock = IO(Input(Clock()))
    val b = IO(Input(bundle.cloneType))
    dontTouch(b)
  }

  implicit class RandomBundleWrapper[T <: Bundle with RandBundle](bundle: T) extends Bundle {
    def randomBundle(): T = {

      if (!bundle.randomize) throw chiselverify.crv.CRVException("ERROR: Chisel-crv couldn't randomize the bundle")

      val module = ModuleElaboration.elaborate(() => new RawModuleWrapper[T](bundle))
      val portNames = DataMirror.fullModulePorts(module).drop(1).filter(!_._2.isInstanceOf[Bundle])
      val modelBinding = portNames.zipWithIndex.map { case (name, index) =>
        new Function1[Bundle, (Data, Data)] {
          def apply(t: Bundle): (Data, Data) = {
            t.getElements(index) -> bundle.currentModel(name._1).value().U
          }
        }
      }
      chiselTypeOf(module.b).Lit(modelBinding: _*)
    }
  }

  implicit class IntegerConverter(i: Int)(implicit model: Model) {
    def R(): Rand = {
      new Rand(i, i)
    }
  }

  implicit class BigIntegerConverter(i: BigInt)(implicit model: Model) {
    require(i < Int.MaxValue)
    require(i > Int.MinValue)

    def R(): Rand = {
      new Rand(i.toInt, i.toInt)
    }
  }
}
