package nl.joozd.joozdlogimporter

import nl.joozd.joozdlogimporter.supportedFileTypes.*
import org.junit.Assert.assertEquals
import org.junit.Test
import java.io.File

class CsvImporterTests {
    private val joozdLogV5BadFileName = "C:\\joozdlog\\test\\importables\\joozdlog_joozd_v5_bad.csv"
    private val joozdLogV5FileName = "C:\\joozdlog\\test\\importables\\joozdlog_joozd_v5.csv"

    private val mccPilotLog1FileName = "C:\\joozdlog\\test\\importables\\mccPilotLog_1.csv"
    private val mccPilotLogJoozdFileName = "C:\\joozdlog\\test\\importables\\mccPilotLog_joozd.csv"

    private val logtenPro1File = "C:\\joozdlog\\test\\importables\\logten_pro_1.txt"
    private val logtenProJoozdFile = "C:\\joozdlog\\test\\importables\\logten_pro_joozd_fixed.txt"

    @Test
    fun testJoozdlogV5Bad(){
        val lines = File(joozdLogV5BadFileName).readLines()
        val file = CsvImporter(lines).getFile()
        assert(file is CurrentJoozdlogCsvFile)
        val flights = getFlightsFromFile(file)
        assertEquals(lines.size - 1, flights!!.size)
        println("JOOZDLOG V5 BAD VERSION PARSED OK - tested with ${lines.size -1} flights.")
    }

    @Test
    fun testJoozdlogV5(){
        val lines = File(joozdLogV5FileName).readLines()
        val file = CsvImporter(lines).getFile()
        assert(file is CurrentJoozdlogCsvFile)
        val flights = getFlightsFromFile(file)
        assertEquals(lines.size - 1, flights!!.size)
        println("JOOZDLOG V5 PARSED OK - tested with ${lines.size -1} flights.")
    }

    @Test
    fun testMccPilotLog1(){
        val lines = File(mccPilotLog1FileName).readLines()
        val file = CsvImporter(lines).getFile()
        assert(file is MccPilotLogFile)
        val flights = getFlightsFromFile(file)
        assertEquals(lines.size - 1, flights!!.size)
        println("MCCPILOTLOG 1 PARSED OK - tested with ${lines.size -1} flights.")
    }

    @Test
    fun testMccPilotLogJoozd(){
        val lines = File(mccPilotLogJoozdFileName).readLines()
        val file = CsvImporter(lines).getFile()
        assert(file is MccPilotLogFile)
        val flights = getFlightsFromFile(file)
        assertEquals(lines.size - 1, flights!!.size)
        println("MCCPILOTLOG JOOZD PARSED OK - tested with ${lines.size -1} flights.")
    }

    @Test
    fun testLogtenPro1(){
        val lines = File(logtenPro1File).readLines()
        val file = CsvImporter(lines).getFile()
        assert(file is LogTenProFile)
        val flights = getFlightsFromFile(file)
        assertEquals(lines.size - 1, flights!!.size)
        println("LOGTEN PRO 1 PARSED OK - tested with ${lines.size -1} flights.")
    }

    @Test
    fun testLogtenProJoozd(){
        val lines = File(logtenProJoozdFile).readLines()
        val file = CsvImporter(lines).getFile()
        assert(file is LogTenProFile)
        val flights = getFlightsFromFile(file)
        assertEquals(lines.size - 1, flights!!.size)
        println("LOGTEN PRO JOOZD PARSED OK - tested with ${lines.size -1} flights.")
    }

    private fun getFlightsFromFile(file: ImportedFile) =
        (file as CompleteLogbookFile).extractCompletedFlights().flights
}