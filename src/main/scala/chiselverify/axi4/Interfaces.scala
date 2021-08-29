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

package chiselverify.axi4

import chisel3._
import chisel3.util.Decoupled

/** AXI4-Lite manager interface
 * 
 * @param addrW the width of the address signals in bits
 * @param dataW the width of the data read/write signals in bits
 */
class ManagerInterfaceLite(val addrW: Int, val dataW: Int) extends Bundle {
  /** Fields implementing each of the AXI channels
   * 
   * [[wa]] is the write address channel
   * [[wd]] is the write data channel
   * [[wr]] is the write response channel
   * [[ra]] is the read address channel
   * [[rd]] is the read data channel
   */
  val wa = Decoupled(Output(WALite(addrW)))
  val wd = Decoupled(Output(WDLite(dataW)))
  val wr = Flipped(Decoupled(Output(WRLite())))
  val ra = Decoupled(RALite(addrW))
  val rd = Flipped(Decoupled(Output(RDLite(dataW))))
}

/** AXI4 full manager interface
 * 
 * @param addrW the width of the address signals in bits
 * @param dataW the width of the data read/write signals in bits
 * @param idW the width of the ID signals in bits, defaults to 0
 * @param userW the width of the user signals in bits, defaults to 0
 */
class ManagerInterface(override val addrW: Int, override val dataW: Int, val idW: Int = 0, val userW: Int = 0) extends ManagerInterfaceLite(addrW, dataW) {
  /** Fields implementing each of the AXI channels
   * 
   * [[wa]] is the write address channel
   * [[wd]] is the write data channel
   * [[wr]] is the write response channel
   * [[ra]] is the read address channel
   * [[rd]] is the read data channel
   */
  override val wa = Decoupled(Output(WA(addrW, idW, userW)))
  override val wd = Decoupled(Output(WD(dataW, userW)))
  override val wr = Flipped(Decoupled(Output(WR(idW, userW))))
  override val ra = Decoupled(RA(addrW, idW, userW))
  override val rd = Flipped(Decoupled(Output(RD(dataW, idW, userW))))
}

/** AXI4-Lite subordinate interface
 * 
 * @param addrW the width of the address signals in bits
 * @param dataW the width of the data read/write signals in bits
 */
class SubordinateInterfaceLite(val addrW: Int, val dataW: Int) extends Bundle {  
  /** Fields implementing each of the AXI channels
   * 
   * [[wa]] is the write address channel
   * [[wd]] is the write data channel
   * [[wr]] is the write response channel
   * [[ra]] is the read address channel
   * [[rd]] is the read data channel
   */
  val wa = Flipped(Decoupled(Output(WALite(addrW))))
  val wd = Flipped(Decoupled(Output(WDLite(dataW))))
  val wr = Decoupled(Output(WRLite()))
  val ra = Flipped(Decoupled(Output(RALite(addrW))))
  val rd = Decoupled(Output(RDLite(dataW)))
}

/** AXI4 full subordinate interface
 * 
 * @param addrW the width of the address signals in bits
 * @param dataW the width of the data read/write signals in bits
 * @param idW the width of the ID signals in bits, defaults to 0
 * @param userW the width of the user signals in bits, defaults to 0
 */
class SubordinateInterface(override val addrW: Int, override val dataW: Int, val idW: Int = 0, val userW: Int = 0) extends SubordinateInterfaceLite(addrW, dataW) {  
  /** Fields implementing each of the AXI channels
   * 
   * [[wa]] is the write address channel
   * [[wd]] is the write data channel
   * [[wr]] is the write response channel
   * [[ra]] is the read address channel
   * [[rd]] is the read data channel
   */
  override val wa = Flipped(Decoupled(Output(WA(addrW, idW, userW))))
  override val wd = Flipped(Decoupled(Output(WD(dataW, userW))))
  override val wr = Decoupled(Output(WR(idW, userW)))
  override val ra = Flipped(Decoupled(Output(RA(addrW, idW, userW))))
  override val rd = Decoupled(Output(RD(dataW, idW, userW)))
}
