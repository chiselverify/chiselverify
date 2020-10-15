// See README.md for license details.

package coverage

import chisel3.Data
import coverage.Coverage.Bins

import scala.collection.mutable
import scala.collection.mutable.ArrayBuffer

/**
  * Stores all of the data relevant to the functional coverage elements
  */
class CoverageDB {
    //Contains all a mapping from coverPoint id to the DUT's port name
    private val coverIdPortMap: mutable.HashMap[String, Data] = new mutable.HashMap[String, Data]()
    //Contains the current sampled values of the different bins
    private val binIdHitValuesMap: mutable.HashMap[Bins, List[BigInt]] = new mutable.HashMap[Bins, List[BigInt]]()
    //Contains a mapping for the bin ID pair (coverPointID, binID) to the current number of hits it has
    private val binIdNumHitsMap: mutable.HashMap[Bins, BigInt] = new mutable.HashMap[Bins, BigInt]()

    //Keep track of the different valid IDs
    private val groupIds: ArrayBuffer[BigInt] = new ArrayBuffer[BigInt]()
    private val pointIds: ArrayBuffer[BigInt] = new ArrayBuffer[BigInt]()

    //Internal group->point->bin mappings
    private val groupPointMap: mutable.HashMap[BigInt, List[BigInt]] = new mutable.HashMap[BigInt, List[BigInt]]()

    //TODO: Maybe add some options

    private var lastCoverGroupId: BigInt = 0
    private var lastCoverPointId: BigInt = 0

    /**
      * Adds a coverGroup to the list of valid coverGroups
      * @return the newly generated group id
      */
    def createCoverGroup(): BigInt = {
        lastCoverGroupId += 1
        groupIds append lastCoverGroupId
        lastCoverGroupId
    }

    /**
      * Creates a coverPoint entry for the given coverGroup
      * @param groupId the id of the coverGroup containing the new coverPoint
      * @param dutPortName the name of the port that's being sampled by the new coverPoint
      * @return the newly generated id for the added coverPoint
      */
    def createCoverPoint(groupId: BigInt, dutPortName: String, dutPort: Data): BigInt = {
        //Sanity check
        if(groupIds contains groupId) {
            //Update the current max id
            lastCoverPointId += 1

            //Add the mappings
            val oldList = groupPointMap getOrElse(groupId, Nil)
            groupPointMap update (groupId, oldList :+ lastCoverPointId)

            coverIdPortMap update (dutPortName, dutPort)
            pointIds append lastCoverPointId

            //Return the coverPoint's id
            lastCoverPointId
        } else {
            throw new IllegalArgumentException("Invalid CoverGroup ID!")
        }
    }

    /**
      * Updates the number of hits done in a given bin
      * @param binId the identifier of the bin we want to increment
      */
    def addBinHit(bin: Bins, value: BigInt): Unit = {
        val newValues = (binIdHitValuesMap.getOrElse(bin, Nil) :+ value).distinct

        binIdHitValuesMap update (bin, newValues)
        binIdNumHitsMap update (bin, newValues.length)
    }

    /**
      * Gets the number of hits form the DB for a given bin id
      * @param binId the unique identifier of the bin
      * @return the number of hits for the given bin
      */
    def getNHits(bin: Bins): BigInt = binIdNumHitsMap getOrElse (bin, 0)

    /**
      * Retrieves a port name given an id
      * @param id the id of the coverPoint that's sampling a port
      * @return a string containing the name of the port
      */
    def getPort(portName: String): Data = coverIdPortMap get portName match {
        case None => throw new IllegalArgumentException(s"$portName is not a registered port name!")
        case Some(port) => port
    }
}
