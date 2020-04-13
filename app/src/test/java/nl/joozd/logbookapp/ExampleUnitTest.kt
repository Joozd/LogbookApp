package nl.joozd.logbookapp

import nl.joozd.joozdlogcommon.serializing.listFromBytes
import nl.joozd.joozdlogcommon.serializing.mapFromBytes
import nl.joozd.joozdlogcommon.serializing.packList
import nl.joozd.joozdlogcommon.serializing.toByteArray
import nl.joozd.joozdlogcommon.utils.aircraftdbbuilder.AircraftTypesConsensus
import nl.joozd.logbookapp.utils.mostRecentCompleteFlight
import nl.joozd.logbookapp.utils.reverseFlight
import org.junit.Test

import org.junit.Assert.*

/**
 * Example local unit test, which will execute on the development machine (host).
 *
 * See [testing documentation](http://d.android.com/tools/testing).
 */
class ExampleUnitTest {
    /*
    @Test
    fun addition_isCorrect() {
        assertEquals(4, 2 + 2)
    }
    */

//    @Test
    fun stringSerialize(){
        val string = ""
        val bytes = string.toByteArray(Charsets.UTF_8)
        bytes.forEach { print("$it, ") }
        println("${bytes.size}")
        val deserialized = bytes.toString(Charsets.UTF_8)
        assertEquals(string, deserialized)
    }

 //   @Test
    fun packListTest(){
        val bytes = packList(listOf(ByteArray(0)))
        print("bytes size: ${bytes.size}\n")
        bytes.forEach { print("$it, ")}
        true
    }

 //   @Test
    fun serializeListOfStringsAndBack(){
        val testList = listOf ("hoi", "fiets", "banaan", "123", "", "\\n", "\n")
        val bytes = testList.toByteArray()
        val deserialized: List<String> = listFromBytes(bytes)
        println(deserialized)
        assertEquals(testList, deserialized)
    }

 //   @Test
    fun testMapSerializing(){
        val map = listOf(1,2,3,4).zip(listOf("boom", "roos", "vis", "vuur")).toMap()
        val bytes = map.toByteArray()
        val deserialized = mapFromBytes<Int, String>(bytes)
        println(deserialized)
        assertEquals(deserialized, map)
    }


    fun testAircraftWorker(){
        val emptyFile = java.io.File("c:\\temp\\file1")
        val emptyFile2 = java.io.File("c:\\temp\\file2")
        val aircraftFile = java.io.File("c:\\temp\\planes2.dat")
        val atw = AircraftTypesConsensus(aircraftFile, emptyFile, emptyFile2)
        print(atw.forcedMap)

       // val type1 = atw.aircraftTypes.aircraftTypes[77]
        //val type2 = atw.aircraftTypes.aircraftTypes[103]
        /*
        val tc1 = TypeCounter(type1, 2)
        val tc2 = TypeCounter(type1, 3)
        val tc3 = TypeCounter(type1, 4)
        val tc4 = TypeCounter(type2, 1)
        atw.removeCounter("PH-ABC", type1)
        atw.removeCounter("PH-ABC", type2)
        atw.removeCounter("PH-ABC", type2)
        atw.addCounter("PH-ABC", type2)
        print("${atw.consensusMap}")
        atw.writeConsensusMapToFile()
        */
        //atw.addForcedTypeToFile("PH-TST", type1)
        //atw.addForcedTypeToFile("PH-JZD", type2)
    }

}
