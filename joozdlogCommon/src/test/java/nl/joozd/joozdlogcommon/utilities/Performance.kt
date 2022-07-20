package nl.joozd.joozdlogcommon.utilities

import nl.joozd.joozdlogcommon.BasicFlight
import org.junit.Assert.assertEquals
import org.junit.Test
import kotlin.system.measureNanoTime

class Performance {
    private val testDataFile1 = "C:\\joozdlog\\wip\\meelworm.csv"
    private val testDataFile2 = "C:\\joozdlog\\wip\\joozd.csv"


    private fun testCheckForDuplicates(flights: List<BasicFlight>){
        val duplicates = checkForDuplicates(flights)
        val originals = flights.filter { it !in duplicates }
        assertEquals(flights.sortedBy { it.flightID }, (duplicates + originals).sortedBy { it.flightID })
        assert(duplicates.none { it in originals})
        assert(originals.none { it in duplicates})
        assert(originals.all { it in flights})
        assert(duplicates.all { it in flights})
    }

    @Test
    fun test(){
        val flights = getSampleFlights(testDataFile1)
        val t1 = measureNanoTime {
            repeat(100) {
                testCheckForDuplicates(flights)
                //testCheckForDuplicates1(testDataFile2)
            }
        }
        println(t1)
    }
}