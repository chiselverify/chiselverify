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

import chisel3._
import chisel3.tester.testableClock
import chiselverify.coverage.CoverReport._

import scala.collection.mutable.ArrayBuffer

/**
  * Handles everything related to functional coverage
  */
class CoverageReporter[T <: Module](private val dut: T) {
    private val coverGroups: ArrayBuffer[CoverGroup] = new ArrayBuffer[CoverGroup]()
    private val coverageDB: CoverageDB = new CoverageDB

    /**
      * Makes a readable functional coverage report
      * @return the report in string form
      */
    def report: CoverageReport =
        CoverageReport(coverGroups.map(g => GroupReport(g.id, g.points.map(_.report(coverageDB)))).toList)

    /**
      * Prints out a human readable coverage report
      */
    def printReport(): Unit = println(report.serialize)

    /**
      * Advances the clock by one cycle
      * @note Needs to be used in order to enable timed coverage
      * @param cycles the number of cycles by which we want to advance the clock
      */
    def step(cycles: Int = 1): Unit = (0 until cycles) foreach {
        _ => {
            sample()

            //Step the dut then the database
            dut.clock.step(1)
            coverageDB.step()
        }
    }

    /**
      * Retrieves the current cycle
      */
    def currentCycle: BigInt = coverageDB.getCurCycle

    /**
      * Samples all points in a coverpoints defined in a given covergroup
      * and updated the values stored in the coverageDB
      * @param id the id of the covergroup that will be sampled.
      */
    def sample(id: Int): Unit = coverGroups.find(_.id == id) match {
        case None => throw new IllegalArgumentException(s"No group with id $id registered!")
        case Some(group) => group.points.foreach(_.sample(coverageDB))
    }

    /**
      * Samples all of the coverpoints defined in the various covergroups
      * and updates the values stored in the coverageDB
      */
    def sample(): Unit = coverGroups foreach(group => group.points.foreach(_.sample(coverageDB)))

    /**
      * Creates a new coverGroup given a list of coverPoints
      * @param points the list of all coverPoints that will be sampled by the group.
      *               These are defined by (portName: String, bins: List[BinSpec])
      * @return the unique ID attributed to the group
      */
    def register(points: CoverConst*): CoverGroup = {
        //Generate the group's identifier
        val gid: BigInt = coverageDB.createCoverGroup()

        //Register coverpoints
        points foreach (p => p.register(coverageDB))

        //Create final coverGroup
        val group = CoverGroup(gid, points.toList)
        coverGroups append group
        group
    }
}
