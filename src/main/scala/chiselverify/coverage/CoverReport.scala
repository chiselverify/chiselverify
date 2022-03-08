/*
* Copyright 2021 DTU Compute - Section for Embedded Systems Engineering
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

import chiselverify.timing.{DelayType, NoDelay}

import scala.collection.mutable

object CoverReport {

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
          * @param bN   the name of the bin itself
          * @return
          */
        def binNHits(groupId: BigInt, pointName: String, bN: Option[String] = None): BigInt = {
            val binName =  bN.getOrElse("NoName")
            //Look for the group
            groups.find(_.id == groupId) match {
                case None => throw new IllegalArgumentException(s"No group with ID $groupId!!")
                //Look for the point
                case Some(group) => group match {
                    case GroupReport(_, points) =>
                        points.find(_.name == pointName) match {
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
                                case PointReport(_, bins) =>
                                    bins.find(_.name == binName) match {
                                        case None => throw new IllegalArgumentException(s"No bin with name $binName!!")
                                        case Some(b) => b match {
                                            case BinReport(_, nHits) => nHits
                                            case _ => throw new IllegalStateException("Bin must be reported with a BinReport")
                                        }
                                    }
                                case TimedOpReport(_, nHits) => nHits
                                case ConditionReport(_, conds, db) =>
                                    conds.find(_.name == binName) match {
                                        case None => throw new IllegalArgumentException(s"No condition with name $binName!!")
                                        case Some(cond) => db.getNHits(cond.name)
                                    }
                                case _ => throw new IllegalStateException("Illegal report type!")
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
            CoverageReport((that.groups zip groups).map { case (g1, g2) => g1 + g2 })
        }
    }

    /**
      * Contains the coverage report info for a given cover group
      *
      * @param id      the id of the group
      * @param points  the list of reports for the coverpoints contained in this group
      */
    case class GroupReport(id: BigInt, points: List[Report] = Nil) extends Report {
        override def report: String = {
            val rep = new StringBuilder(s"============== GROUP ID: $id ==============\n")
            points foreach (point => rep append s"${point.report}\n=========================================\n")
            rep.mkString
        }

        override def equals(that: Any): Boolean = that match {
            case GroupReport(i, p) => i == id && p.sortWith(sortByName) == points.sortWith(sortByName)
            case _ => false
        }

        override def +(that: Report): Report = that match {
            case GroupReport(_, tpoints) =>
                // require(this == that) // TODO: why is this a requirement?
                val newPoints: List[Report] = (tpoints zip points).map { case (p1, p2) => p1 + p2 }
                GroupReport(id, newPoints)

            case _ => throw new IllegalArgumentException("Argument must be of type GroupReport")
        }

        /**
          * String ID for the current report
          */
        override val name: String = s"$id"
    }

    case class ConditionReport(name: String, conds: List[Condition], db: CoverageDB) extends Report {
        override def report: String = {
            val rep = new StringBuilder(s"COVER_CONDITION NAME: $name")

            conds foreach {
                cond => {
                    val nHits = db.getNHits(cond.name)
                    rep append s"\n${cond.report} HAS ${nHits} HITS"
                    //CAUSES HEAP OVERFLOW
                    //val percent = (nHits.toDouble / db.getCondSize(cond.name).toDouble) * 100.0
                    //Using expected hits instead
                    rep append (cond.expectedHits match {
                        case None => ""
                        case Some(eH) => s" EXPECTED $eH = ${(nHits.toDouble / eH.toDouble) * 100.0}%"
                    })
                }
            }
            rep.mkString
        }

        override def +(that: Report): Report = that match {
            case ConditionReport(_, tconds, db) =>
                require(this == that)
                val newPoints = (conds zip tconds).map { case (b1, b2) => b1 + b2 }
                ConditionReport(name, newPoints, db)

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

    case class TimedOpReport(timedCoverOp: TimedCoverOp, nHits: Int) extends Report {
        /**
          * Generates a part of or the entirety of the coverage report
          *
          * @return a string containing the coverage report
          */
        override def report: String = s"${timedCoverOp.serialize} HAS $nHits HIT(S)."

        /**
          * String ID for the current report
          */
        override val name: String = timedCoverOp.pointName

        /**
          * Integer ID for the current report
          */
        override val id: BigInt = timedCoverOp.pointName.hashCode

        /**
          * Adds two different reports of the same type together
          *
          * @param that an other report of the same type as this one
          * @return a concatenated version of the two reports
          */
        override def +(that: Report): Report = that match {
            case TimedOpReport(t, hits) => TimedOpReport(timedCoverOp + t, nHits + hits)
        }
    }

    /**
      * Contains the coverage report info for a given cross point
      *
      * @param cross a reference to the cross point for which we are generating a report
      * @param bins  the list of reports related to the bins of the current cross point
      */
    case class CrossReport(cross: CrossConst, bins: List[Report], delay: DelayType = NoDelay) extends Report {
        override def report: String = {
            val rep = new StringBuilder(s"CROSS_POINT ${cross.name}")
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
                // require(this == that) // TODO: why is this a requirement?
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
    case class BinReport(bin: Bin, nHits: BigInt) extends Report {
        private val proportion = nHits.toInt / bin.range.size.toDouble
        private val percentage = f"${if(nHits == 0) 0 else if (proportion > 1) 100 else proportion * 100}%1.2f"

        override def report: String = s"BIN ${bin.name} COVERING ${bin.range.toString}" +
            s" HAS $nHits HIT(S) = $percentage%"

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
        private val proportion = nHits.toDouble / crossBin.ranges.map(_.size).foldLeft(1.0)(_*_)
        private val percentage = f"${if(nHits == 0) 0 else if (proportion > 1) 100 else proportion * 100.0}%1.2f"

        override def report: String = {
            val rep: StringBuilder = new StringBuilder(s"BIN ${crossBin.name} COVERING: CROSS(")
            crossBin.ranges.foreach(r => rep append s"$r, ")
            //Remove trailing comma and space
            rep.delete(rep.size - 2, rep.size)
            rep append s") HAS $nHits HIT(S) = $percentage%"
            rep.mkString
        }

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
}
