package examples.newpriorityqueue.min

import chiseltest._
import chiselverify.coverage._
import org.scalatest.FlatSpec

import scala.util.Random._

class MinTest extends FlatSpec with ChiselScalatestTester{

  val n = 4
  val width = 4

  val seed = nextInt(Int.MaxValue)
  val iterations = 1000

  behavior of "Min Module"

  it should "find the smallest item" in {
    test(new Min(n,new MinTestType(width))){ dut =>

      println(s"Seed = $seed")
      val testTransaction = new MinTestTransaction(n,width,seed)

      val cr = new CoverageReporter(dut)
      cr.register(List(
        CoverPoint(dut.io.items(0).value,"items")(
          Bins("all0",0 to 0) :: Nil
        )
      ))

      val testSeq = Seq.fill(iterations){
        testTransaction.randomize
        testTransaction.toBundle
      }

      val results = testSeq.map{ transaction =>
        dut.io.items.zip(transaction).foreach{ case (port,wiggle) => port.poke(wiggle)}
        dut.clock.step()
        cr.sample()
        val modelOut = MinModel.compute(transaction)
        val dutOut = dut.io.min.peek
        dut.io.min.expect(modelOut,s"$transaction should be $modelOut but was $dutOut")
        (transaction,dutOut,modelOut)
      }
      cr.printReport()
      //println(results.map{case (tr,dut,model) => s"${tr.mkString("[",", ","]")} should be $model and was $dut"}.mkString("\n"))
    }
  }

}
