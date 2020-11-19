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

import chisel3.tester.testableData

import scala.collection.mutable.ArrayBuffer

/**
  * Handles everything related to functional coverage
  */
class CoverageReporter {
    private val coverGroups: ArrayBuffer[CoverGroup] = new ArrayBuffer[CoverGroup]()
    private val coverageDB: CoverageDB = new CoverageDB

    /**
      * Makes a readable functional coverage report
      * @return the report in string form
      */
    def report: CoverageReport = CoverageReport(
        coverGroups.map(g =>
            GroupReport(
                g.id,
                g.points.map(p =>
                    PointReport(p.portName, p.bins.map(b => BinReport(b, coverageDB.getNHits(p.portName, b.name))))),
                g.crosses.map(c =>
                    CrossReport(c, c.bins.map(cb => CrossBinReport(cb, coverageDB.getNHits(cb))))))
        ).toList
    )

    /**
      * Prints out a human readable coverage report
      */
    def printReport(): Unit = println(report.serialize)

    /**
      * Samples all of the coverpoints defined in the various covergroups
      * and updates the values stored in the coverageDB
      */
    def sample(): Unit = {

        def sampleBins(point: CoverPoint, value: Int) : Unit =
            point.bins.foreach(bin => {
                if(bin.range contains value) {
                    coverageDB.addBinHit(point.portName, bin.name, value)
                }
            })

        coverGroups foreach(group => {
            var sampledPoints: List[CoverPoint] = Nil

            //Sample cross points
            group.crosses.foreach(cross => {
                val (point1, point2) = coverageDB.getPointsFromCross(cross)
                val pointVal1 = point1.port.peek().asUInt().litValue()
                val pointVal2 = point2.port.peek().asUInt().litValue()

                //Add the points to the list
                sampledPoints = sampledPoints :+ point1
                sampledPoints = sampledPoints :+ point2

                //Sample the points individually first
                sampleBins(point1, pointVal1.toInt)
                sampleBins(point2, pointVal2.toInt)

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
                    val pointVal = point.port.peek().asUInt().litValue()
                    sampleBins(point, pointVal.toInt)
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
