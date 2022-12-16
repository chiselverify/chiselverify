package chiselverify.axi4

import chisel3._
import chisel3.util._
import chisel3.experimental.BundleLiterals._
import chiseltest._
import chiseltest.internal.TesterThreadList
import scala.util.Random
import scala.collection.mutable

/** 
  * An AXI4 functional manager
  * @param dut a subordinate DUT
  */
class FunctionalManager[T <: Subordinate](dut: T) {
  /** DUT information */
  private[this] val idW   = dut.idW
  private[this] val addrW = dut.addrW
  private[this] val dataW = dut.dataW

  /** Shortcuts to the channel IO */
  private[this] val wa     = dut.io.wa
  private[this] val wd     = dut.io.wd
  private[this] val wr     = dut.io.wr
  private[this] val ra     = dut.io.ra
  private[this] val rd     = dut.io.rd
  private[this] val resetn = dut.reset
  private[this] val clk    = dut.clock

  /** Threads and transaction state */
  // For writes
  private[this] val awaitingWAddr = mutable.ArrayBuffer[WriteTransaction]()
  private[this] val awaitingWrite = mutable.ArrayBuffer[WriteTransaction]()
  private[this] val awaitingResp  = mutable.ArrayBuffer[WriteTransaction]()
  private[this] val responses     = mutable.ArrayBuffer[Response]()
  private[this] var wAddrT: TesterThreadList = _
  private[this] var writeT: TesterThreadList = _
  private[this] var respT:  TesterThreadList = _
  // For reads
  private[this] val awaitingRAddr = mutable.ArrayBuffer[ReadTransaction]()
  private[this] val awaitingRead  = mutable.ArrayBuffer[ReadTransaction]()
  private[this] val readValues    = mutable.ArrayBuffer[Seq[BigInt]]()
  private[this] var rAddrT: TesterThreadList = _
  private[this] var readT:  TesterThreadList = _
  // For random data
  private[this] val rng = new Random(42)

  /** Default values on all signals */
  // Address write
  wa.bits.pokePartial(WA.default(wa.bits))
  wa.valid.poke(false.B)
  
  // Data write
  wd.bits.pokePartial(WD.default(wd.bits))
  wd.valid.poke(false.B)

  // Write response
  wr.ready.poke(false.B)

  // Address read
  ra.bits.pokePartial(RA.default(ra.bits))
  ra.valid.poke(false.B)

  // Data read
  rd.ready.poke(false.B)

  // Reset subordinate device controller
  resetn.poke(false.B)
  clk.step()
  resetn.poke(true.B)

  /** 
    * Checks for in-flight operations
    * @return true if one of the internal waiting queues is non-empty
    */
  def hasInflightOps() = !awaitingWAddr.isEmpty || !awaitingWrite.isEmpty || !awaitingResp.isEmpty || !awaitingRAddr.isEmpty || !awaitingRead.isEmpty

  /** 
    * Checks for responses or read data
    * @return true if one of the internal response waiting queues is non-empty
    */
  def hasRespOrReadData() = !responses.isEmpty || !readValues.isEmpty

  /** 
    * Handles the write address channel
    * @note never call this method explicitly
    */
  private[this] def writeAddrHandler(): Unit = {
    println("New write address handler")

    // Run this thread as long as the manager is initialized or more transactions are waiting
    while (!awaitingWAddr.isEmpty) {
      // Get the current transaction
      val head = awaitingWAddr.head

      // Write address to subordinate
      wa.valid.poke(true.B)
      wa.bits.pokePartial(head.ctrl)
      while (!wa.ready.peek().litToBoolean) clk.step()
      clk.step()
      wa.valid.poke(false.B) 

      // Update transaction and queue
      awaitingWAddr.remove(0)
      head.addrSent = true
    }
    println("Closing write address handler")
  }

  /** 
    * Handles the data write channel
    * @note never call this method explicitly
    */
  private[this] def writeHandler(): Unit = {
    println("New write handler")

    // Run this thread as long as the manager is initialized or more transactions are waiting
    while (!awaitingWrite.isEmpty) {
      // Get the current transaction
      val head = awaitingWrite.head
      while (!head.addrSent) clk.step()

      // Write data to subordinate
      wd.valid.poke(true.B)
      while (!head.complete) {
        val nextVal = head.next()
        wd.bits.pokePartial(nextVal)
        println("Write " + nextVal.data.litValue + " with strobe " + nextVal.strb.toString + " and last " + nextVal.last.litToBoolean)
        while (!wd.ready.peek().litToBoolean) clk.step()
        clk.step()
      }
      wd.valid.poke(false.B)

      // Update transaction and queue
      awaitingWrite.remove(0)
      head.dataSent = true
    }
    println("Closing write handler")
  }

