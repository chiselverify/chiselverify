package examples.priorityqueue.ringbuffer

import chisel3._
import chisel3.util._

class RingBuffer[T <: Data](gen: => T)(depth: Int) extends Module {
  val io = IO(new Bundle {
    val enq = Flipped(EnqIO(gen))
    val deq = Flipped(DeqIO(gen))
  })

  // FIFO registers
  val mem = RegInit(VecInit(Seq.fill(depth)(0.U.asTypeOf(gen))))

  // pointer registers
  val head = Counter(depth)
  val tail = Counter(depth)

  // wrapping bits
  val wrapBitHead = Counter(2)
  val wrapBitTail = Counter(2)

  // wrap signal
  val wrap = (depth - 1).U

  // full and empty conditions
  val full = WireDefault(0.B)
  val empty = WireDefault(0.B)

  when(head.value === tail.value) {
    when(wrapBitHead.value === wrapBitTail.value) {
      empty := 1.B
    }.otherwise {
      full := 1.B
    }
  }

  // write operation
  when(io.enq.valid && !full) {
    mem(head.value) := io.enq.bits
    when(head.value === wrap) {
      head.reset()
      wrapBitHead.inc()
    }.otherwise {
      head.inc()
    }
  }

  // read operation
  io.deq.bits := mem(tail.value)
  when(io.deq.valid && !empty) {
    when(tail.value === wrap) {
      tail.reset()
      wrapBitTail.inc()
    }.otherwise {
      tail.inc()
    }
  }

  io.enq.ready := !full
  io.deq.valid := !empty
}