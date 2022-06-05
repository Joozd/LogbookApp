package nl.joozd.logbookapp.comms

import kotlinx.coroutines.runBlocking
import nl.joozd.joozdlogcommon.DataFilesMetaData
import nl.joozd.joozdlogcommon.comms.Protocol
import nl.joozd.logbookapp.comm.HTTPServer
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Before
import org.junit.Test
import java.io.File

class HTTPServerTest {
    private val testServer = HTTPServer(MOCK_SERVER_ADDRESS)

    private val testData = DataFilesMetaData.fromJSON("""{
    "aircraftTypesVersion": 5,
    "aircraftTypesLocation": "aircrafttypes_5",
    
    "aircraftForcedTypesVersion": 4,
    "aircraftForcedTypesLocation": "forcedtypes_4",
    
    "airportsVersion": 1,
    "airportsLocation": "airports_1"
}""") // this is copypasted from C:\joozdlog\test\joozdlog\datafiles_metadata.json

    @Test
    fun testHTTPServer() = runBlocking {
        println("starting test")
        val serverMetaData = testServer.getDataFilesMetaData()!!
        assertEquals(testData, serverMetaData)
        println("Metadata test OK")

        val aircraftTypes = testServer.getAircraftTypes(serverMetaData)!!
        assert(aircraftTypes.isNotEmpty()) // if not null and not empty, I think it's safe to assume that this works.
        println("Aircraft types test OK, got ${aircraftTypes.size} items")

        val aircraftForcedTypes = testServer.getForcedTypes(serverMetaData)!!
        assert(aircraftForcedTypes.isNotEmpty()) // if not null and not empty, I think it's safe to assume that this works.
        println("Aircraft Forced Types test OK, got ${aircraftForcedTypes.size} items")

        val airports = testServer.getAirports(serverMetaData)!!
        assert(airports.isNotEmpty()) // if not null and not empty, I think it's safe to assume that this works.
        println("airports test OK, got ${airports.size} items")
    }



    companion object{
        private const val MOCK_SERVER_ADDRESS = "file:///c|/joozdlog/test/"
    }
}