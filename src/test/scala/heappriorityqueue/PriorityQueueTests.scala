package heappriorityqueue

import org.scalatest.{FlatSpec, Matchers}

/**
 * contains 2 test plans for the heap based priority queue
 *  - one drives a single random test onto the dut
 *  - the other tests all memory size and children count configurations
 */
class PriorityQueueTests extends FlatSpec with Matchers {
  val cWid = 2
  val nWid = 8
  val rWid = 5
  val heapSize = 17
  val chCount = 4
  val debugLvl = 0
  val testRuns = 10
  "HeapPriorityQueue" should s"pass $testRuns test runs" in {
    chisel3.iotesters.Driver(() => new HeapPriorityQueue(heapSize, chCount, nWid, cWid, rWid)) {
      c => {new PriorityQueueTester(testRuns,debugLvl)(c,heapSize,chCount)(cWid,nWid,rWid).dut}
    } should be(true)
  }
  "HeapPriorityQueue" should s"pass 16*$testRuns test runs with different memory sizes and children count" in {
    val sizes = Array(33,65,129,257)
    val chCounts = Array(2,4,8,16)
    for(size <- sizes) {
      for(chCount <- chCounts) {
        chisel3.iotesters.Driver(() => new HeapPriorityQueue(size, chCount, nWid, cWid, rWid)) {
          c => {
            new PriorityQueueTester(testRuns, debugLvl)(c, size, chCount)(cWid, nWid, rWid).dut
          }
        } should be(true)
      }
    }
  }
}

/**
 * Tester class containing the input driving code for the model and the dut
 *  - the dut and model are poked with random operations and values
 *  - after every operation the memory state of the dut is verified by comparing with the behavioural model
 * @param testRuns number of operations to poke the dut with
 * @param debugLvl debug details: 0=muted, 1=overall information, 2=stepwise information
 * @param c the dut
 * @param heapSize the maximum size of the heap
 * @param chCount the number of children per node in the heap
 * @param cWid the width of the cyclic priority field
 * @param nWid the width of the normal priority field
 * @param rWid the width of the reference ID field
 */
class PriorityQueueTester(testRuns: Int, debugLvl: Int)(c: HeapPriorityQueue, heapSize: Int, chCount: Int)(cWid : Int,nWid : Int,rWid : Int) {

  val rand = scala.util.Random
  val dut = new HeapPriorityQueueWrapper(c, heapSize, chCount, debugLvl)(cWid, nWid, rWid)
  val model = new Behavioural(heapSize, chCount)(cWid, nWid, rWid)

  var refIDcounter = 0

  // tracking variables
  var stepCounter = 0
  var successfulInsertions = 0
  var successfulRemovals = 0
  var insertionTimes = 0
  var removalTimes = 0

  for (i <- 0 until testRuns) {
    var debug = ""
    if (rand.nextInt(2) == 1) { // insert operation
      // determine what to insert
      val ins = Array(rand.nextInt(math.pow(2, cWid).toInt), rand.nextInt(math.pow(2, nWid).toInt), refIDcounter)
      // increment reference ID counter and wrap if necessary
      refIDcounter += 1
      if (refIDcounter >= Math.pow(2, rWid)) refIDcounter = 0

      // simulate dut and model
      val (steps,success,debugStr) = dut.insert(ins(0), ins(1), ins(2))
      model.insert(ins(0), ins(1), ins(2))

      // process results
      debug = debugStr
      stepCounter += steps
      if(success){
        insertionTimes += steps
        successfulInsertions += 1
      }
    } else { // remove operation
      // determine removal reference ID
      val rem = rand.nextInt(math.pow(2, rWid).toInt - 1)

      // simulate dut and model
      val (steps,success,debugStr) = dut.remove(rem)
      model.remove(rem)

      // process results
      debug = debugStr
      stepCounter += steps
      if(success){
        removalTimes += steps
        successfulRemovals += 1
      }
    }
    // cross check dut and model; kill test and print debug if not matching
    if(!dut.compareWithModel(model.mem)){
      println(debug)
      sys.exit(0)
    }
  }

  //////////////////////////////////////////////////Report////////////////////////////////////////////////////////

  val avgIns = insertionTimes.toDouble/successfulInsertions
  val avgRem = removalTimes.toDouble/successfulRemovals
  val successRate = ((successfulInsertions+successfulRemovals.toDouble)/testRuns)*100

  println(s"${"="*20}Report${"="*20}\n"+
    s"Heapsize = $heapSize, children count = $chCount\n"+
    s"${"%.2f".format(successRate)}% of operations were successful\n"+
    s"$successfulInsertions insertions which took on average ${"%.2f".format(avgIns)} cycles\n"+
    s"$successfulRemovals removals which took on average ${"%.2f".format(avgRem)} cycles\n"+
    s"${"="*46}"
  )

  ////////////////////////////////////////////////////////////////////////////////////////////////////////////////
}

