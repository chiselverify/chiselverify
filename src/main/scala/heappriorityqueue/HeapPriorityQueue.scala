package heappriorityqueue

import chisel3._
import chisel3.util._
import lib._

//beaware: the all max configuration is reserved
/**
 *
 * @param size the maximum size of the heap
 * @param chCount the number of children per node in the tree. Must be power of 2
 * @param nWid width of the normal priority value
 * @param cWid width of the cyclic priority value
 * @param rWid width of the reference ID tags
 */
class HeapPriorityQueue(size : Int, chCount : Int, nWid : Int, cWid : Int, rWid : Int) extends Module{
  val io = IO(new Bundle{
    // Interface for signaling head element to user.
    // I.e. the element with the lowest priority
    val head = new Bundle{
      val valid = Output(Bool())
      val none = Output(Bool())
      val prio = Output(new Priority(nWid,cWid))
      val refID = Output(UInt(rWid.W))
    }
    // Interface for element insertion/removal
    // Timing:
    // User must maintain input asserted until done is asserted.
    // User must deassert input when done is asserted (unless a new cmd is made).
    // User must ensure that reference ID tags are unique.
    val cmd = new Bundle{
      // inputs
      val valid = Input(Bool())
      val op = Input(Bool()) // 0=Remove, 1=Insert
      val prio = Input(new Priority(nWid,cWid))
      val refID = Input(UInt(rWid.W))
      // outputs
      val done = Output(Bool())
      val result = Output(Bool()) // 0=Success, 1=Failure
      val rm_prio = Output(new Priority(nWid,cWid))
    }

    // ram ports to synchronous memory
    val rdPort = new rdPort(log2Ceil(size/chCount),Vec(chCount,new PriorityAndID(nWid,cWid,rWid)))
    val wrPort = new wrPort(log2Ceil(size/chCount),chCount,Vec(chCount,new PriorityAndID(nWid,cWid,rWid)))

    // search port to a component with the ability to find the positions of reference ID's in memory
    val srch = new searchPort(size,rWid)

    //TODO: remove debug outputs
    val debug = new Bundle{
      val state = Output(UInt())
      val heapifierState = Output(UInt())
      val heapifierIndex = Output(UInt(log2Ceil(size).W))
      val minOut = Output(UInt(log2Ceil(chCount).W))
      val minInputs = Output(Vec(chCount+1,new PriorityAndID(nWid,cWid,rWid)))
      val swap = Output(Bool())
      val heapSize = Output(UInt(log2Ceil(size).W))
    }
  })
  if(!isPow2(chCount)) throw new Exception("The number of children must be a power of 2!")

  // modules
  val heapifier = Module(new Heapifier(size, chCount, nWid, cWid, rWid))

  // state elements
  val idle :: headInsertion :: normalInsertion :: initSearch :: waitForSearch :: resetCell :: lastRemoval :: headRemoval :: tailRemoval :: removal :: waitForHeapifyUp :: waitForHeapifyDown :: Nil = Enum(12)
  val stateReg = RegInit(idle)
  val heapSizeReg = RegInit(0.U(log2Ceil(size+1).W))
  val headReg = RegInit(0.U.asTypeOf(new PriorityAndID(nWid,cWid,rWid)))
  val tempReg = RegInit(VecInit(Seq.fill(chCount)(0.U.asTypeOf(new PriorityAndID(nWid,cWid,rWid)))))
  val removalIndex = RegInit(0.U(log2Ceil(size).W))
  val removedPrio = RegInit(0.U.asTypeOf(new Priority(nWid,cWid)))

  // flags
  val headValid = RegInit(true.B)
  val errorReg = RegInit(false.B)

  // ram write address and offset
  val wrIndex = WireDefault(0.U(log2Ceil(size).W))
  val wrIndexToRam = ((wrIndex - 1.U) >> log2Ceil(chCount)).asUInt
  val wrIndexOffset = Mux(wrIndex === 0.U, 0.U, wrIndex(log2Ceil(chCount),0) - 1.U(log2Ceil(size).W))

  // ram read address and offset
  val rdIndex = WireDefault(0.U(log2Ceil(size).W))
  val rdIndexToRam = ((rdIndex - 1.U) >> log2Ceil(chCount)).asUInt
  val rdIndexOffset = Mux(rdIndex === 0.U, 0.U, rdIndex(log2Ceil(chCount),0) - 1.U(log2Ceil(size).W))

