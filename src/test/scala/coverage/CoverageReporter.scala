// See README.md for license details.

package coverage

import chisel3._
import chisel3.tester.testableData
import coverage.Coverage._

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
        //Sample each coverPoint in each group and update the value stored in the DB
        coverGroups foreach(group => {
            group.points.foreach(point => {
                //Check for the ports
                val pointVal = point.port.peek().litValue()
                point.bins.foreach(bin => {
                    if(bin.range contains pointVal) {
                        coverageDB.addBinHit(bin, pointVal)
                    }
                })
            })
        })
    }

    /**
      * Creates a new coverGroup given a list of coverPoints
      * @param points the list of all coverPoints that will be sampled by the group.
      *               These are defined by (portName: String, bins: List[BinSpec])
      * @return the unique ID attributed to the group
      */
    def register(points: List[CoverPoint]): CoverGroup = {
        //Generate the group's identifier
        val gid: BigInt = coverageDB.createCoverGroup()

        //Create final coverGroup
        val group = CoverGroup(points, gid)
        coverGroups append group
        group
    }
}
