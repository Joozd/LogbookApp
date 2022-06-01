package nl.joozd.logbookapp.comms

import kotlinx.coroutines.runBlocking
import nl.joozd.joozdlogcommon.DataFilesMetaData
import nl.joozd.joozdlogcommon.Protocol
import nl.joozd.logbookapp.comm.HTTPServer
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Before
import org.junit.Test
import java.io.File

class HTTPServerTest {
    private val testServer = HTTPServer(TEMP_DIR_URL)

    private val testData = DataFilesMetaData(
        1,
        "acTypesLocation",
        2,
        "acForcedLocation",
        3,
        "airportsLocation"
    )

    @Before
    fun setup(){
        println("setting up test")
        File(TEMP_DIR + Protocol.DATAFILES_METADATA_FILENAME).writeText(testData.toString())
    }

    @Test
    fun testHTTPServer() = runBlocking {
        println("starting test")
        assertEquals(testData, testServer.getDataFilesMetaData())
        println("test complete")
    }

    @After
    fun cleanUp(){
        File(TEMP_DIR + Protocol.DATAFILES_METADATA_FILENAME).delete()
        println("cleaned up test")
    }

    companion object{
        private const val TEMP_DIR_URL = "file:///c|/temp/"
        private const val TEMP_DIR = "c:\\temp\\"
    }
}