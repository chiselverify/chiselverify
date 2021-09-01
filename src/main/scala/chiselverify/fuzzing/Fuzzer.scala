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
        timeout : Int = 1000000
    )(
        result: String, //Without extension
        bugResult: String, //Without extension
        seeds: String* //Without extension
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
        def fuzzLoop(curCoverage: Int, corpusIdx: Int) : (Int, Int) = {
            if((curCoverage == target) || (corpusIdx == timeout)) (curCoverage, corpusIdx)
            else {
                implicit val nPasses: Int = 4

                val t: String = if(corpusIdx < seedCorpus.size) seedCorpus(corpusIdx) else {
                    val seed = readBitString(corpusName).split("\n")(corpusIdx)
                    mutate(seed)
                }

                //reset FC and DUT
                dut.reset.poke(Reset()) //TODO: Is this really how we reset the DUT?
                dut.clock.step()
                cr.reset()

                //Poke
                val inVals = poke(dut, ports, t)
                val coverage = (cr.report.coverage * 100).toInt

                //Retrieve results from coverage reporter
                val res: Seq[(String, List[BigInt])] = cr.results

                //Result is stored as a csv file
                val oldResults: Seq[(String, List[BigInt])] = readResultCSV(result)

                //Compare result with existing results. If new write test to corpus.txt
                //Sorting should ensure that the names are the same
                val diff = res.sortBy(_._1).zip(oldResults.sortBy(_._1)).map {
                    case ((name1, vals), (name2, oldvals)) if name1 == name2 => (name1, vals filterNot oldvals.contains)
                    case _ => throw new IllegalArgumentException("Illegal result state")
                }

                //Check if the result is interesting and update files
                if(diff.nonEmpty) {
                    val corpus = new PrintWriter(new FileOutputStream(new File(corpusName),true))
                    corpus.write(t)
                    corpus.close()

                    //Update results file
                    val resMap = oldResults.toMap
                    val newRes = diff.map { case (name, vals) => name -> (resMap(name) ++ vals) }
                    val resFile = new PrintWriter(new FileOutputStream(new File(s"$result.csv"), false))
                    resFile.write(formatResultCSV(newRes))
                    resFile.close()
                }

                //Check for bugs and write to bug file if needed
                if(res != goldenModel(inVals.toList)) {
                    val bugFile = new PrintWriter(new FileOutputStream(new File(s"$bugResult.csv"), true))
                    bugFile.write(t)
                    bugFile.close()
                }

                fuzzLoop(if(coverage > curCoverage) coverage else curCoverage, corpusIdx + 1)
            }
        }
        fuzzLoop(0, 0)._1
    }

    def readBitString(fileName: String) : String = Files.readAllBytes(Paths.get(fileName))
            .map(b => String.format("%8s", Integer.toBinaryString(b & 0xFF)))
            .foldLeft("")((acc, byte) => acc ++ byte)

    def readResultCSV(fileName: String): Seq[(String, List[BigInt])] = {
        //Open result stream
        val bufferedSource = io.Source.fromFile(s"$fileName.csv")

        //Parse CSV to get result
        val res = bufferedSource.getLines.map(line => {
            val cols = line.split(",").map(_.trim)
            // do whatever you want with the columns here
            (cols.head, cols.tail.map(BigInt(_)).toList)
        }).toSeq

        //Close stream and return
        bufferedSource.close
        res
    }

    def formatResultCSV(result: Seq[(String, List[BigInt])]): String = {
        val res = new StringBuilder()
        result.foreach {
            case (name, vals) =>
                res ++ s"$name,${vals.map(v => s"$v,")}"
                res.deleteCharAt(res.size - 1)
                res ++ "\n"
        }
        res.mkString
    }

    /**
      * Mutates a given input bit stream using the following methods:
      * - Walking bit flips
      * - Walking byte flips
      * - Simple arithmetics
      * - Known integers
      * @param inputBits the input that will be mutated.
      * @return a mutated version of inputBits
      */
    def mutate(inputBits: String)(implicit nPasses: Int): String = kI(sA(wBF(wbF(inputBits))))

    /**
      * Walking bit flips mutation
      * @param inputBits the input that will be mutated.
      * @return a mutated version of inputBits
      */
    def wbF(inputBits: String)(implicit nPasses: Int): String = ???

    /**
      * Walking byte flips mutation
      * @param inputBits the input that will be mutated.
      * @return a mutated version of inputBits
      */
    def wBF(inputBits: String)(implicit nPasses: Int): String = ???

    /**
      * Simple arithmetic mutation technique
      * @param inputBits the input that will be mutated.
      * @return a mutated version of inputBits
      */
    def sA(inputBits: String)(implicit nPasses: Int): String = ???

    /**
      * Known integer mutation technique
      * @param inputBits the input that will be mutated.
      * @return a mutated version of inputBits
      */
    def kI(inputBits: String)(implicit nPasses: Int): String = ???


    /**
      * Generic poke that pokes each input individually by:
      * - Mapping the bit string to the right size to fit each UInt input.
      * - Poke new value into input.
      * @param dut the DUT that we want to poke.
      * @param inputs the bit string read from the test corpus.
      * @tparam T the module type of the dut.
      */
    def poke[T <: MultiIOModule](dut: T, ports: Seq[(String,Data)], inputs: String): Seq[BigInt] = {
        /**
          * Retrieve a wanted input from the set of inputs
          */
        def extractInput(input: String, inputSize: Int, startIdx: Int): BigInt =
            BigInt(input.slice(startIdx, startIdx + inputSize), inputSize)

        //Get port widths
        val sizes = ports.map(_._2.getWidth)
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
        testInputs
    }
}
