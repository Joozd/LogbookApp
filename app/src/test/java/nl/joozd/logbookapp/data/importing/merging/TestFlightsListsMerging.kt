package nl.joozd.logbookapp.data.importing.merging

import nl.joozd.logbookapp.model.dataclasses.Flight
import org.junit.Assert.assertEquals
import org.junit.Test

class TestFlightsListsMerging{
    private val masterFlight1 = Flight(
        flightID = 1,
        orig = "orig1",
        dest = "dest1",
        registration = "reg1",
        aircraftType = "type1",
        timeOut = 123,
        timeIn = 456
    )
    private val otherFlight1 = Flight(
        flightID = 1,
        orig = "orig1",
        dest = "dest1",
        registration = "reg1",
        aircraftType = "type1",
        timeOut = 123,
        timeIn = 456
    )

    private val masterFlight2 = Flight(
        flightID = 2,
        orig = "orig2",
        dest = "dest2",
        registration = "reg2",
        aircraftType = "type2",
        timeOut = 789,
        timeIn = 101112,
        remarks = "these remarks should overwrite other remarks"
    )

    // same flight as [masterFlight1] but with different ID
    private val otherFlight4 = Flight(
        flightID = 2,
        orig = "orig1",
        dest = "dest1",
        registration = "reg1",
        aircraftType = "type1",
        timeOut = 123,
        timeIn = 456,
        remarks = "otherFlight4"
    )
    private val merged14Result = masterFlight1.copy(remarks = otherFlight4.remarks)


    private val otherFlight3 = Flight(
        flightID = 3,
        orig = "orig1",
        dest = "dest1",
        registration = "reg1",
        aircraftType = "type1",
        timeOut = 123,
        timeIn = 456,
        remarks = "extraRemarksToMerge"
    )

    private val otherFlight2 = Flight(
        flightID = 2,
        orig = "orig2",
        dest = "dest2",
        registration = "reg2",
        aircraftType = "type2",
        timeOut = 789,
        timeIn = 101112,
        remarks = "these remarks should overwrite other remarks"
    )

    // same flight as [masterFlight1] but with different ID; overlaps with ID of masterFlight3 (should end up merged)
    private val otherFlight1OverlapsWith2 = Flight(
        flightID = 2,
        orig = "orig1",
        dest = "dest1",
        registration = "reg1",
        aircraftType = "type1",
        timeOut = 123,
        timeIn = 456,
        remarks = "otherFlight1OverlapsWith2"
    )
    //New flight, should get new ID as it overlaps with masterFlight1
    private val otherFlightNew1 =
        Flight(
            flightID = 1,
            orig = "orig1",
            dest = "destNew",
            registration = "reg1",
            aircraftType = "type1",
            timeOut = 123,
            timeIn = 456,
            remarks = "otherFlightNew"
        )

    //New flight, should get new ID as it has FLIGHT_ID_NOT_INITIALIZED as ID
    private val otherFlightNewNoId =
        Flight(
            flightID = Flight.FLIGHT_ID_NOT_INITIALIZED,
            orig = "orig1",
            dest = "destNew",
            registration = "reg1",
            aircraftType = "type1",
            timeOut = 123,
            timeIn = 456,
            remarks = "otherFlightNew"
        )

    @Test
    fun testMerging(){
        val mf1 = listOf(masterFlight1)
        val mf2 = listOf(masterFlight2)

        //assert merging two different flights works
        assertEquals(mf1 + mf2, mergeFlightsLists(mf1, mf2))

        val of1 = listOf(otherFlight1)

        //assert merging works when the same flight but with something that needs to be merged; e.g. remarks
        assertEquals (mf1, mergeFlightsLists(mf1, of1))

        val mf12 = listOf(masterFlight1, masterFlight2)
        val of4 = listOf(otherFlight4)
        var expected = listOf(merged14Result, masterFlight2)

        assertEquals(expected, mergeFlightsLists(mf12, of4))

        val ofNewId = listOf(otherFlightNewNoId)
        expected = listOf(masterFlight1, otherFlightNewNoId.copy (flightID = 2))
        assertEquals(expected, mergeFlightsLists(mf1, ofNewId))

        val ofNewOverlappingID = listOf(otherFlightNew1)
        expected = listOf(masterFlight1, otherFlightNew1.copy (flightID = 2))
        assertEquals(expected, mergeFlightsLists(mf1, ofNewOverlappingID))
    }


}