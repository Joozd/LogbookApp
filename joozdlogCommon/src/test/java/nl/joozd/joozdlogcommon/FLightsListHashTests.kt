package nl.joozd.joozdlogcommon

import org.junit.Assert.assertNotEquals
import org.junit.Assert.assertEquals
import org.junit.Test
import kotlin.random.Random
import kotlin.system.measureNanoTime

class FLightsListHashTests {
    private val list1 = listOf<Long>(1,2,3,4,5)
    private val list2 = listOf<Long>(1,2,3,4,5)
    private val list3 = listOf<Long>(1,2,3,3,6)
    private val list4 = listOf<Long>(5,4,3,2,1)

    private fun checkSums(list: List<List<Long>>) = list.map {FlightsListChecksum.checksum(it)}


    @Test
    fun testHashFunction(){
        println("checksums:")
        println(FlightsListChecksum.checksum(list1))
        println(FlightsListChecksum.checksum(list2))
        println(FlightsListChecksum.checksum(list3))
        println(FlightsListChecksum.checksum(list4))
        assertEquals(FlightsListChecksum.checksum(list1), FlightsListChecksum.checksum(list2))
        assertNotEquals(FlightsListChecksum.checksum(list1), FlightsListChecksum.checksum(list3))
        assertNotEquals(FlightsListChecksum.checksum(list1), FlightsListChecksum.checksum(list4))
        val tenThousand = buildRandomList(15000, 1000)
        val checkSums = checkSums(tenThousand)
        val time = measureNanoTime {
            checkSums.forEachIndexed{ i1, first ->
                if (i1%1000 == 0)
                    println(i1)
                checkSums.forEachIndexed{ i2, second ->
                    if (i1 != i2) {
                        if (first == second) {
                            println("$first matches $second - i1: $i1, i2: $i2")
                            println("${tenThousand[i1]} matches ${tenThousand[i2]}")
                        }

                    }
                }
            }
        }
        println("10K took ${time/1000000} millis")
    }

    private fun buildRandomList(amount: Int, length: Int): List<List<Long>> = (0..amount).map{
        (0..length).map { Random.nextLong() }
    }
}