  /** 
    * Watches the response channel
    * @note never call this method explicitly
    */
  private[this] def respHandler() = {
    println("New response handler")

    // Run this thread as long as the manager is initialized or more transactions are waiting
    while (!awaitingResp.isEmpty) {
      // Get the current transaction
      val head = awaitingResp.head
      while (!head.dataSent) clk.step()

      // Indicate that interface is ready and wait for response
      wr.ready.poke(true.B)
      while (!wr.valid.peek().litToBoolean) clk.step()
      responses += new Response(wr.bits.resp.peek(), if (wr.bits.idW > 0) wr.bits.id.peek().litValue else 0)
      wr.ready.poke(false.B)

      // Update queue
      awaitingResp.remove(0)
    }
    println("Closing response handler")
  }

  /** 
    * Handles the read address channel
    * @note never call this method explicitly
    */
  private[this] def readAddrHandler(): Unit = {
    println("New read address handler")

    // Run this thread as long as the manager is initialized or more transactions are waiting
    while (!awaitingRAddr.isEmpty) {
      // Get the current transaction
      val head = awaitingRAddr.head 

      // Write address to subordinate
      ra.valid.poke(true.B)
      ra.bits.pokePartial(head.ctrl)
      while (!ra.ready.peek().litToBoolean) clk.step()
      clk.step()
      ra.valid.poke(false.B)

      // Update transaction and queue
      awaitingRAddr.remove(0)
      head.addrSent = true
    }
    println("Closing read address handler")
  }

  /** 
    * Handles the data read channel
    * @note never call this method explicitly
    */
  private[this] def readHandler(): Unit = {
    println("New read handler")

    // Run this thread as long as the manager is initialized or more transactions are waiting
    while (!awaitingRead.isEmpty) {
      // Get the current transaction
      val head = awaitingRead.head
      while (!head.addrSent) clk.step()

      // Read data from subordinate
      rd.ready.poke(true.B)
      while (!head.complete) {
        if (rd.valid.peek().litToBoolean) {
          val (data, resp, last) = (rd.bits.data.peek(), rd.bits.resp.peek(), rd.bits.last.peek())
          println(s"Read " + data.litValue + " with response " + resp.litValue + " and last " + last.litToBoolean)
          head.add(data.litValue)
        }
        clk.step()
      }
      readValues += head.data
      rd.ready.poke(false.B)

      // Update queue
      awaitingRead.remove(0)
    }
    println("Closing read handler")
  }

  /** 
    * Destructor joins all non-null thread pointers and checks for responses and read data waiting in queues
    */
  override def finalize() = {
    // Join handlers
    if (wAddrT != null) wAddrT.join()
    if (writeT != null) writeT.join()
    if (respT  != null) respT.join()
    if (rAddrT != null) rAddrT.join()
    if (readT  != null) readT.join()

    // Check for unchecked responses and read data
    if (hasRespOrReadData()) println(s"WARNING: manager had ${responses.length} responses and ${readValues.length} Seq's of read data waiting")
  }

