package formal


import chisel3._
import chisel3.experimental.IO
import chisel3.util._


object MagicPacketTracker {
  def apply[D <: Data](enq: ValidIO[D], deq: ValidIO[D], depth: Int, debugPrint: Boolean = false): Unit = {
    val tracker = Module(new MagicPacketTracker(chiselTypeOf(enq.bits), depth))
    tracker.enq := enq
    tracker.deq := deq
    val startTracking = IO(Input(Bool()))
    tracker.startTracking := startTracking
    if(debugPrint) {
      val cycle = RegInit(0.U(8.W))
      cycle := cycle + 1.U
      when(tracker.enq.fire) {
        when(tracker.deq.fire) {
          printf(p"$cycle: ${Hexadecimal(tracker.enq.bits.asUInt)} --> ${Hexadecimal(tracker.deq.bits.asUInt)}\n")
        } .otherwise {
          printf(p"$cycle: ${Hexadecimal(tracker.enq.bits.asUInt)} -->\n")
        }
      } .elsewhen(tracker.deq.fire) {
        printf(p"$cycle: --> ${Hexadecimal(tracker.deq.bits.asUInt)}\n")
      }
    }
  }
}

/** Tracks random packets for formally verifying FIFOs
  *
  *  This ensures that when some data enters the FIFO, it
  *  will always be dequeued after the correct number of
  *  elements.
  *  So essentially we are verifying data integrity.
  *  Note that this does not imply that the FIFO has no bugs
  *  since e.g., a FIFO that never allows elements to be
  *  enqueued would easily pass our assertions.
  */
class MagicPacketTracker[D <: Data](dataTpe: D, fifoDepth: Int) extends Module {
  val enq = IO(Input(ValidIO(dataTpe)))
  val deq = IO(Input(ValidIO(dataTpe)))

  // count the number of elements in the fifo
  val elementCount = RegInit(0.U(log2Ceil(fifoDepth + 1).W))
  val nextElementCount = Mux(
    enq.fire && !deq.fire,
    elementCount + 1.U,
    Mux(!enq.fire && deq.fire, elementCount - 1.U, elementCount)
  )
  elementCount := nextElementCount

  // track a random "magic" packet through the fifo
  val startTracking = IO(Input(Bool()))
  val isActive = RegInit(false.B)
  val packetValue = Reg(chiselTypeOf(enq.bits))
  val packetCount = Reg(chiselTypeOf(elementCount))

  when(!isActive && enq.fire && startTracking) {
    when(deq.fire && elementCount === 0.U) {
      assert(
        enq.bits.asUInt === deq.bits.asUInt,
        "element should pass through the fifo, but %x != %x",
        enq.bits.asUInt,
        deq.bits.asUInt
      )
    }.otherwise {
      isActive := true.B
      packetValue := enq.bits
      packetCount := nextElementCount
    }
  }

  when(isActive && deq.fire) {
    packetCount := packetCount - 1.U
    when(packetCount === 1.U) {
      assert(
        packetValue.asUInt === deq.bits.asUInt,
        "element should be dequeued in this cycle, but %x != %x",
        packetValue.asUInt,
        deq.bits.asUInt
      )
      isActive := false.B
    }
  }
}