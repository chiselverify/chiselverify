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

package chiselverify.coverage

import chisel3._
import chisel3.tester.{testableClock, testableData}
import chiselverify.timing._

import scala.collection.mutable.ArrayBuffer

/** Handles everything related to functional coverage
  */
class CoverageReporter[T <: Module](private val dut: T) {
  private val coverGroups: ArrayBuffer[CoverGroup] = new ArrayBuffer[CoverGroup]()
  private val coverageDB:  CoverageDB = new CoverageDB

  /** Makes a readable functional coverage report
    * @return the report in string form
    */
  def report: CoverageReport = CoverageReport(
    coverGroups
      .map(g =>
        GroupReport(
          g.id,
          g.points.map(p =>
            PointReport(p.portName, p.bins.map(b => BinReport(b, coverageDB.getNHits(p.portName, b.name))))
          ),
          g.crosses.map {
            case t: TimedCross =>
              //Sanity check
              if (currentCycle == 0)
                throw new IllegalStateException(
                  "Stepping needs to be done with the coverage reporter in order to enable timed coverage!"
                )

              CrossReport(t, compileTimedHits(t).map(e => CrossBinReport(e._1, e._2)), t.delay)

            case c: CrossPoint => CrossReport(c, c.bins.map(cb => CrossBinReport(cb, coverageDB.getNHits(cb))))
          }
        )
      )
      .toList
  )

  /** Prints out a human readable coverage report
    */
  def printReport(): Unit = println(report.serialize)

  /** Advances the clock by one cycle
    * @note Needs to be used in order to enable timed coverage
    * @param cycles the number of cycles by which we want to advance the clock
    */
  def step(cycles: Int = 1): Unit = {
    for (_ <- 0 until cycles) {
      sample()

      //Step the dut then the database
      dut.clock.step(1)
      coverageDB.step()
    }
  }

  /** Retrieves the current cycle
    */
  def currentCycle: BigInt = coverageDB.getCurCycle

  /** Go through the database and compile the number of timed hits into a number of delayed hits
    * @param t the timed cross point in question
    * @return a list of Cross bin => numHits mappings
    */
  private def compileTimedHits(t: TimedCross): List[(CrossBin, BigInt)] =
    t.bins.map(cb => {
      //Retrieve the timed hit samples for both ranges
      val bin1cycles = coverageDB.getTimedHits(t.pointName1, cb.bin1Name)
      val bin2cycles = coverageDB.getTimedHits(t.pointName2, cb.bin2Name)

      //Compute the number of delay-synchronized hits
      val groups = bin1cycles.zip(bin2cycles)
      t.delay match {
        case Exactly(delay) =>
          (cb, BigInt(groups.filter(g => (g._1._2 + delay) == g._2._2).map(g => g._1._1).length))
        case Eventually(delay) =>
          (cb, BigInt(groups.filter(g => (g._2._2 - g._1._2) <= delay).map(g => g._1._1).length))
        case Always(delay) =>
          (
            cb,
            if ((0 until delay).forall(i => bin2cycles.map(_._2).contains(i) && bin1cycles.map(_._2).contains(i)))
              BigInt(1)
            else
              BigInt(0)
          )
        case _ => (cb, BigInt(0))
      }
    })

  /** Samples all of the coverpoints defined in the various covergroups
    * and updates the values stored in the coverageDB
    */
  def sample(): Unit = {

    def sampleBins(point: CoverPoint, value: Int): Unit =
      point.bins.foreach(bin => {
        if (bin.range contains value) {
          coverageDB.addBinHit(point.portName, bin.name, value)
        }
      })

    coverGroups.foreach(group => {
      var sampledPoints: List[CoverPoint] = Nil

      //Sample cross points
      group.crosses.foreach(cross => {
        val points = cross.sample(coverageDB)

        if (points.isDefined) {
          sampledPoints = sampledPoints :+ points.get._1
          sampledPoints = sampledPoints :+ points.get._2
        }
      })

      //Sample individual points
      group.points.foreach(point => {
        if (!sampledPoints.contains(point)) {
          //Add the point to the list
          sampledPoints = sampledPoints :+ point

          //Check for the ports
          val pointVal = point.port.peek().asUInt().litValue()
          sampleBins(point, pointVal.toInt)
        }
      })
    })
  }

  /** Creates a new coverGroup given a list of coverPoints
    * @param points the list of all coverPoints that will be sampled by the group.
    *               These are defined by (portName: String, bins: List[BinSpec])
    * @return the unique ID attributed to the group
    */
  def register(points: List[CoverPoint], crosses: List[Cross] = Nil): CoverGroup = {
    //Generate the group's identifier
    val gid: BigInt = coverageDB.createCoverGroup()

    //Register coverpoints
    points.foreach(p => coverageDB.registerCoverPoint(p.portName, p))
    crosses.foreach(c => c.register(coverageDB))

    //Create final coverGroup
    val group = CoverGroup(gid, points, crosses)
    coverGroups.append(group)
    group
  }
}
