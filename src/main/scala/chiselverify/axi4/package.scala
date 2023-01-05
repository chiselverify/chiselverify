package chiselverify

import chisel3._
import chisel3.experimental.ChiselEnum

package object axi4 {
  /** 
    * AXI4-Lite manager
    * @param addrW the width of the address signals in bits
    * @param dataW the width of the data read/write signals in bits
    * 
    * An empty class representing an AXI manager
    */
  abstract class LiteManager(val addrW: Int, val dataW: Int) extends Module {
    val io = IO(new ManagerInterfaceLite(addrW, dataW))
  }

  /** 
    * AXI4 full manager
    * @param addrW the width of the address signals in bits
    * @param dataW the width of the data read/write signals in bits
    * @param idW [Optiona] the width of the ID signals in bits
    * @param userW [Optional] the width of the user signals in bits
    * 
    * An empty class representing an AXI manager
    */
  abstract class Manager(val addrW: Int, val dataW: Int, val idW: Int = 0, val userW: Int = 0) extends Module {
    val io = IO(new ManagerInterface(addrW, dataW, idW, userW))
  }

  /** 
    * AXI4-Lite subordinate
    * @param addrW the width of the address signals in bits
    * @param dataW the width of the data read/write signals in bits
    * 
    * An empty class representing an AXI subordinate
    */
  abstract class LiteSubordinate(val addrW: Int, val dataW: Int) extends Module {
    val io = IO(new SubordinateInterfaceLite(addrW, dataW))
  }

  /** 
    * AXI4 full subordinate
    * @param addrW the width of the address signals in bits
    * @param dataW the width of the data read/write signals in bits
    * @param idW [Optional] the width of the ID signals in bits
    * @param userW [Optional] the width of the user signals in bits
    * 
    * An empty class representing an AXI subordinate
    */
  abstract class Subordinate(val addrW: Int, val dataW: Int, val idW: Int = 0, val userW: Int = 0) extends Module {
    val io = IO(new SubordinateInterface(addrW, dataW, idW, userW))
  }

  /** AXI4 burst encodings */
  object BurstEncodings extends ChiselEnum {
    val Fixed = Value("b00".U)
    val Incr  = Value("b01".U)
    val Wrap  = Value("b10".U)
  }
  
  /** AXI lock encodings */
  object LockEncodings extends ChiselEnum {
    val NormalAccess    = Value(false.B)
    val ExclusiveAccess = Value(true.B)
  }
  
  /** AXI4 memory encodings */
  object MemoryEncodings extends ChiselEnum {
    val DeviceNonbuf = Value("b0000".U)
    val DeviceBuf    = Value("b0001".U)
    val NormalNonbuf = Value("b0010".U)
    val NormalBuf    = Value("b0011".U)
    val WtNoalloc    = Value("b0110".U)
    val WtReadalloc  = Value("b0110".U)
    val WtWritealloc = Value("b1110".U)
    val WtRwalloc    = Value("b1110".U)
    val WbNoalloc    = Value("b0111".U)
    val WbReadalloc  = Value("b0111".U)
    val WbWritealloc = Value("b1111".U)
    val WbRwalloc    = Value("b1111".U)
  }
  
  /** AXI4 protection encodings */
  object ProtectionEncodings extends ChiselEnum {
    val DataSecUpriv   = Value("b000".U)
    val DataSecPriv    = Value("b001".U)
    val DataNsecUpriv  = Value("b010".U)
    val DataNsecPriv   = Value("b011".U)
    val InstrSecUpriv  = Value("b100".U)
    val InstrSecPriv   = Value("b101".U)
    val InstrNsecUpriv = Value("b110".U)
    val InstrNsecPriv  = Value("b111".U)
  }
  
  /** AXI4 response encodings */
  object ResponseEncodings extends ChiselEnum {
    val Okay   = Value("b00".U)
    val Exokay = Value("b01".U)
    val Slverr = Value("b10".U)
    val Decerr = Value("b11".U)
  }
}
