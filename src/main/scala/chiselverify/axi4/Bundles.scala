package chiselverify.axi4

import chisel3._
import chisel3.util.isPow2
import chisel3.experimental.BundleLiterals._

/** AXI4 Lite write address
  * 
  * @param addrW the width of the AWADDR signal in bits
  */
class WALite(val addrW: Int) extends Bundle {
  require(addrW > 0, "the address width must be a positive integer")
  val addr = UInt(addrW.W)
  val prot = UInt(3.W)
}

object WALite {
  /** 
    * Creates a new WALite interface given an address width
    * @param addrW the width of the AWADDR signal in bits
    * @return an unitialized WALite object
    */
  def apply(addrW: Int) = new WALite(addrW)

  /** 
    * Default values for this channel
    * @param in a WALite object
    * @return an initialized (hardware) WALite object
    */
  def default(in: WALite) = (new WALite(in.addrW)).Lit(_.addr -> 0.U, _.prot -> ProtectionEncodings.DataNsecUpriv.asUInt)
}

/** 
  * AXI4 full write address
  * @param addrW the width of the AWADDR signal in bits
  * @param idW the width of the AWID signal in bits
  * @param userW the width of the AWUSER signal in bits
  */
class WA(addrW: Int, val idW: Int, val userW: Int) extends WALite(addrW) {
  require(idW >= 0, "the id width must be a non-negative integer")
  require(userW >= 0, "the user with must be a non-negative integer")
  val id     = UInt(idW.W)
  val len    = UInt(8.W)
  val size   = UInt(3.W)
  val burst  = UInt(2.W)
  val lock   = Bool()
  val cache  = UInt(4.W)
  val qos    = UInt(4.W)
  val region = UInt(4.W)
  val user   = UInt(userW.W)
}

object WA {
  /** 
    * Creates a new WA interface given an address width
    * @param addrW the width of the AWADDR signal in bits
    * @param idW [Optional] the width of the AWID signal in bits
    * @param userW [Optional] the width of the AWUSER signal in bits
    * @return an unitialized WA object
    */
  def apply(addrW: Int, idW: Int = 0, userW: Int = 0) = new WA(addrW, idW, userW)

  /** 
    * Default values for this channel
    * @param in a WA object
    * @return an initialized (hardware) WA object
    */
  def default(in: WA) = {
    var defLits = Seq((x: WA) => x.addr -> 0.U, (x: WA) => x.len -> 0.U, (x: WA) => x.size -> 0.U, 
      (x: WA) => x.burst -> BurstEncodings.Fixed.asUInt, (x: WA) => x.lock -> LockEncodings.NormalAccess.asUInt, 
      (x: WA) => x.cache -> MemoryEncodings.DeviceNonbuf.asUInt, (x: WA) => x.prot -> ProtectionEncodings.DataNsecUpriv.asUInt,
      (x: WA) => x.qos -> 0.U, (x: WA) => x.region -> 0.U)
    if (in.idW > 0) defLits = defLits :+ ((x: WA) => x.id -> 0.U)
    if (in.userW > 0) defLits = defLits :+ ((x: WA) => x.user -> 0.U)
    (new WA(in.addrW, in.idW, in.userW)).Lit(defLits :_*)
  }
}

/** 
  * AXI4 Lite write data
  * @param dataW the width of the WDATA signal in bits
  */
class WDLite(val dataW: Int) extends Bundle {
  require(dataW == 32 || dataW == 64, "the data width must be either 32 or 64 bits")
  val data = UInt(dataW.W)
  val strb = UInt((dataW/8).W)
}

object WDLite {
  /** 
    * Creates a new WDLite interface given an address width
    * @param dataW [Optional] the width of the WDATA signal in bits
    * @return an unitialized WDLite object
    */
  def apply(dataW: Int = 32) = new WDLite(dataW)

  /** 
    * Default values for this channel
    * @param in a WDLite object
    * @return an initialized (hardware) WDLite object
    */
  def default(in: WDLite) = (new WDLite(in.dataW)).Lit(_.data -> 0.U, _.strb -> 0.U)
}

/** 
  * AXI4 full write data
  * @param dataW the width of the WDATA signal in bits
  * @param userW the width of the WUSER signal in bits
  */
