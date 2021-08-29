/*
* Copyright 2020 DTU Compute - Section for Embedded Systems Engineering
*
* Licensed under the Apache License, Version 2.0 (the "License");
* you may not use this file except in compliance with the License.
* You may obtain a copy of the License at
*
* http://www.apache.org/licenses/LICENSE-2.0
*
* Unless required by applicable law or agreed to in writing, software
* distributed under the License is distributed on an "AS IS" BASIS,
* WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express
* or implied. See the License for the specific language governing
* permissions and limitations under the License.
*/

package chiselverify

import chisel3._

package object axi4 {
  /** AXI4-Lite manager
   * 
   * An empty class representing an AXI manager
   *
   * @param addrW the width of the address signals in bits
   * @param dataW the width of the data read/write signals in bits
   */
  abstract class LiteManager(val addrW: Int, val dataW: Int) extends Module {
    val io = IO(new ManagerInterfaceLite(addrW, dataW))
  }

  /** AXI4 full manager
   * 
   * An empty class representing an AXI manager
   *
   * @param addrW the width of the address signals in bits
   * @param dataW the width of the data read/write signals in bits
   * @param idW the width of the ID signals in bits, defaults to 0
   * @param userW the width of the user signals in bits, defaults to 0
   */
  abstract class Manager(val addrW: Int, val dataW: Int, val idW: Int = 0, val userW: Int = 0) extends Module {
    val io = IO(new ManagerInterface(addrW, dataW, idW, userW))
  }

  /** AXI4-Lite subordinate
   * 
   * An empty class representing an AXI subordinate
   *
   * @param addrW the width of the address signals in bits
   * @param dataW the width of the data read/write signals in bits
   */
  abstract class LiteSubordinate(val addrW: Int, val dataW: Int) extends Module {
    val io = IO(new SubordinateInterfaceLite(addrW, dataW))
  }

  /** AXI4 full subordinate
   * 
   * An empty class representing an AXI subordinate
   *
   * @param addrW the width of the address signals in bits
   * @param dataW the width of the data read/write signals in bits
   * @param idW the width of the ID signals in bits, defaults to 0
   * @param userW the width of the user signals in bits, defaults to 0
   */
  abstract class Subordinate(val addrW: Int, val dataW: Int, val idW: Int = 0, val userW: Int = 0) extends Module {
    val io = IO(new SubordinateInterface(addrW, dataW, idW, userW))
  }

  /** AXI4 burst encodings */
  object BurstEncodings {
    val Fixed             = "b00".U
    val Incr              = "b01".U
    val Wrap              = "b10".U
  }
  
  /** AXI lock encodings */
  object LockEncodings {
    val NormalAccess     = false.B
    val ExclusiveAccess  = true.B
  }
  
  /** AXI4 memory encodings */
  object MemoryEncodings {
    val DeviceNonbuf     = "b0000".U
    val DeviceBuf        = "b0001".U
    val NormalNonbuf     = "b0010".U 
    val NormalBuf        = "b0011".U
    val WtNoalloc        = "b0110".U
    val WtReadalloc      = "b0110".U
    val WtWritealloc     = "b1110".U
    val WtRwalloc        = "b1110".U
    val WbNoalloc        = "b0111".U
    val WbReadalloc      = "b0111".U
    val WbWritealloc     = "b1111".U
    val WbRwalloc        = "b1111".U
  }
  
  /** AXI4 protection encodings */
  object ProtectionEncodings {
    val DataSecUpriv    = "b000".U
    val DataSecPriv     = "b001".U
    val DataNsecUpriv   = "b010".U
    val DataNsecPriv    = "b011".U
    val InstrSecUpriv   = "b100".U
    val InstrSecPriv    = "b101".U
    val InstrNsecUpriv  = "b110".U
    val InstrNsecPriv   = "b111".U
  }
  
  /** AXI4 response encodings */
  object ResponseEncodings {
    val Okay              = "b00".U
    val Exokay            = "b01".U
    val Slverr            = "b10".U
    val Decerr            = "b11".U
  }
}