  /** 
    * Starts a write transaction to the given address
    * @param addr start write address
    * @param data [Optional] list of data to write
    * @param id [Optional] id
    * @param len [Optional] burst length
    * @param size [Optional] beat size
    * @param burst [Optional] burst type
    * @param lock [Optional] lock type
    * @param cache [Optional] memory attribute signal
    * @param prot [Optional] protection type
    * @param qos [Optional] QoS
    * @param region [Optional] region
    * @param user [Optional] user
    * 
    * @note Arguments must satisfy the following criteria:
    * - [[addr]] must fit within the subordinate DUT's write address width
    * - entries in [[data]] must fit within the subordinate DUT's write data width, and the list can have at most [[len]] entries
    * - [[id]] must fit within DUT's ID width, likewise [[size]] cannot be greater than the DUT's write data width
    * - [[burst]], [[lock]], [[cache]], and [[prot]] should be a set of those defined in Defs.scala
    */
  def createWriteTrx(
    addr: BigInt, 
    data: Seq[BigInt] = Seq[BigInt](), 
    id: BigInt = 0, 
    len: Int = 0, 
    size: Int = 0, 
    burst: UInt = BurstEncodings.Fixed.asUInt, 
    lock: Bool = LockEncodings.NormalAccess.asUInt(0), // <- extracts a Bool
    cache: UInt = MemoryEncodings.DeviceNonbuf.asUInt, 
    prot: UInt = ProtectionEncodings.DataNsecUpriv.asUInt, 
    qos: UInt = 0.U, 
    region: UInt = 0.U,
    user: UInt = 0.U) = {
    require(log2Up(addr) <= addrW, s"address must fit within DUT's write address width (got $addr)")
    require(log2Up(id) <= idW, s"ID must fit within DUT's ID width (got $id)")

    /** 
      * [[len]] and [[size]] checks
      * - [[size]] must be less than or equal to the write data width
      * - [[len]] must be <= 15 for FIXED and WRAP transactions, only INCR can go beyond
      * - Bursts cannot cross 4KB boundaries
      */
    val startAddr = addr
    val numBytes  = 1 << size
    val burstLen  = len + 1
    val alignedAddr = (startAddr / numBytes) * numBytes
    val wrapBoundary = (startAddr / (numBytes * burstLen)) * (numBytes * burstLen)
    require(numBytes <= dataW, s"size must be less than or equal to the write data width")
    burst match {
      case BurstEncodings.Fixed =>
        require(burstLen <= 16, s"len for FIXED transactions must be less than or equal to 15 (got $len)")
        require(((startAddr + numBytes) >> 12) == (startAddr >> 12), "burst cannot cross 4KB boundary")
      case BurstEncodings.Incr =>
        require(burstLen <= 256, s"len for INCR transactions must be less than or equal to 255 (got $len)")
        require(((startAddr + numBytes * burstLen) >> 12) == (startAddr >> 12), "burst cannot cross 4KB boundary")
      case BurstEncodings.Wrap =>
        require(burstLen <= 16, s"len for WRAP transactions must be less than or equal to 15 (got $len)")
        require((startAddr >> 12) == (wrapBoundary >> 12), "burst cannot cross 4KB boundary")
      case _ => throw new IllegalArgumentException("invalid burst type entered")
    }

    // Select data
    val tdata = if (data != Nil) {
      require(data.length == burstLen, "given data length should match burst length")
      data
    } else Seq.fill(burstLen) { BigInt(numBytes, rng) }

    // Create and queue new write transaction
    var lits = Seq((x: WA) => x.addr -> addr.U, (x: WA) => x.len -> len.U, (x: WA) => x.size -> size.U,
      (x: WA) => x.burst -> burst, (x: WA) => x.lock -> lock, (x: WA) => x.cache -> cache,
      (x: WA) => x.prot -> prot, (x: WA) => x.qos -> qos, (x: WA) => x.region -> region)
    if (wa.bits.idW > 0) lits = lits :+ ((x: WA) => x.id -> id.U)
    if (wa.bits.userW > 0) lits = lits :+ ((x: WA) => x.user -> user)
    val trx = new WriteTransaction((new WA(wa.bits.addrW, wa.bits.idW, wa.bits.userW)).Lit(lits :_*), wd.bits, data)
    awaitingWAddr += trx
    awaitingWrite += trx
    awaitingResp  += trx

    // If this was the first transaction, fork new handlers
    if (awaitingWAddr.length == 1) wAddrT = fork { writeAddrHandler() }
    if (awaitingWrite.length == 1) writeT = fork { writeHandler() }
    if (awaitingResp.length  == 1) respT  = fork { respHandler() }
  }

