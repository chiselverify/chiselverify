package heappriorityqueue

import chisel3.iotesters.PeekPokeTester
import org.scalatest._

class MinFinderTester(dut: MinFinder, nWid: Int, cWid: Int, rWid: Int,  n: Int, testPasses: Int, debugOutput: Boolean) extends PeekPokeTester(dut) {
  /////////////////////////////////// helper methods ///////////////////////////////////
  def populatedList(): Array[Array[BigInt]] = {
    val rand = scala.util.Random
    return Array.tabulate(n)(i => Array(i,rand.nextInt(math.pow(2,cWid).toInt),rand.nextInt(math.pow(2,nWid).toInt)))
  }
  def applyVec(list: Array[Array[BigInt]]) = {
    for(i <- 0 until n){
      poke(dut.io.values(i).prio.cycl,list(i)(1))
      poke(dut.io.values(i).prio.norm,list(i)(2))
      poke(dut.io.values(i).id,list(i)(0))
    }
  }
  def calculateOut(list: Array[Array[BigInt]]): Int = {
    val cyclic = list.map(_(1))
    val cyclicMins = cyclic.zipWithIndex.filter(_._1 == cyclic.min).map(_._2)
    if(cyclicMins.length == 1){
      return cyclicMins(0)
    }else{
      val normals = list.map(_(2))
      val candidates = normals.filter(a => cyclicMins.contains(normals.indexOf(a)))
      val normalMins = candidates.zipWithIndex.filter(_._1 == candidates.min).map(_._2)
      return normals.indexOf(candidates(normalMins(0)))
    }
  }
  /////////////////////////////////// helper methods ///////////////////////////////////
  /////////////////////////////////// Test ///////////////////////////////////
  if(debugOutput) println("Format: referenceID : cyclicPriority : normalPriority")
  for(i <- 0 until testPasses){
    val values = populatedList()
    applyVec(values)
    if(debugOutput) println(s"${values.map(_.mkString(":")).mkString(", ")} -> ${peek(dut.io.res).values.mkString(":")}")
    expect(dut.io.idx,calculateOut(values))
    expect(dut.io.res.prio.cycl, values(calculateOut(values))(1))
    expect(dut.io.res.prio.norm, values(calculateOut(values))(2))
    expect(dut.io.res.id, values(calculateOut(values))(0))
  }
  /////////////////////////////////// Test ///////////////////////////////////
}

class MinFinderTest extends FlatSpec with Matchers {
  val nWid = 8
  val cWid = 2
  val rWid = 3
  val n = 8
  val testPasses = 20
  val debugOutput = false
  "MinFinder" should "identify minimum value with the lowest index" in {
    chisel3.iotesters.Driver(() => new MinFinder(n, nWid, cWid, rWid)) {
      c => new MinFinderTester(c,nWid,cWid,rWid,n,testPasses,debugOutput)
    } should be(true)
  }
}