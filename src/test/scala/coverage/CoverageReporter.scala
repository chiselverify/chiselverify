// See README.md for license details.

package coverage

import chisel3.tester.testableData
import coverage.Coverage._

import scala.collection.mutable
import scala.collection.mutable.ArrayBuffer

/**
  * Handles everything related to functional coverage
  * @param dut the DUT currently being tested
  * @tparam T the type of the DUT
  */
class CoverageReporter {
    private val coverGroups: ArrayBuffer[CoverGroup] = new ArrayBuffer[CoverGroup]()
    private val coverageDB: CoverageDB = new CoverageDB

    /**
      * Makes a readable fucntional coverage report
      * @return the report in string form
      */
    def report: String = {
        val rep: StringBuilder = new StringBuilder(s"\n============ COVERAGE REPORT ============\n")
        coverGroups foreach(group => {
            rep append s"============== GROUP ID: ${group.id} ==============\n"
            group.points.foreach(point => {
                rep append s"COVER_POINT PORT NAME: ${point.portName}\n"
                point.bins.foreach(bin => {
                    val nHits = coverageDB.getNHits(bin)
                    rep append s"BIN ${bin.name} COVERING ${bin.range.toString} HAS $nHits HIT(S)\n"
                })
                rep append s"=========================================\n"
            })
            group.crosses.foreach(cross => {
                rep append s"CROSS_POINT ${cross.name} FOR POINTS ${cross.pointName1} AND ${cross.pointName2}\n"
                cross.bins.foreach(cb => {
                    val nHits = coverageDB.getNHits(cb)
                    rep append s"BIN ${cb.name} COVERING ${cb.range1.toString} CROSS ${cb.range2.toString} HAS $nHits HIT(S)\n"
                })
                rep append s"=========================================\n"
            })
        })
        rep.mkString
    }

    /**
      * Prints out a human readable coverage report
      */
    def printReport(): Unit = println(report)

    /**
      * Samples all of the coverpoints defined in the various covergroups
      * and updates the values stored in the coverageDB
      */
    def sample(): Unit = {

        def sampleBins(point: CoverPoint, value: BigInt) : Unit =
            point.bins.foreach(bin => {
                if(bin.range contains value) {
                    coverageDB.addBinHit(bin, value)
                }
            })

        coverGroups foreach(group => {
            var sampledPoints: List[CoverPoint] = Nil

            //Sample cross points
            group.crosses.foreach(cross => {
                val (point1, point2) = coverageDB.getPointsFromCross(cross)
                val pointVal1 = point1.port.peek().litValue()
                val pointVal2 = point2.port.peek().litValue()

                //Add the points to the list
                sampledPoints = sampledPoints :+ point1
                sampledPoints = sampledPoints :+ point2

                //Sample the points individually first
                sampleBins(point1, pointVal1)
                sampleBins(point2, pointVal2)

                //Sample the cross bins
                cross.bins.foreach(cb => {
                    if((cb.range1 contains pointVal1) && (cb.range2 contains pointVal2)) {
                        coverageDB.addCrossBinHit(cb, (pointVal1, pointVal2))
                    }
                })
            })

            //Sample individual points
            group.points.foreach(point => {
                if(!sampledPoints.contains(point)) {
                    //Add the point to the list
                    sampledPoints = sampledPoints :+ point

                    //Check for the ports
                    val pointVal = point.port.peek().litValue()
                    sampleBins(point, pointVal)
                }
            })
        })
    }

    /**
      * Creates a new coverGroup given a list of coverPoints
      * @param points the list of all coverPoints that will be sampled by the group.
      *               These are defined by (portName: String, bins: List[BinSpec])
      * @return the unique ID attributed to the group
      */
    def register(points: List[CoverPoint], crosses: List[Cross] = Nil): CoverGroup = {
        //Generate the group's identifier
        val gid: BigInt = coverageDB.createCoverGroup()

        //Register coverpoints
        points foreach (p => coverageDB.registerCoverPoint(p.portName, p))
        crosses foreach coverageDB.registerCross

        //Create final coverGroup
        val group = CoverGroup(gid, points, crosses)
        coverGroups append group
        group
    }
}
