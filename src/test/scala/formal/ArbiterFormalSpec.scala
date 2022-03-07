package formal

import chisel3._
import chisel3.util._
import chiseltest._
import chiseltest.formal._
import examples.Arbiter
import org.scalatest.flatspec.AnyFlatSpec

class ArbiterFormalSpec extends AnyFlatSpec with ChiselScalatestTester with Formal {
  behavior of "Arbiter"

  it should "maintain data integrity" in {
    verify(new ArbiterDataIntegrityCheck(new Arbiter(3, UInt(8.W)), true), Seq(BoundedCheck(4)))
  }

  it should s"answer request within a certain time frame" in {
    val ports = 3
    verify(new ArbiterFormalFairnessCheck(new Arbiter(ports, UInt(8.W)), true), Seq(BoundedCheck(2 * ports + 4)))
  }
}



private class ArbiterFormalFairnessCheck[D <: Data](makeDut: => Arbiter[D], debugPrint: Boolean = false) extends Module {
  val dut = Module(makeDut)
  val io = IO(chiselTypeOf(dut.io))
  io <> dut.io
  val portCount = dut.io.in.size

  // keep track of how many grants are issued
  val grants = dut.io.in.map(_.fire)
  val grantCount = PopCount(VecInit(grants).asUInt)

  // keep track of how many other grants are issued, while waiting for our own grant
  dut.io.in.zipWithIndex.foreach { case (port, index) =>
    val waitCounter = RegInit(0.U((log2Ceil(portCount + 1) + 1).W)).suggestName(s"wait_counter_$index")
    when(port.valid && !port.ready) {
      waitCounter := waitCounter + grantCount
    }.otherwise {
      waitCounter := 0.U
    }
    assert(waitCounter < portCount.U, s"Port $index has been waiting for a request while %d other requests were issued!", waitCounter)
  }

  if(debugPrint) {
    cyclePrinter(255)
    printGrants(dut.io.in)
  }
}

private class ArbiterDataIntegrityCheck[D <: Data](makeDut: => Arbiter[D], debugPrint: Boolean = false) extends Module {
  val dut = Module(makeDut)
  val io = IO(chiselTypeOf(dut.io))
  io <> dut.io
  val tpe = chiselTypeOf(dut.io.in(0).bits)

  if(debugPrint) { cyclePrinter(255) }

  // our assumption is that only one request will be granted by the arbiter in a single cycle
  val grants = VecInit(dut.io.in.map(_.fire)).asUInt
  assume(grants === 0.U || PopCount(grants) === 1.U, "More than one grant in a single cycle! %b", grants)

  // if only one grant is granted each cycle, we can treat the arbiter like a fifo with a single input
  val enq = Wire(ValidIO(tpe))
  enq.valid := grants.orR
  enq.bits := Mux1H(dut.io.in.map(i => i.fire -> i.bits))

  // track packets
  val deq = Wire(ValidIO(tpe))
  deq.valid := dut.io.out.fire
  deq.bits := dut.io.out.bits
  MagicPacketTracker(enq, deq, depth = 4, debugPrint = debugPrint)

  if(debugPrint) {
    printGrants(dut.io.in)
  }
}

private object printGrants {
  def apply[T <: Data](in: Vec[DecoupledIO[T]]): Unit = {
    in.zipWithIndex.foreach { case (port, index) =>
      when(port.fire) {
        printf(s"P$index: granted (%x)\n", port.bits.asUInt)
      }.elsewhen(port.valid) {
        printf(s"P$index: waiting\n")
      }
    }
  }
}

/** Helper function to make tests fail artificially after a fixed number of cycles */
private object failAfter {
  def apply(n: Int): Unit = {
    require(n > 0)
    val failCount = RegInit(n.U)
    failCount := failCount - 1.U
    assert(failCount =/= 0.U, s"failure triggered after $n cycles")
  }
}

private object cyclePrinter {
  def apply(maxCycles: Int): Unit = {
    require(maxCycles > 0)
    val counter = RegInit(0.U(log2Ceil(maxCycles + 1).W))
    counter := counter + 1.U
    printf(p"$counter ----------------------------\n")
  }
}