  val resetVal = VecInit(Seq.fill(nWid+cWid+rWid)(true.B)).asUInt.asTypeOf(new PriorityAndID(nWid,cWid,rWid))

  // heapSize controlling
  val incHeapsize = heapSizeReg + 1.U
  val decHeapsize = heapSizeReg - 1.U

  // connect heapifier
  heapifier.io.control.heapifyUp := false.B
  heapifier.io.control.heapifyDown := false.B
  heapifier.io.control.idx := heapSizeReg
  heapifier.io.control.heapSize := heapSizeReg
  heapifier.io.headPort.rdData := headReg
  when(heapifier.io.headPort.write){
    headReg := heapifier.io.headPort.wrData
  }
  // default ram connections
  io.rdPort.address := heapifier.io.rdPort.address
  heapifier.io.rdPort.data := io.rdPort.data
  io.wrPort.address := heapifier.io.wrPort.address
  io.wrPort.data := heapifier.io.wrPort.data
  io.wrPort.write := heapifier.io.wrPort.write
  io.wrPort.mask := heapifier.io.wrPort.mask

  // default assignments
  io.head.prio := headReg.prio
  io.head.none := heapSizeReg === 0.U
  io.head.valid := headValid
  io.head.refID := headReg.id
  io.cmd.done := false.B
  io.cmd.result := errorReg
  io.cmd.rm_prio := removedPrio
  io.srch.refID := io.cmd.refID
  io.srch.search := false.B
  io.srch.heapSize := heapSizeReg

  //TODO: remove debug outputs
  io.debug.state := stateReg
  io.debug.heapSize := heapSizeReg
  io.debug.heapifierState := heapifier.io.state
  io.debug.heapifierIndex := heapifier.io.indexOut
  io.debug.minOut := heapifier.io.out
  io.debug.minInputs := heapifier.io.minInputs
  io.debug.swap := heapifier.io.swap

