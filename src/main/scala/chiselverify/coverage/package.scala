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

import chisel3.Data
import chiseltest.testableData
import chiselverify.timing._

package object coverage {

  /** Class that can generate a part of the coverage report
    */
  abstract class Report {

    /** Generates a part of or the entirety of the coverage report
      * @return a string containing the coverage report
      */
    def report: String
  }

  /** General data class containing the coverage report
    * @param groups the list of group reports
    */
  case class CoverageReport(groups: List[GroupReport]) extends Serializable {

    /** Creates a human-readable coverage report
      */
    def serialize: String = {
      val rep = new StringBuilder(s"\n============ COVERAGE REPORT ============\n")
      groups.foreach(group => rep.append(s"${group.report}"))
      rep.append("=========================================\n")
      rep.mkString
    }

    /** Traverses the report tree to find the given bin (if any)
      * @param groupId the id of the group containing the bin
      * @param pointName the name the point that contains the bin
      * @param binName the name of the bin itself
      * @return
      */
    def binNHits(groupId: BigInt, pointName: String, binName: String): BigInt = {
      //Look for the group
      groups.find(_.id == groupId) match {
        case None => throw new IllegalArgumentException(s"No group with ID $groupId!!")

        //Look for the point
        case Some(group) =>
          group.points.find(_.name == pointName) match {
            case None =>
              group.crosses.find(_.cross.name == pointName) match {
                case None => throw new IllegalArgumentException(s"No point with name $pointName!!")

                //Look for the cross bin
                case Some(c) =>
                  c.bins.find(_.crossBin.name == binName) match {
                    case None      => throw new IllegalArgumentException(s"No bin with name $binName!!")
                    case Some(bin) => bin.nHits
                  }
              }

            //Look for the bin
            case Some(p) =>
              p.bins.find(_.bin.name == binName) match {
                case None    => throw new IllegalArgumentException(s"No bin with name $binName!!")
                case Some(b) => b.nHits
              }
          }
      }
    }
  }

  /** Contains the coverage report info for a given cover group
    * @param id the id of the group
    * @param points the list of reports for the coverpoints contained in this group
    * @param crosses the list of reports for the crosspoints contained in this group
    */
  case class GroupReport(id: BigInt, points: List[PointReport] = Nil, crosses: List[CrossReport] = Nil) extends Report {
    override def report: String = {
      val rep = new StringBuilder(s"============== GROUP ID: $id ==============\n")
      points.foreach(point => rep.append(s"${point.report}\n=========================================\n"))
      crosses.foreach(cross => rep.append(s"${cross.report}\n=========================================\n"))
      rep.mkString
    }
  }

  /** Contains the coverage report info for a given cover point
    * @param name the name of the cover point
    * @param bins the list of reports related to the bins of the current cover point
    */
  case class PointReport(name: String, bins: List[BinReport]) extends Report {
    override def report: String = {
      val rep = new StringBuilder(s"COVER_POINT PORT NAME: $name")
      bins.foreach(bin => rep.append(s"\n${bin.report}"))
      rep.mkString
    }
  }

  /** Contains the coverage report info for a given cross point
    * @param cross a reference to the cross point for which we are generating a report
    * @param bins the list of reports related to the bins of the current cross point
    */
  case class CrossReport(cross: Cross, bins: List[CrossBinReport], delay: DelayType = NoDelay) extends Report {
    override def report: String = {
      val rep = new StringBuilder(s"CROSS_POINT ${cross.name} FOR POINTS ${cross.pointName1} AND ${cross.pointName2}")
      rep.append(delay.toString)
      bins.foreach(bin => rep.append(s"\n${bin.report}"))
      rep.mkString
    }
  }

  /** Contains the coverage report info for a given cover point bin
    * @param bin a reference to the bin for which we are generating a report
    * @param nHits the number of hits sampled for this bin during the test suite
    */
  case class BinReport(bin: Bins, nHits: BigInt) extends Report {
    override def report: String = s"BIN ${bin.name} COVERING ${bin.range.toString} HAS $nHits HIT(S)"
  }

  /** Contains the coverage report info for a given cross point bin
    * @param crossBin a reference to the cross bin for which we are generating a report
    * @param nHits the number of hits sampled for this cross bin during the test suite
    */
  case class CrossBinReport(crossBin: CrossBin, nHits: BigInt) extends Report {
    override def report: String =
      s"BIN ${crossBin.name} COVERING ${crossBin.range1.toString} CROSS ${crossBin.range2.toString} HAS $nHits HIT(S)"
  }

  /** Represents a group of cover points that will be sampled simultaneously
    * @param id a unique identifier for the group
    * @param points the cover points that are grouped together
    * @param crosses the cross points contained in the group
    */
  case class CoverGroup(id: BigInt, points: List[CoverPoint], crosses: List[Cross])

  /** Represents a single cover point that samples a given dut port
    * @param port the DUT port that will be sampled for this point
    * @param portName the name that will be used to represent the point in the report
    * @param bins the list of value ranges that will be checked for for the given port
    */
  case class CoverPoint(port: Data, portName: String)(val bins: List[Bins] = List(DefaultBin(port))) {
    override def toString: String = s"CoverPoint($port, $portName)($bins)"
  }

  abstract class Cross(val name: String, val pointName1: String, val pointName2: String, val bins: List[CrossBin]) {

    /** Samples the current cross relation using a given database
      * @param db the current database used throughout the test suits
      * @return the two points that were sampled during this cross sampling
      */
    def sample(db: CoverageDB): Option[(CoverPoint, CoverPoint)]

    /** Registers the current cross point with the given coverage DB
      * @param db the database used for the current test suite
      */
    def register(db: CoverageDB): Unit
  }

  /** Represents a coverage relation between two different DUT ports
    * @param name the name that will be used to represent the relation in the report
    * @param pointName1 the first point of the relation
    * @param pointName2 the other point in the relation
    * @param bins the list of value ranges that will be checked for for the given relation
    */
  case class CrossPoint(
    override val name:       String,
    override val pointName1: String,
    override val pointName2: String
  )(override val bins:       List[CrossBin])
      extends Cross(name, pointName1, pointName2, bins) {

    override def sample(db: CoverageDB): Option[(CoverPoint, CoverPoint)] = {

      def sampleBins(point: CoverPoint, value: Int): Unit =
        point.bins.foreach(bin => {
          if (bin.range contains value) {
            db.addBinHit(point.portName, bin.name, value)
          }
        })

      val (point1, point2) = db.getPointsFromCross(this)
      val pointVal1 = point1.port.peek().asUInt().litValue()
      val pointVal2 = point2.port.peek().asUInt().litValue()

      //Sample the points individually first
      sampleBins(point1, pointVal1.toInt)
      sampleBins(point2, pointVal2.toInt)

      //Sample the cross bins
      bins.foreach(cb => {
        if ((cb.range1 contains pointVal1) && (cb.range2 contains pointVal2)) {
          db.addCrossBinHit(cb, (pointVal1, pointVal2))
        }
      })

      Some(point1, point2)
    }

    override def register(db: CoverageDB): Unit = db.registerCross(this)
  }

  /** A timed version of a cross point. This means that, given a delay, point 2 will be sampled a certain amount of
    * cycles after point 1.
    * @param name the name of the cross, used for the report
    * @param delay the number of cycles between sampling point1 and point2
    * @param pointName1 the first point that will be sampled
    * @param pointName2 the point that will be sampled ${delay} cycles after point1
    * @param bins the list of value ranges that will be checked for for the given relation
    */
  case class TimedCross(
    override val name:       String,
    override val pointName1: String,
    override val pointName2: String,
    delay:                   DelayType
  )(override val bins:       List[CrossBin])
      extends Cross(name, pointName1, pointName2, bins) {
    private var initCycle: Option[BigInt] = None

    override def sample(db: CoverageDB): Option[(CoverPoint, CoverPoint)] = {
      //Sanity check
      if (initCycle.isEmpty) throw new IllegalStateException("Timed relation hasn't been registered!")

      def sampleBins(
        portNames: (String, String),
        tBins:     List[CrossBin],
        values:    (BigInt, BigInt),
        cycle:     BigInt
      ): Unit =
        tBins.foreach(tBin => {
          if (tBin.range1 contains values._1) {
            db.addTimedBinHit(portNames._1, tBin.bin1Name, values._1, cycle)
          }

          if (tBin.range2 contains values._2) {
            db.addTimedBinHit(portNames._2, tBin.bin2Name, values._2, cycle)
          }
        })

      val (point1, point2) = db.getPointsFromCross(this)
      val pointVal1 = point1.port.peek().asUInt().litValue()
      val pointVal2 = point2.port.peek().asUInt().litValue()

      //Sample the points at the current cycle
      val curCycle = db.getCurCycle
      sampleBins((point1.portName, point2.portName), bins, (pointVal1, pointVal2), curCycle)

      None
    }

    /** Registers the cross relation with the db and schedules the second point
      * @param db the database used for the current test suite
      */
    override def register(db: CoverageDB): Unit = {
      //Register our point as a regular cross point
      db.registerCross(this)

      //Initialize current cycle
      initCycle = Some(db.getCurCycle)
    }
  }

  /** A value range that will be used for sampling
    * @param name the name of the value range that will be used to represent it in the report
    * @param range the actual scala range
    */
  case class Bins(name: String, range: Range) {
    def ==(that: Bins): Boolean =
      (name == that.name) && (range.start == that.range.start) && (range.end == that.range.`end`)
  }

  /** A range relation between two different ranges
    * @param name the name of the relation, used for the report
    * @param range1 the range that will be sampled for point1 of the relation
    * @param range2 the range that will be sampled for point2 of the relation
    */
  case class CrossBin(name: String, range1: Range, range2: Range) {
    val bin1Name: String = s"${name}_1"
    val bin2Name: String = s"${name}_2"

    def ==(that: CrossBin): Boolean = (name == that.name) &&
      (range1.start == that.range1.start) && (range1.end == that.range2.`end`) &&
      (range2.start == that.range1.start) && (range2.end == that.range2.`end`)
  }

  /** Defines the default bins for both cover points and cross points
    */
  object DefaultBin {
    private def defaultRange(port: Data): Range = 0 until math.pow(2, port.asUInt().getWidth).toInt

    /** Generates a default bin for a given cover point port
      * @param port the port for which the bin will be generated
      * @return a bin covering all possible values for a given port
      */
    def apply(port: Data): Bins = Bins("default", defaultRange(port))

    /** Generates a default bin for given cross point ports
      * @param port1 the first point of the cross point
      * @param port2 the second point of the cross point
      * @return a cross bin covering all possible value combinations for the given ports
      */
    def apply(port1: Data, port2: Data): CrossBin = CrossBin("defaultCross", defaultRange(port1), defaultRange(port2))
  }
}
