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

package object coverage {

    /**
      * Class that can generate a part of the coverage report
      */
    abstract class Report {
        /**
          * Generates a part of or the entirety of the coverage report
          * @return a string containing the coverage report
          */
        def report: String
    }

    /**
      * General data class containing the coverage report
      * @param groups the list of group reports
      */
    case class CoverageReport(groups: List[GroupReport]) extends Serializable {
        /**
          * Creates a human-readable coverage report
          */
        def serialize: String = {
            val rep = new StringBuilder(s"\n============ COVERAGE REPORT ============\n")
            groups foreach(group => rep append s"${group.report}")
            rep append "=========================================\n"
            rep.mkString
        }

        /**
          * Traverses the report tree to find the given bin (if any)
          * @param groupId the id of the group containing the bin
          * @param pointName the name the point that contains the bin
          * @param binName the name of the bin itself
          * @return
          */
        def binNHits(groupId: BigInt, pointName: String, binName: String) : BigInt = {
            //Look for the group
            groups.find(_.id == groupId) match {
                case None => throw new IllegalArgumentException(s"No group with ID $groupId!!")

                //Look for the point
                case Some(group) => group.points.find(_.name == pointName) match {
                    case None => group.crosses.find(_.cross.name == pointName) match {
                        case None => throw new IllegalArgumentException(s"No point with name $pointName!!")

                        //Look for the cross bin
                        case Some(c) => c.bins.find(_.crossBin.name == binName) match {
                            case None => throw new IllegalArgumentException(s"No bin with name $binName!!")
                            case Some(bin) => bin.nHits
                        }
                    }

                    //Look for the bin
                    case Some(p) => p.bins.find(_.bin.name == binName) match {
                        case None => throw new IllegalArgumentException(s"No bin with name $binName!!")
                        case Some(b) => b.nHits
                    }
                }
            }
        }
    }

    /**
      * Contains the coverage report info for a given cover group
      * @param id the id of the group
      * @param points the list of reports for the coverpoints contained in this group
      * @param crosses the list of reports for the crosspoints contained in this group
      */
    case class GroupReport(id: BigInt, points: List[PointReport] = Nil, crosses: List[CrossReport] = Nil) extends Report {
        override def report: String = {
            val rep = new StringBuilder(s"============== GROUP ID: $id ==============\n")
            points foreach (point => rep append s"${point.report}\n=========================================\n")
            crosses foreach (cross => rep append s"${cross.report}\n=========================================\n")
            rep.mkString
        }
    }

    /**
      * Contains the coverage report info for a given cover point
      * @param name the name of the cover point
      * @param bins the list of reports related to the bins of the current cover point
      */
    case class PointReport(name: String, bins: List[BinReport]) extends Report {
        override def report: String = {
            val rep = new StringBuilder(s"COVER_POINT PORT NAME: $name")
            bins foreach (bin => rep append s"\n${bin.report}")
            rep.mkString
        }
    }

    /**
      * Contains the coverage report info for a given cross point
      * @param cross a reference to the cross point for which we are generating a report
      * @param bins the list of reports related to the bins of the current cross point
      */
    case class CrossReport(cross: Cross, bins: List[CrossBinReport]) extends Report {
        override def report: String = {
            val rep = new StringBuilder(s"CROSS_POINT ${cross.name} FOR POINTS ${cross.pointName1} AND ${cross.pointName2}")
            bins foreach (bin => rep append s"\n${bin.report}")
            rep.mkString
        }
    }

    /**
      * Contains the coverage report info for a given cover point bin
      * @param bin a reference to the bin for which we are generating a report
      * @param nHits the number of hits sampled for this bin during the test suite
      */
    case class BinReport(bin: Bins, nHits: BigInt) extends Report {
        override def report: String = s"BIN ${bin.name} COVERING ${bin.range.toString} HAS $nHits HIT(S)"
    }

    /**
      * Contains the coverage report info for a given cross point bin
      * @param crossBin a reference to the cross bin for which we are generating a report
      * @param nHits the number of hits sampled for this cross bin during the test suite
      */
    case class CrossBinReport(crossBin: CrossBin, nHits: BigInt) extends Report {
        override def report: String =
            s"BIN ${crossBin.name} COVERING ${crossBin.range1.toString} CROSS ${crossBin.range2.toString} HAS $nHits HIT(S)"
    }

    /**
      * Represents a group of cover points that will be sampled simultaneously
      * @param id a unique identifier for the group
      * @param points the cover points that are grouped together
      * @param crosses the cross points contained in the group
      */
    case class CoverGroup(id: BigInt, points: List[CoverPoint], crosses: List[Cross])

    /**
      * Represents a single cover point that samples a given dut port
      * @param port the DUT port that will be sampled for this point
      * @param portName the name that will be used to represent the point in the report
      * @param bins the list of value ranges that will be checked for for the given port
      */
    case class CoverPoint(port: Data, portName: String)(val bins: List[Bins] = List(DefaultBin(port)))

    /**
      * Represents a coverage relation between two different DUT ports
      * @param name the name that will be used to represent the relation in the report 
      * @param pointName1 the first point of the relation
      * @param pointName2 the other point in the relation
      * @param bins the list of value ranges that will be checked for for the given relation
      */
    case class Cross(name: String, pointName1: String, pointName2: String)(val bins: List[CrossBin])

    /**
      * A timed version of a cross point. This means that, given a delay, point 2 will be sampled a certain amount of 
      * cycles after point 1.
      * @param name the name of the cross, used for the report
      * @param delay the number of cycles between sampling point1 and point2
      * @param pointName1 the first point that will be sampled
      * @param pointName2 the point that will be sampled ${delay} cycles after point1 
      * @param bins the list of value ranges that will be checked for for the given relation
      */
    case class TimedCross(name: String, delay: Int, pointName1: String, pointName2: String)(val bins: List[CrossBin])

    /**
      * A value range that will be used for sampling
      * @param name the name of the value range that will be used to represent it in the report
      * @param range the actual scala range
      */
    case class Bins(name: String, range: Range) {
        def ==(that: Bins): Boolean = (name == that.name) && (range.start == that.range.start) && (range.end == that.range.`end`)
    }

    /**
      * A range relation between two different ranges
      * @param name the name of the relation, used for the report
      * @param range1 the range that will be sampled for point1 of the relation
      * @param range2 the range that will be sampled for point2 of the relation
      */
    case class CrossBin(name: String, range1: Range, range2: Range) {
         def ==(that: CrossBin): Boolean = (name == that.name) &&
            (range1.start == that.range1.start) && (range1.end == that.range2.`end`) &&
            (range2.start == that.range1.start) && (range2.end == that.range2.`end`)
    }

    /**
      * Defines the default bins for both cover points and cross points
      */
    object DefaultBin {
        private def defaultRange(port: Data): Range = 0 until math.pow(2, port.asUInt().getWidth).toInt

        /**
          * Generates a default bin for a given cover point port
          * @param port the port for which the bin will be generated
          * @return a bin covering all possible values for a given port
          */
        def apply(port: Data): Bins = Bins("default", defaultRange(port))

        /**
          * Generates a default bin for given cross point ports
          * @param port1 the first point of the cross point
          * @param port2 the second point of the cross point
          * @return a cross bin covering all possible value combinations for the given ports
          */
        def apply(port1: Data, port2: Data): CrossBin = CrossBin("defaultCross", defaultRange(port1), defaultRange(port2))
    }
}