  /** 
    * Starts a read transaction from the given address
    * @param addr start read address
    * @param id [Optional] id
    * @param len [Optional] burst length
    * @param size [Optional] beat size
    * @param burst [Optional] burst type
    * @param lock [Optional] lock type
    * @param cache [Optional] memory attribute signal
    * @param prot [Optional] protection type
    * @param qos [Optional] QoS
    * @param region [Optional] region
    * @param user [Optional] user
    * 
    * @note Arguments must satisfy the following criteria:
    * - [[addr]] must fit within the subordinate DUT's write address width
    * - [[id]] must fit within DUT's ID width, likewise [[size]] cannot be greater than the DUT's write data width
    * - [[burst]], [[lock]], [[cache]], and [[prot]] should be a set of those defined in Defs.scala
    */
  def createReadTrx(
    addr: BigInt, 
    id: BigInt = 0, 
    len: Int = 0, 
    size: Int = 0, 
    burst: UInt = BurstEncodings.Fixed.asUInt, 
    lock: Bool = LockEncodings.NormalAccess.asUInt(0), // <- extracts a Bool
    cache: UInt = MemoryEncodings.DeviceNonbuf.asUInt, 
    prot: UInt = ProtectionEncodings.DataNsecUpriv.asUInt, 
    qos: UInt = 0.U, 
    region: UInt = 0.U,
    user: UInt = 0.U) = {
    require(log2Up(addr) <= addrW, s"address must fit within DUT's write address width (got $addr)")
    require(log2Up(id) <= idW, s"ID must fit within DUT's ID width (got $id)")

    /** 
      * [[len]] and [[size]] checks
      * - [[size]] must be less than or equal to the write data width
      * - [[len]] must be <= 15 for FIXED and WRAP transactions, only INCR can go beyond
      * - Bursts cannot cross 4KB boundaries
      */
    val startAddr = addr
    val numBytes  = 1 << size
    val burstLen  = len + 1
    val alignedAddr = (startAddr / numBytes) * numBytes
    val wrapBoundary = (startAddr / (numBytes * burstLen)) * (numBytes * burstLen)
    require(numBytes <= dataW, s"size must be less than or equal to the write data width")
    burst match {
      case BurstEncodings.Fixed =>
        require(burstLen <= 16, s"len for FIXED transactions must be less than or equal to 15 (got $len)")
        require(((startAddr + numBytes) >> 12) == (startAddr >> 12), "burst cannot cross 4KB boundary")
      case BurstEncodings.Incr =>
        require(burstLen <= 256, s"len for INCR transactions must be less than or equal to 255 (got $len)")
        require(((startAddr + numBytes * burstLen) >> 12) == (startAddr >> 12), "burst cannot cross 4KB boundary")
      case BurstEncodings.Wrap =>
        require(burstLen <= 16, s"len for WRAP transactions must be less than or equal to 15 (got $len)")
        require((startAddr >> 12) == (wrapBoundary >> 12), "burst cannot cross 4KB boundary")
      case _ => throw new IllegalArgumentException("invalid burst type entered")
    }

    // Create and queue new read transaction
    var lits = Seq((x: RA) => x.addr -> addr.U, (x: RA) => x.len -> len.U, (x: RA) => x.size -> size.U,
      (x: RA) => x.burst -> burst, (x: RA) => x.lock -> lock, (x: RA) => x.cache -> cache,
      (x: RA) => x.prot -> prot, (x: RA) => x.qos -> qos, (x: RA) => x.region -> region)
    if (ra.bits.idW > 0) lits = lits :+ ((x: RA) => x.id -> id.U)
    if (ra.bits.userW > 0) lits = lits :+ ((x: RA) => x.user -> user)
    val trx = new ReadTransaction((new RA(ra.bits.addrW, ra.bits.idW, ra.bits.userW)).Lit(lits :_*))
    awaitingRAddr += trx
    awaitingRead  += trx

    // If this was the first transaction, fork new handlers
    if (awaitingRAddr.length == 1) rAddrT = fork { readAddrHandler() }
    if (awaitingRead.length  == 1) readT  = fork { readHandler() }
  }

  /** 
    * Checks for write response
    * @note write responses are continuously stored in an internal queue by a second thread
    * @note reading is destructive; i.e., the response being checked is removed from the queue
    */
  def responseOption() = {
    responses.headOption match {
      case Some(r) => 
        responses.remove(0)
        Some(r)
      case _ => None
    }
  }

  /** 
    * Checks for read data
    * @note read values are continuously stored in an internal queue by a second thread spawned when creating a new read transaction
    * @note reading is destructive; i.e., the data being returned is removed from the queue
    */
  def readDataOption() = {
    readValues.headOption match {
      case Some(r) => 
        readValues.remove(0)
        Some(r)
      case _ => None
    }
  }
}