class WD(dataW: Int, val userW: Int) extends WDLite(dataW) {
  require(dataW > 0, "the data width must be a positive integer")
  require(isPow2(dataW / 8), "the data width must be a power of 2 multiple of bytes")
  require(userW >= 0, "the user with must be a non-negative integer")
  val last = Bool()
  val user = UInt(userW.W)
}

object WD {
  /** 
    * Creates a new WD interface given a data width
    * @param dataW the width of the WDATA signal in bits
    * @param userW [Optional] the width of the WUSER signal in bits
    * @return an unitialized WD object
    */
  def apply(dataW: Int, userW: Int = 0) = new WD(dataW, userW)

  /** 
    * Default values for this channel
    * @param in a WD object
    * @return an initialized (hardware) WD object
    */
  def default(in: WD) = {
    var defLits = Seq((x: WD) => x.data -> 0.U, (x: WD) => x.strb -> 0.U, (x: WD) => x.last -> false.B)
    if (in.userW > 0) defLits = defLits :+ ((x: WD) => x.user -> 0.U)
    (new WD(in.dataW, in.userW)).Lit(defLits :_*)
  }
}

/** 
  * AXI4 Lite write response
  * @param idW the width of the BID signal in bits
  */
class WRLite extends Bundle {
  val resp = UInt(2.W)
}

object WRLite {
  /** 
    * Creates a new WRLite interface
    * @return an unitialized WRLite object
    */
  def apply() = new WRLite()

  /** 
    * Default values for this channel
    * @param in a WRLite object
    * @return an initialized (hardware) WRLite object
    */
  def default(in: WRLite) = (new WRLite()).Lit(_.resp -> ResponseEncodings.Okay.asUInt)
}

/** 
  * AXI4 full write response
  * @param idW the width of the BID signal in bits
  * @param userW the width of the BUSER signal in bits
  */
class WR(val idW: Int, val userW: Int) extends WRLite {
  require(idW >= 0, "the id width must be a non-negative integer")
  require(userW >= 0, "the user with must be a non-negative integer")
  val id   = UInt(idW.W)
  val user = UInt(userW.W)
}

object WR {
  /** 
    * Creates a new WR interface
    * @param idW [Optional] the width of the BID signal in bits
    * @param userW [Optional] the width of the BUSER signal in bits
    * @return an unitialized WR object
    */
  def apply(idW: Int = 0, userW: Int = 0) = new WR(idW, userW)

  /** 
    * Default values for this channel
    * @param in a WR object
    * @return an initialized (hardware) WR object
    */
  def default(in: WR) = {
    var defLits = Seq((x: WR) => x.resp -> ResponseEncodings.Okay.asUInt)
    if (in.idW > 0) defLits = defLits :+ ((x: WR) => x.id -> 0.U)
    if (in.userW > 0) defLits = defLits :+ ((x: WR) => x.user -> 0.U)
    (new WR(in.idW, in.userW)).Lit(defLits :_*)
  }
}

/** 
  * AXI4 Lite read address
  * @param addrW the width of the ARADDR signal in bits
  */
class RALite(val addrW: Int) extends Bundle {
  require(addrW > 0, "the address width must be a positive integer")
  val addr = UInt(addrW.W)
  val prot = UInt(3.W)
}

object RALite {
  /** 
    * Creates a new RALite interface given an address width
    * @param addrW the width of the ARADDR signal in bits
    * @return an unitialized RALite object
    */
  def apply(addrW: Int) = new RALite(addrW)

  /** 
    * Default values for this channel
    * @param in a RALite object
    * @return an initialized (hardware) RALite object
    */
  def default(in: RALite) = (new RALite(in.addrW)).Lit(_.addr -> 0.U, _.prot -> ProtectionEncodings.DataNsecUpriv.asUInt)
}

/** 
  * AXI4 full read address
  * @param addrW the width of the ARADDR signal in bits
  * @param idW the width of the ARID signal in bits
  * @param userW the width of the ARUSER signal in bits
  */
class RA(addrW: Int, val idW: Int, val userW: Int) extends RALite(addrW) {
  require(idW >= 0, "the id width must be a non-negative integer")
  require(userW >= 0, "the user with must be a non-negative integer")
  val id     = UInt(idW.W)
  val len    = UInt(8.W)
  val size   = UInt(3.W)
  val burst  = UInt(2.W)
  val lock   = Bool()
  val cache  = UInt(4.W)
  val qos    = UInt(4.W)
  val region = UInt(4.W)
  val user   = UInt(userW.W)
}