    switch(stateReg){
    is(idle){
      io.cmd.done := true.B

      wrIndex := heapSizeReg // prepare write port for insertion
      rdIndex := decHeapsize // prepare read port for removal
      io.wrPort.address := wrIndexToRam
      io.rdPort.address := rdIndexToRam

      when(heapSizeReg =/= 0.U){ // reset valid flag if queue not empty
        headValid := true.B
      }

      when(io.cmd.valid){
        errorReg := false.B
        when(io.cmd.op){ // insertion
          headValid := false.B
          stateReg := normalInsertion
          when(heapSizeReg === size.U){ // queue is full
            errorReg := true.B
            stateReg := idle
          }.elsewhen(heapSizeReg === 0.U){
            stateReg := headInsertion
          }
        }.otherwise{ // removal
          headValid := false.B
          when(heapSizeReg === 1.U){ // beaware: refID is disregarded when last element is removed
            stateReg := lastRemoval
          }.otherwise{
            stateReg := initSearch
            io.srch.search := true.B
          }
        }
      }
    }
    is(headInsertion){ // insertion into empty queue
      headReg.prio := io.cmd.prio
      headReg.id := io.cmd.refID
      heapSizeReg := incHeapsize

      stateReg := idle
    }
    is(normalInsertion){ // insertion into already filled queue
      // write new priority
      wrIndex := heapSizeReg
      io.wrPort.data(wrIndexOffset).prio := io.cmd.prio
      io.wrPort.data(wrIndexOffset).id := io.cmd.refID
      io.wrPort.mask := UIntToOH(wrIndexOffset,chCount)
      io.wrPort.write := true.B

      // increase heap size
      heapSizeReg := incHeapsize

      // initiate heapify up
      heapifier.io.control.idx := Mux(heapSizeReg < (chCount+1).U, 0.U, ((heapSizeReg - 1.U) >> log2Ceil(chCount)).asUInt)
      heapifier.io.control.heapifyUp := true.B
      stateReg := waitForHeapifyUp
    }
    is(initSearch){ // start search and prepare for following steps
      io.srch.search := true.B

      // decrease size of heap
      heapSizeReg := decHeapsize

      // read last queue element from ram into temporary register
      rdIndex := decHeapsize
      tempReg := io.rdPort.data

      // prepare ram write port
      wrIndex := decHeapsize
      io.wrPort.address := wrIndexToRam

      stateReg := waitForSearch
      headValid := true.B // head is valid as long as we are not removing the head element
      when(headReg.id === io.cmd.refID){ // compare head and tail with desired refID
        stateReg := headRemoval
        headValid := false.B
        removedPrio := headReg.prio
      }.elsewhen(io.rdPort.data(rdIndexOffset).id === io.cmd.refID){
        stateReg := tailRemoval
        removedPrio := io.rdPort.data(rdIndexOffset).prio
      }
    }
    is(waitForSearch){ // wait for memory to look up the index corresponding to the reference ID

      // prepare ram write port
      wrIndex := heapSizeReg
      io.wrPort.address := wrIndexToRam

      // prepare ram read port to read the element to be removed
      rdIndex := io.srch.res
      io.rdPort.address := rdIndexToRam

      io.srch.search := true.B

      stateReg := waitForSearch
      when(io.srch.error){ // detect a none existing reference ID
        errorReg := true.B
        stateReg := idle
        heapSizeReg := incHeapsize // reset heapsize
      }.elsewhen(io.srch.done){
        removalIndex := io.srch.res // save index of removal
        stateReg := resetCell
      }
    }
    is(resetCell){
      // reset the cleared queue position to max values
      rdIndex := heapSizeReg
      io.wrPort.data(rdIndexOffset) := resetVal
      io.wrPort.mask := UIntToOH(rdIndexOffset, chCount)
      io.wrPort.write := true.B

      // prepare write port
      wrIndex := removalIndex
      io.wrPort.address := wrIndexToRam

      // store the element to be removed
      removedPrio := io.rdPort.data(wrIndexOffset).prio

      stateReg := removal
    }
    is(lastRemoval){ // last element is removed from queue
      removedPrio := headReg.prio
      heapSizeReg := decHeapsize
      stateReg := idle
    }
    is(headRemoval){ // remove head from queue
      rdIndex := heapSizeReg
      wrIndex := removalIndex

      headReg := tempReg(rdIndexOffset)

      io.wrPort.data(rdIndexOffset) := resetVal
      io.wrPort.mask := UIntToOH(rdIndexOffset,chCount)
      io.wrPort.write := true.B

      // initiate heapify down
      heapifier.io.control.heapifyDown := true.B
      heapifier.io.control.idx := Mux(removalIndex < (chCount+1).U, 0.U, ((removalIndex - 1.U) >> log2Ceil(chCount)).asUInt)
      stateReg := waitForHeapifyDown
    }
    is(tailRemoval){
      // set tail to max values
      wrIndex := heapSizeReg
      io.wrPort.data(wrIndexOffset) := resetVal
      io.wrPort.mask := UIntToOH(wrIndexOffset,chCount)
      io.wrPort.write := true.B

      stateReg := idle
    }
    is(removal){
      // overwrite the element to be deleted with cached tail
      wrIndex := removalIndex
      rdIndex := heapSizeReg
      io.wrPort.data(wrIndexOffset) := tempReg(rdIndexOffset)
      io.wrPort.mask := UIntToOH(wrIndexOffset)
      io.wrPort.write := true.B

      // initiate heapify up
      heapifier.io.control.heapifyUp := true.B
      heapifier.io.control.idx := Mux(removalIndex < (chCount+1).U, 0.U, ((removalIndex - 1.U) >> log2Ceil(chCount)).asUInt)
      stateReg := waitForHeapifyUp
    }
    is(waitForHeapifyUp){ // wait for the heapifier to complete one up pass
      stateReg := waitForHeapifyUp
      when(heapifier.io.control.done){
        stateReg := idle
        when(io.cmd.op === 0.U && !heapifier.io.control.swapped){ // when no swap occurred during removal -> heapify down
          heapifier.io.control.idx := Mux(removalIndex < (chCount+1).U, 0.U, ((removalIndex - 1.U) >> log2Ceil(chCount)).asUInt)
          heapifier.io.control.heapifyDown := true.B
          stateReg := waitForHeapifyDown
        }
      }.otherwise{
        heapifier.io.control.heapifyUp := true.B
      }
    }
    is(waitForHeapifyDown){ // wait for the heapifier to complete one down pass
      stateReg := waitForHeapifyDown
      when(heapifier.io.control.done) {
        stateReg := idle
      }.otherwise{
        heapifier.io.control.heapifyDown := true.B
      }
    }
  }
}
