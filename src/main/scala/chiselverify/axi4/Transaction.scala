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
import chisel3.experimental.BundleLiterals._
import chisel3.internal.requireIsHardware

/** Transaction superclass */
trait Transaction {
  def complete: Boolean
}

/** Write transaction
 *
 * @param ctrl an initialized WA object
 * @param dw a WD object representing the write data channel
 * @param data a sequence of data to write
 */
class WriteTransaction(
  val ctrl: WA,
  val dw: WD,
  val data: Seq[BigInt]) extends Transaction {
  requireIsHardware(ctrl, "ctrl must be an initialized WA object")
  private[this] val numBytes = 1 << ctrl.size.litValue.intValue
  private[this] val dtsize = numBytes * data.length
  private[this] val lowerBoundary = (ctrl.addr.litValue / dtsize) * dtsize
  private[this] val upperBoundary = lowerBoundary + dtsize
  private[this] val alignedAddress = ((ctrl.addr.litValue / numBytes) * numBytes)
  private[this] var aligned = ctrl.addr.litValue == alignedAddress
  private[this] var address = ctrl.addr.litValue
  private[this] var count = 0

  private[this] var _addrSent = false
  private[this] var _dataSent = false

  /** Getter and setter for [[addrSent]] */
  def addrSent = _addrSent
  def addrSent_=(newValue: Boolean): Unit = _addrSent = newValue

  /** Getter and setter for [[dataSent]] */
  def dataSent = _dataSent
  def dataSent_=(newValue: Boolean): Unit = _dataSent = newValue

  /** Get next (data, strb, last) tuple
   * 
   * @return (data, strb, last) tuple
   * 
   * @note has side effect on internal index count
   */
  def next() = {
    /** Strobe calculation */
    val offset = (address / dw.dataW) * dw.dataW
    val lowerByteLane = address - offset
    val upperByteLane = if (aligned) lowerByteLane + numBytes-1 else alignedAddress + numBytes-1 - offset
    def within(x: Int) = x >= 0 && x <= (upperByteLane - lowerByteLane)
    val strb = ("b"+(0 until (dw.dataW/8)).foldRight("") { (elem, acc) => if (within(elem)) acc + "1" else acc + "0" }).asUInt

    /** Update address */
    if (ctrl.burst != BurstEncodings.Fixed) {
      if (aligned) {
        address += numBytes
        if (ctrl.burst == BurstEncodings.Wrap) {
          if (address >= upperBoundary) {
            address = lowerBoundary
          }
        }
      } else {
        address += numBytes
        aligned = true
      }
    }
    count += 1

    /** Return data to write */
    var lits = Seq((x: WD) => x.data -> data(count-1).U, (x: WD) => x.strb -> strb,
      (x: WD) => x.last -> complete.B)
    if (dw.userW > 0) lits = lits :+ ((x: WD) => x.user -> ctrl.user)
    (new WD(dw.dataW, dw.userW)).Lit(lits :_*)
  }
  def complete = data.length == count
}

/** Read transaction 
 * 
 * @param ctrl an initialized RA object
 */
class ReadTransaction(val ctrl: RA) extends Transaction {
  requireIsHardware(ctrl, "ctrl must be an initialized RA object")
  var data = Seq[BigInt]()

  private[this] var _addrSent = false

  /** Getter and setter for [[addrSent]] */
  def addrSent = _addrSent
  def addrSent_=(newValue: Boolean): Unit = _addrSent = newValue

  /** Add element to data sequence
   *
   * @param v value to add
   * 
   * @note has side effect on internal data sequence
   */
  def add(v: BigInt) = {
    data = data :+ v
  }
  def complete = data.length == (ctrl.len.litValue + 1)
}

/** Transaction response
 *
 * @param resp transaction response
 * @param id optional id
 */
case class Response(val resp: UInt, val id: BigInt = 0)