object RA {
  /** 
    * Creates a new RA interface given an address width
    * @param addrW the width of the ARADDR signal in bits
    * @param idW [Optional] the width of the ARID signal in bits
    * @param userW [Optional] the width of the ARUSER signal in bits
    * @return an unitialized RA object
    */
  def apply(addrW: Int, idW: Int = 0, userW: Int = 0) = new RA(addrW, idW, userW)

  /** 
    * Default values for this channel
    * @param in an RA object
    * @return an initialized (hardware) RA object
    */
  def default(in: RA) = {
    var defLits = Seq((x: RA) => x.addr -> 0.U, (x: RA) => x.len -> 0.U, (x: RA) => x.size -> 0.U, 
      (x: RA) => x.burst -> BurstEncodings.Fixed.asUInt, (x: RA) => x.lock -> LockEncodings.NormalAccess.asUInt, 
      (x: RA) => x.cache -> MemoryEncodings.DeviceNonbuf.asUInt, (x: RA) => x.prot -> ProtectionEncodings.DataNsecUpriv.asUInt,
      (x: RA) => x.qos -> 0.U, (x: RA) => x.region -> 0.U)
    if (in.idW > 0) defLits = defLits :+ ((x: RA) => x.id -> 0.U)
    if (in.userW > 0) defLits = defLits :+ ((x: RA) => x.user -> 0.U)
    (new RA(in.addrW, in.idW, in.userW)).Lit(defLits :_*)
  }
}

/** 
  * AXI4 Lite read data
  * @param dataW the width of the RDATA signal in bits
  * @param idW the width of the RID signal in bits
  * @param userW the width of the RUSER signal in bits
  */
class RDLite(val dataW: Int) extends Bundle {
  require(dataW == 32 || dataW == 64, "the data width must be either 32 or 64 bits")
  val data = UInt(dataW.W)
  val resp = UInt(2.W)
}

object RDLite {
  /** 
    * Creates a new RDLite interface
    * @param dataW [Optional] the width of the RDATA signal in bits
    * @return an uninitialized RDLite object
    */
  def apply(dataW: Int = 32) = new RDLite(dataW)

  /** 
    * Default values for this channel
    * @param in a RDLite object
    * @return an initialized (hardware) RDLite object
    */
  def default(in: RDLite) = (new RDLite(in.dataW)).Lit(_.data -> 0.U, _.resp -> ResponseEncodings.Okay.asUInt)
}

/** 
  * AXI4 full read data
  * @param dataW the width of the RDATA signal in bits
  * @param idW the width of the RID signal in bits
  * @param userW the width of the RUSER signal in bits
  */
class RD(dataW: Int, val idW: Int, val userW: Int) extends RDLite(dataW) {
  require(isPow2(dataW / 8), "the data width must be a power of 2 multiple of bytes")
  require(idW >= 0, "the id width must be a non-negative integer")
  require(userW >= 0, "the user with must be a non-negative integer")
  val id   = UInt(idW.W)
  val last = Bool()
  val user = UInt(userW.W)
}

object RD {
  /** 
    * Creates a new RD interface given a data width
    * @param dataW the width of the RDATA signal in bits
    * @param idW [Optional] the width of the RID signal in bits, defaults to 0
    * @param userW [Optional] the width of the RUSER signal in bits, defaults to 0
    * @return an uninitialized RD object
    */
  def apply(dataW: Int, idW: Int = 0, userW: Int = 0) = new RD(dataW, idW, userW)

  /** 
    * Default values for this channel
    * @param in an RD object
    * @return an initialized (hardware) RD object
    */
  def default(in: RD) = {
    var defLits = Seq((x: RD) => x.data -> 0.U, (x: RD) => x.resp -> ResponseEncodings.Okay.asUInt,
      (x: RD) => x.last -> false.B)
    if (in.idW > 0) defLits = defLits :+ ((x: RD) => x.id -> 0.U)
    if (in.userW > 0) defLits = defLits :+ ((x: RD) => x.user -> 0.U)
    (new RD(in.dataW, in.idW, in.userW)).Lit(defLits :_*)
  }
}
