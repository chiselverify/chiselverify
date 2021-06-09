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

import scala.collection.mutable

package object coverage {

    /**
      * Class that can generate a part of the coverage report
      */
    abstract class Report {
        /**
          * Generates a part of or the entirety of the coverage report
          *
          * @return a string containing the coverage report
          */
        def report: String

        /**
          * String ID for the current report
          */
        val name: String

        /**
          * Integer ID for the current report
          */
        val id: BigInt

        /**
          * Adds two different reports of the same type together
          *
          * @param that an other report of the same type as this one
          * @return a concatenated version of the two reports
          */
        def +(that: Report): Report
    }

    def sortByName(a: Report, b: Report): Boolean = a.name > b.name

    def sortById(a: Report, b: Report): Boolean = a.id > b.id

    class CoverageCollector {
        private val reports = mutable.ArrayBuffer[CoverageReport]()

        def collect(groupReport: CoverageReport): Unit = {
            reports += groupReport
        }

        def report: String = {
            reports.reduce(_ + _).serialize
        }
    }

    /**
      * General data class containing the coverage report
      *
      * @param groups the list of group reports
      */
    case class CoverageReport(groups: List[Report]) extends Serializable {
        /**
          * Creates a human-readable coverage report
          */
        def serialize: String = {
            val rep = new StringBuilder(s"\n============ COVERAGE REPORT ============\n")
            groups foreach (group => rep append s"${group.report}")
            rep append "=========================================\n"
            rep.mkString
        }

        /**
          * Traverses the report tree to find the given bin (if any)
          *
          * @param groupId   the id of the group containing the bin
          * @param pointName the name the point that contains the bin
          * @param binName   the name of the bin itself
          * @return
          */
        def binNHits(groupId: BigInt, pointName: String, binName: String): BigInt = {
            //Look for the group
            groups.find(_.id == groupId) match {
                case None => throw new IllegalArgumentException(s"No group with ID $groupId!!")

                //Look for the point
                case Some(group) => group match {
                    case GroupReport(_, points, crosses) =>
                        points.find(_.name == pointName) match {
                            case None => crosses.find(_.name == pointName) match {
                                case None => throw new IllegalArgumentException(s"No point with name $pointName!!")

                                //Look for the cross bin
                                case Some(c) => c match {
                                    case CrossReport(_, bins, _) =>
                                        bins.find(_.name == binName) match {
                                            case None => throw new IllegalArgumentException(s"No bin with name $binName!!")
                                            case Some(b) => b match {
                                                case CrossBinReport(_, nHits) => nHits
                                                case _ => throw new IllegalStateException("CrossBin must be reported with a CrossBinReport")
                                            }
                                        }
                                    case _ => throw new IllegalStateException("Cross must be reported with a CrossReport")
                                }
                            }
                            //Look for the bin
                            case Some(p) => p match {
                                case PointReport(_, bins) =>
                                    bins.find(_.name == binName) match {
                                        case None => throw new IllegalArgumentException(s"No bin with name $binName!!")
                                        case Some(b) => b match {
                                            case BinReport(_, nHits) => nHits
                                            case _ => throw new IllegalStateException("Bin must be reported with a BinReport")
                                        }
                                    }
                                case _ => throw new IllegalStateException("Point must be reported with a PointReport")
                            }
                        }
                    case _ => throw new IllegalStateException("Group must be reported with a GroupReport")
                }
            }
        }

        override def equals(that: Any): Boolean = that match {
            case CoverageReport(g) => g.sortWith(sortById) == groups.sortWith(sortById)
            case _ => false
        }

        def +(that: CoverageReport): CoverageReport = {
            require(this == that)
            CoverageReport((that.groups zip groups).map { case (g1, g2) => g1 + g2 })
        }
    }

    /**
      * Contains the coverage report info for a given cover group
      *
      * @param id      the id of the group
      * @param points  the list of reports for the coverpoints contained in this group
      * @param crosses the list of reports for the crosspoints contained in this group
      */
    case class GroupReport(id: BigInt, points: List[Report] = Nil, crosses: List[Report] = Nil) extends Report {
        override def report: String = {
            val rep = new StringBuilder(s"============== GROUP ID: $id ==============\n")
            points foreach (point => rep append s"${point.report}\n=========================================\n")
            crosses foreach (cross => rep append s"${cross.report}\n=========================================\n")
            rep.mkString
        }

        override def equals(that: Any): Boolean = that match {
            case GroupReport(i, p, c) => i == id && p.sortWith(sortByName) == points.sortWith(sortByName) &&
                c.sortWith(sortByName) == crosses.sortWith(sortByName)
            case _ => false
        }

        override def +(that: Report): Report = that match {
            case GroupReport(_, tpoints, tcrosses) =>
                require(this == that)
                val newPoints: List[Report] = (tpoints zip points).map { case (p1, p2) => p1 + p2 }
                val newCrosses = (tcrosses zip crosses).map { case (c1, c2) => c1 + c2 }
                GroupReport(id, newPoints, newCrosses)

            case _ => throw new IllegalArgumentException("Argument must be of type GroupReport")
        }

        /**
          * String ID for the current report
          */
        override val name: String = s"$id"
    }

    case class ConditionReport(name: String, conds: List[Report]) extends Report {
        override def report: String = {
            val rep = new StringBuilder(s"COVER_CONDITION NAME: $name")
            ???
        }

        override def +(that: Report): Report = that match {
            case ConditionReport(_, tconds) =>
                require(this == that)
                val newPoints = (conds zip tconds).map { case (b1, b2) => b1 + b2 }
                ConditionReport(name, newPoints)

            case _ => throw new IllegalArgumentException("Argument must be of type ConditionReport")
        }

        override val id: BigInt = name.hashCode
    }

    /**
      * Contains the coverage report info for a given cover point
      *
      * @param name the name of the cover point
      * @param bins the list of reports related to the bins of the current cover point
      */
    case class PointReport(name: String, bins: List[Report]) extends Report {
        override def report: String = {
            val rep = new StringBuilder(s"COVER_POINT PORT NAME: $name")
            bins foreach (bin => rep append s"\n${bin.report}")
            rep.mkString
        }

        override def equals(that: Any): Boolean = that match {
            case PointReport(n, b) => n == name && b.sortWith(sortByName) == bins.sortWith(sortByName)
            case _ => false
        }

        override def +(that: Report): Report = that match {
            case PointReport(_, tbins) =>
                require(this == that)
                val newPoints = (bins zip tbins).map { case (b1, b2) => b1 + b2 }
                PointReport(name, newPoints)

            case _ => throw new IllegalArgumentException("Argument must be of type PointReport")
        }

        override val id: BigInt = name.hashCode
    }

    /**
      * Contains the coverage report info for a given cross point
      *
      * @param cross a reference to the cross point for which we are generating a report
      * @param bins  the list of reports related to the bins of the current cross point
      */
    case class CrossReport(cross: Cross, bins: List[Report], delay: DelayType = NoDelay) extends Report {
        override def report: String = {
            val rep = new StringBuilder(s"CROSS_POINT ${cross.name} FOR POINTS ${cross.pointName1} AND ${cross.pointName2}")
            rep append delay.toString
            bins foreach (bin => rep append s"\n${bin.report}")
            rep.mkString
        }

        override def equals(that: Any): Boolean = that match {
            case CrossReport(c, b, d) =>
                c == cross && bins.sortWith(sortByName) == b.sortWith(sortByName) && d == delay
            case _ => false
        }

        def +(that: Report): Report = that match {
            case CrossReport(_, tbins, _) =>
                require(this == that)
                val newBins = (bins zip tbins).map { case (b1, b2) => b1 + b2 }
                CrossReport(cross, newBins, delay)
            case _ => throw new IllegalArgumentException("Argument must be of type CrossReport")
        }

        override val name: String = cross.name
        override val id: BigInt = name.hashCode
    }

    /**
      * Contains the coverage report info for a given cover point bin
      *
      * @param bin   a reference to the bin for which we are generating a report
      * @param nHits the number of hits sampled for this bin during the test suite
      */
    case class BinReport(bin: Bins, nHits: BigInt) extends Report {
        private val proportion = nHits.toInt / bin.range.size.toDouble
        private val percentage = f"${if (proportion > 1) 100 else proportion * 100}%1.2f"

        override def report: String = s"BIN ${bin.name} COVERING ${bin.range.toString}" +
            s"${if (bin.condition != ((_: BigInt) => true)) s"WITH CONDITION ${bin.condition}" else ""}" +
            s"HAS $nHits HIT(S) = $percentage%"

        override def equals(that: Any): Boolean = that match {
            case BinReport(b, _) => b == bin
            case _ => false
        }

        override def +(that: Report): Report = that match {
            case BinReport(_, tnHits) =>
                require(this == that)
                BinReport(bin, tnHits + nHits)
            case _ => throw new IllegalArgumentException("Argument must be of type BinReport")
        }

        override val name: String = bin.name
        override val id: BigInt = bin.name.hashCode
    }

    /**
      * Contains the coverage report info for a given cross point bin
      *
      * @param crossBin a reference to the cross bin for which we are generating a report
      * @param nHits    the number of hits sampled for this cross bin during the test suite
      */
    case class CrossBinReport(crossBin: CrossBin, nHits: BigInt) extends Report {
        private val proportion = nHits.toInt / (crossBin.range1.size * crossBin.range2.size).toDouble
        private val percentage = f"${if (proportion > 1) 100 else proportion * 100}%1.2f"

        override def report: String =
            s"BIN ${crossBin.name} COVERING ${crossBin.range1.toString} CROSS ${crossBin.range2.toString} HAS $nHits HIT(S) = $percentage%"

        override def equals(that: Any): Boolean = that match {
            case CrossBinReport(c, _) => c == crossBin
            case _ => false
        }

        def +(that: Report): Report = that match {
            case CrossBinReport(_, tnHits) =>
                require(this == that)
                CrossBinReport(crossBin, nHits + tnHits)
            case _ => throw new IllegalArgumentException("Argument must be of type CrossBinReport")
        }

        override val name: String = crossBin.name
        override val id: BigInt = name.hashCode

    }

    /**
      * Represents a group of cover points that will be sampled simultaneously
      *
      * @param id      a unique identifier for the group
      * @param points  the cover points that are grouped together
      * @param crosses the cross points contained in the group
      */
    case class CoverGroup(id: BigInt, points: List[Cover], crosses: List[Cross])

    abstract class Cover(val ports: List[Data], val portName: String) {
        override def toString: String = serialize

        /**
          * Generates a report in a form that is compatible with the coverage reporter
          *
          * @return a Report
          */
        def report(db: CoverageDB): Report

        /**
          * Converts the current cover construct into a human-readable string
          *
          * @return a String representing the cover construct
          */
        def serialize: String

        /**
          * Samples the current cover construct
          *
          * @param db the coverage DataBase we are sampling in
          */
        def sample(db: CoverageDB): Unit
    }

    case class Condition(name: String, cond: List[BigInt] => Boolean) {
        def apply(args: List[BigInt]): Boolean = cond(args)
    }

    case class CoverCondition(p: List[Data], pN: String)(val conds: List[Condition])
        extends Cover(p, pN) {
        override def report(db: CoverageDB): Report = ???

        override def serialize: String = s"CoverCondition($ports, $portName)($conds)"

        override def sample(db: CoverageDB): Unit = {
            val pointVals = ports.map(_.peek().asUInt().litValue())
            db.addConditionalHit(portName, conds.filter(c => c(pointVals)).map(_.name))
        }
    }

    /**
      * Represents a single cover point that samples a given dut port
      *
      * @param p    the DUT port that will be sampled for this point
      * @param pN   the name that will be used to represent the point in the report
      * @param bins the list of value ranges that will be checked for for the given port
      */
    case class CoverPoint(port: Data, pN: String)(val bins: List[Bins] = List(DefaultBin(port)))
        extends Cover(port::Nil, pN) {

        override def serialize: String = s"CoverPoint($port, $portName)(${bins.map(_.serialize)})"

        override def sample(db: CoverageDB): Unit = {
            //Check for the ports & sample all bins
            val pointVal = port.peek().asUInt().litValue().toInt
            bins.foreach(_.sample(portName, pointVal, db))
        }

        override def report(db: CoverageDB): Report =
            PointReport(portName, bins.map(b => BinReport(b, db.getNHits(portName, b.name))))
    }

    abstract class Cross(val name: String, val pointName1: String, val pointName2: String, val bins: List[CrossBin]) {

        /**
          * Samples the current cross relation using a given database
          *
          * @param db the current database used throughout the test suits
          * @return the two points that were sampled during this cross sampling
          */
        def sample(db: CoverageDB): Option[(Cover, Cover)]

        /**
          * Registers the current cross point with the given coverage DB
          *
          * @param db the database used for the current test suite
          */
        def register(db: CoverageDB): Unit
    }

    /**
      * Represents a coverage relation between two different DUT ports
      *
      * @param name       the name that will be used to represent the relation in the report
      * @param pointName1 the first point of the relation
      * @param pointName2 the other point in the relation
      * @param bins       the list of value ranges that will be checked for for the given relation
      */
    case class CrossPoint(override val name: String, override val pointName1: String, override val pointName2: String)(override val bins: List[CrossBin])
        extends Cross(name, pointName1, pointName2, bins) {

        override def sample(db: CoverageDB): Option[(Cover, Cover)] = {

            db.getPointsFromCross(this) match {
                case (point1: CoverPoint, point2: CoverPoint) =>
                    val pointVal1 = point1.port.peek().asUInt().litValue()
                    val pointVal2 = point2.port.peek().asUInt().litValue()

                    //Sample the points individually first
                    point1.sample(db)
                    point2.sample(db)

                    //Sample the cross bins
                    bins.foreach(cb => {
                        if ((cb.range1 contains pointVal1) && (cb.range2 contains pointVal2)) {
                            db.addCrossBinHit(cb, (pointVal1, pointVal2))
                        }
                    })

                    Some(point1, point2)

                case _ => None
            }
        }

        override def register(db: CoverageDB): Unit = db.registerCross(this)
    }

    /**
      * A timed version of a cross point. This means that, given a delay, point 2 will be sampled a certain amount of 
      * cycles after point 1.
      *
      * @param name       the name of the cross, used for the report
      * @param delay      the number of cycles between sampling point1 and point2
      * @param pointName1 the first point that will be sampled
      * @param pointName2 the point that will be sampled ${delay} cycles after point1 
      * @param bins       the list of value ranges that will be checked for for the given relation
      */
    case class TimedCross(override val name: String, override val pointName1: String, override val pointName2: String,
                          delay: DelayType)(override val bins: List[CrossBin]) extends Cross(name, pointName1, pointName2, bins) {
        private var initCycle: Option[BigInt] = None

        override def sample(db: CoverageDB): Option[(CoverPoint, CoverPoint)] = {
            //Sanity check
            if (initCycle.isEmpty) throw new IllegalStateException("Timed relation hasn't been registered!")

            def sampleBins(portNames: (String, String), tBins: List[CrossBin], values: (BigInt, BigInt), cycle: BigInt): Unit =
                tBins.foreach(tBin => {
                    if (tBin.range1 contains values._1) {
                        db.addTimedBinHit(portNames._1, tBin.bin1Name, values._1, cycle)
                    }

                    if (tBin.range2 contains values._2) {
                        db.addTimedBinHit(portNames._2, tBin.bin2Name, values._2, cycle)
                    }
                })

            db.getPointsFromCross(this) match {
                case (point1: CoverPoint, point2: CoverPoint) =>
                    val pointVal1 = point1.port.peek().asUInt().litValue()
                    val pointVal2 = point2.port.peek().asUInt().litValue()

                    //Sample the points at the current cycle
                    val curCycle = db.getCurCycle
                    sampleBins((point1.portName, point2.portName), bins, (pointVal1, pointVal2), curCycle)
            }

            None
        }

        /**
          * Registers the cross relation with the db and schedules the second point
          *
          * @param db the database used for the current test suite
          */
        override def register(db: CoverageDB): Unit = {
            //Register our point as a regular cross point
            db.registerCross(this)

            //Initialize current cycle
            initCycle = Some(db.getCurCycle)
        }
    }

    /**
      * A value range that will be used for sampling
      *
      * @param name      the name of the value range that will be used to represent it in the report
      * @param range     the actual scala range
      * @param condition an extra condition that can be used to consider a hit
      */
    class Bins(val name: String, val rangeOpt: Option[Range], val condition: (BigInt) => Boolean = _ => true) {
        def ==(that: Bins): Boolean = {
            val r = rangeOpt.getOrElse(0 to 0)
            val thatR = that.rangeOpt.getOrElse(0 to 0)
            (name == that.name) && (r.start == thatR.start) && (r.end == thatR.end)
        }

        def sample(portName: String, value: BigInt, coverageDB: CoverageDB): Unit =
            if (condition(value)) rangeOpt match {
                case None => coverageDB.addBinHit(portName, name, value)
                case Some(r) => if (r.contains(value)) coverageDB.addBinHit(portName, name, value)
            }

        def range: Range = rangeOpt.getOrElse(0 to 0)

        def serialize: String = s"Bin( $name, ${rangeOpt.getOrElse("")} ${if (condition != ((_: BigInt) => true)) condition})"
    }

    object Bins {
        def apply(name: String, condition: BigInt => Boolean): Bins = new Bins(name, None, condition)
        def apply(name: String, range: Range): Bins = new Bins(name, Some(range))
        def apply(name: String, range: Range, condition: BigInt => Boolean) = new Bins(name, Some(range), condition)
    }


    /**
      * A range relation between two different ranges
      *
      * @param name   the name of the relation, used for the report
      * @param range1 the range that will be sampled for point1 of the relation
      * @param range2 the range that will be sampled for point2 of the relation
      */
    case class CrossBin(name: String, range1: Range, range2: Range) {
        val bin1Name: String = s"${name}_1"
        val bin2Name: String = s"${name}_2"

        def ==(that: CrossBin): Boolean = (name == that.name) &&
            (range1.start == that.range1.start) && (range1.end == that.range1.end) &&
            (range2.start == that.range2.start) && (range2.end == that.range2.end)
    }

    /**
      * Defines the default bins for both cover points and cross points
      */
    object DefaultBin {
        private def defaultRange(port: Data): Range = 0 until math.pow(2, port.asUInt().getWidth).toInt

        /**
          * Generates a default bin for a given cover point port
          *
          * @param port the port for which the bin will be generated
          * @return a bin covering all possible values for a given port
          */
        def apply(port: Data): Bins = Bins("default", defaultRange(port))

        /**
          * Generates a default bin for given cross point ports
          *
          * @param port1 the first point of the cross point
          * @param port2 the second point of the cross point
          * @return a cross bin covering all possible value combinations for the given ports
          */
        def apply(port1: Data, port2: Data): CrossBin = CrossBin("defaultCross", defaultRange(port1), defaultRange(port2))
    }
}
