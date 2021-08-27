package chiselverify.fuzzing

import chisel3._
import chisel3.experimental.DataMirror
import chiselverify.coverage.CoverageReporter

import java.nio.file.{Files, Paths}

object Fuzzer {
    def apply[T <: MultiIOModule](
        dut: T,
        cr: CoverageReporter[T],
        target : Int = 100,
        timeout : BigInt = BigInt(1000000)
    )(
        result: String,
        seeds: String*
    ) : Int = {
        //Create a test corpus by reading seed file
        //Here we assume that the seeds have a legal format TODO: maybe bad idea?
        val corpus = seeds.map(path => Files.readAllBytes(Paths.get(path)))

        //Get ports to poke
        val ports = (DataMirror.fullModulePorts(dut)).filter(p => p._1 != "clock" && p._1 != "reset" && p._1 != "io")

        //TODO: Make a generic DUT.poke(inputBits)

    }

    /**
      * Generic poke that pokes each input individually by:
      * - Mapping the array byte to the right size to fit each UInt input.
      * - Poke new value into input.
      * @param dut the DUT that we want to poke.
      * @param value the byte array read from the test corpus.
      * @tparam T the modue type of the dut.
      */
    def poke[T <: MultiIOModule](dut: T, value: Array[Byte]): Unit = ???
}
