package chiselverify.fuzzing

import chisel3.Module.clock
import chisel3._
import chisel3.experimental.DataMirror
import chisel3.tester.{testableClock, testableData}
import chiselverify.coverage.CoverageReporter

import java.nio.file.{Files, Paths}
import java.io._
import scala.annotation.tailrec

object Fuzzer {
    def apply[T <: MultiIOModule](
        dut: T,
        cr: CoverageReporter[T],
        goldenModel: List[BigInt] => List[BigInt],
        target : Int = 100,
        timeout : BigInt = BigInt(1000000)
    )(
        result: String,
        bugResult: String,
        seeds: String*
    ) : Int = {
        //Create a test corpus by reading seed file
        //Here we assume that the seeds have a legal format TODO: maybe bad idea?
        val seedCorpus = seeds.map(readBitString)

        //Create a text corpus that will store all of our data during the fuzzing
        val corpusName = "corpus.txt"
        val pw = new PrintWriter(new File(corpusName))
        pw.write(seedCorpus.foldLeft("")((acc, seed) => acc ++ seed ++ "\n"))
        pw.close()

        //Get ports to poke
        val ports = (DataMirror.fullModulePorts(dut)).filter(p => p._1 != "clock" && p._1 != "reset" && p._1 != "io")

        //Fuzz loop
        @tailrec
        def fuzzLoop(curCoverage: Int, corpusIdx: Int) : Int = {
            if((curCoverage == target) || (corpusIdx == timeout)) curCoverage
            else {
                //TODO: Start with T from seedCorpus and poke
                val t: String = if(corpusIdx < seedCorpus.size) seedCorpus(corpusIdx) else {
                    val seed = readBitString(corpusName).split("\n")(corpusIdx)
                    //TODO: When corpusIdx >= seedCorpus.size start mutating with AFL
                    seed
                }

                //TODO: Call CR.totalCoverage to update curCoverage
                //TODO: Compute result by getting bin hit values from coverage DB
                //TODO: Compare result with existing results. If new write test to corpus.txt
                //TODO: Compare result to golden model result. If different, report bug and save to bug.txt
                ???
                fuzzLoop(0, 0)
            }
        }
        fuzzLoop(0, 0)
    }

    def readBitString(fileName: String) : String = Files.readAllBytes(Paths.get(fileName))
            .map(b => String.format("%8s", Integer.toBinaryString(b & 0xFF)))
            .foldLeft("")((acc, byte) => acc ++ byte)


    /**
      * Generic poke that pokes each input individually by:
      * - Mapping the bit string to the right size to fit each UInt input.
      * - Poke new value into input.
      * @param dut the DUT that we want to poke.
      * @param inputs the bit string read from the test corpus.
      * @tparam T the module type of the dut.
      */
    def poke[T <: MultiIOModule](dut: T, ports: Seq[(String,Data)], inputs: String): Unit = {
        /**
          * Retrieve a wanted input from the set of inputs
          */
        def extractInput(input: String, inputSize: Int, startIdx: Int): BigInt =
            BigInt(input.slice(startIdx, startIdx + inputSize), inputSize)

        //Get port widths
        val sizes = ports.map(_._2.getWidth)
        val totalSize = sizes.sum
        val testInputs = sizes.foldLeft((Seq[BigInt](), 0)) {
            case ((result, index), size) =>
                val in = extractInput(inputs, size, sizes.slice(0, index).sum)
                (result :+ in, index + 1)
        }._1

        //Group tests into waves of inputs separated by a step
        testInputs.grouped(ports.size).foreach (t => {
            ports.zip(t.zip(sizes)).foreach {
                case ((_, data), (test, size)) => data.poke(test.U(size))
            }
            dut.clock.step()
        })
    }
}
