package nl.joozd.joozdlogcommon.utilities

import org.junit.Assert.assertEquals
import org.junit.Test

class CheckForDuplicates {
    private val testDataFile = "C:\\joozdlog\\wip\\meelworm.csv"



    @Test
    fun testCheckForDuplicates(){
        val flights = getSampleFlights(testDataFile)
        val duplicates = checkForDuplicates(flights)
        val originals = flights.filter { it !in duplicates }
        assertEquals(flights.sortedBy { it.flightID }, (duplicates + originals).sortedBy { it.flightID })
        assert(duplicates.none { it in originals})
        assert(originals.none { it in duplicates})
        assert(originals.all { it in flights})
        assert(duplicates.all { it in flights})
    }
}

