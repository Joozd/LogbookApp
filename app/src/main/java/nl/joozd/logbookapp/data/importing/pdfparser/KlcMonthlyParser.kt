/*
 *  JoozdLog Pilot's Logbook
 *  Copyright (c) 2020-2022 Joost Welle
 *
 *      This program is free software: you can redistribute it and/or modify
 *      it under the terms of the GNU Affero General Public License as
 *      published by the Free Software Foundation, either version 3 of the
 *      License, or (at your option) any later version.
 *
 *      This program is distributed in the hope that it will be useful,
 *      but WITHOUT ANY WARRANTY; without even the implied warranty of
 *      MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 *      GNU Affero General Public License for more details.
 *
 *      You should have received a copy of the GNU Affero General Public License
 *      along with this program.  If not, see https://www.gnu.org/licenses
 *
 */

package nl.joozd.logbookapp.data.importing.pdfparser

import com.itextpdf.text.pdf.PdfReader
import com.itextpdf.text.pdf.parser.PdfTextExtractor
import com.itextpdf.text.pdf.parser.SimpleTextExtractionStrategy
import java.io.InputStream
import nl.joozd.logbookapp.data.importing.interfaces.CompletedFlights
import nl.joozd.logbookapp.model.dataclasses.Flight
import java.time.*
import java.time.format.DateTimeFormatter

/**
 * Takes an InputStream of a KLC Monthly Overview and does things with that
 * public accessible:
 * [isValid] = true if this seems to be working
 * [flights] = List<Flight>, or null if ![isValid]
 * NOTE: Registrations are currently not correct on Montly Overviews (just last 3 letters), it will return them like that.
 * NOTE: ID's are always -1
 */
@Deprecated("Use KlcMonthlyFile from JoozdlogImport")
class KlcMonthlyParser(private val inputStream: InputStream): CompletedFlights {
    /*********************************************************************************************
     * Private parts: constructor and variables
     *********************************************************************************************/

    /**
     * Misc private data that will be used in this class:
     */

    private val dateRegEx = """\d{2}-\d{2}-\d{4}""".toRegex()
    // commented out due no named regex support
    // private val flightRegEx = """(?<$DAY>\d{1,2}) (?<$FLIGHTNUMBER>[A-Z]{2}\d{3,4}) (?<$TIME_OUT>\d\d:\d\d) (?<$ORIG>[A-Z]{3}) (?<$DEST>[A-Z]{3}) (?<$REGISTRATION>[A-Z]{3}) (?<$TIME_IN>\d\d:\d\d) (?<$TOTAL_TIME>\d\d:\d\d)""".toRegex()
    private val flightRegEx = """(\d{1,2}) ([A-Z]{2}\d{3,4}) (\d\d:\d\d) ([A-Z]{3}) ([A-Z]{3}) ([A-Z]{3}) (\d\d:\d\d) (\d\d:\d\d)""".toRegex()
    //18 KL986 08:40 LCY AMS EXC 09:42 01:02



    /**
     * Data related to the PDF and parsing and being valid and such
     */
    private val reader = PdfReader(inputStream)
    private val firstPage = PdfTextExtractor.getTextFromPage(reader, 1, SimpleTextExtractionStrategy())
    private var _dataSeemsValid: Boolean = false // firstPage.startsWith(KLC_MONTHLY.second)

    //Whole text of document, split by line
    private val text: List<String> by lazy{
        (1..reader.numberOfPages).joinToString("\n") { pageNumber ->
            PdfTextExtractor.getTextFromPage(reader, pageNumber, SimpleTextExtractionStrategy())
        }.split('\n').filter{it.isNotEmpty()}
    }
    private val periodLine = (text.firstOrNull{ it.startsWith(PERIOD_LINE_IDENTIFIER)} ?: "!PERIOD_LINE_IDENTIFIER").also {
        if (dateRegEx.find(it) == null) _dataSeemsValid = false }

    //monthStartInstant = LocalDateTime at midnight at start of month
    private val monthStart: LocalDate by lazy {
        val dateString = dateRegEx.find(periodLine)?.value ?: error ("Date not found - check [validMonthlyOverview] before using this")
        val dateFormatter = DateTimeFormatter.ofPattern("dd-MM-yyyy")
        LocalDate.parse(dateString, dateFormatter).withDayOfMonth(1)
    }

    /*********************************************************************************************
     * Private parts: functions
     *********************************************************************************************/

    private fun buildFlightsList(): List<Flight>{
        val matches = text.mapNotNull { flightRegEx.find(it) }
        return matches.map{ flightLine ->
            val instantOut: Instant = flightLine.timeOut()
                .atDate(monthStart.plusDays(flightLine.day() - 1)) // because month doesn't start at day 0
                .toInstant(ZoneOffset.UTC)
            val instantIn: Instant = flightLine.timeIn()
                .atDate(monthStart.plusDays(flightLine.day() - 1))
                .toInstant(ZoneOffset.UTC).let {
                    if (it < instantOut) it.plusSeconds(ONE_DAY_IN_SECONDS) // in case arriving after midnight
                    else it
                }
            Flight(-1, flightNumber = flightLine.flightNumber(), orig = flightLine.orig(), dest = flightLine.dest(), timeOut = instantOut.epochSecond, timeIn = instantIn.epochSecond, registration = flightLine.reg(), isPlanned = false)
        }

    }

    /**
     * Makes a period from midnight start of Overview to midnight after end of overview
     */
    private fun makePeriod(): ClosedRange<Instant>{
        if (!isValid) return (Instant.EPOCH..Instant.EPOCH).also{ _dataSeemsValid = false}
        val format = DateTimeFormatter.ofPattern("dd-MM-yyyy")
        return dateRegEx.findAll(periodLine).map{LocalDate.parse(it.value, format)}.let{
            it.first().atStartOfDay().toInstant(ZoneOffset.UTC)..it.last().plusDays(1).atStartOfDay().toInstant(ZoneOffset.UTC)
        }

    }

    /*********************************************************************************************
     * Private parts: extension functions
     *********************************************************************************************/

    private val timeFormatter = DateTimeFormatter.ofPattern("HH:mm")
    private fun MatchResult.day(): Long = (this.groups[DAY]?.value ?: error ("ERROR 0003 NO DAY")).toLong()
    private fun MatchResult.flightNumber(): String = (this.groups[FLIGHTNUMBER]?.value ?: error ("ERROR 0003 NO FLIGHT NUMBER"))
    private fun MatchResult.timeOut() = LocalTime.parse(groups[TIME_OUT]?.value ?: error ("ERROR 0001 NO TIME_OUT"), timeFormatter)
    private fun MatchResult.timeIn() = LocalTime.parse(groups[TIME_IN]?.value ?: error ("ERROR 0001 NO TIME_IN"), timeFormatter)
    private fun MatchResult.orig() = (groups[ORIG]?.value ?: error ("ERROR 0004 NO ORIG"))
    private fun MatchResult.dest() = (groups[DEST]?.value ?: error ("ERROR 0005 NO DEST"))
    private fun MatchResult.reg() = (groups[REGISTRATION]?.value?.let { "PH-$it" } ?: error ("ERROR 0005 NO REGISTRATION"))
    /*
    // Not used ATM
    private fun MatchResult.totalTime(): Duration {
        val flightTime = LocalTime.parse(groups[REGISTRATION]?.value ?: error ("ERROR 0005 NO REGISTRATION"), timeFormatter)
        return Duration.between(LocalTime.of(0,0), flightTime)
    }
    */

    /*********************************************************************************************
     * Public values
     *********************************************************************************************/
    override val isValid: Boolean
        get() = _dataSeemsValid

    override val flights: List<Flight>
    get() = if (!isValid) emptyList()
        else buildFlightsList()

    override val period
        get() = makePeriod()

    override fun close() {
        inputStream.close()
    }

    /*********************************************************************************************
     *Companion object
     *********************************************************************************************/

    companion object{
        const val PERIOD_LINE_IDENTIFIER = "Period: From "
/*
        const val DAY = "DAY"
        const val FLIGHTNUMBER = "FLIGHTNUMBER"
        const val TIME_OUT = "TIMEOUT"
        const val ORIG = "ORIG"
        const val DEST = "DEST"
        const val REGISTRATION = "REGISTRATION"
        const val TIME_IN = "TIMEIN"
        const val TOTAL_TIME = "TOTALTIME"

 */
        const val DAY = 1
        const val FLIGHTNUMBER = 2
        const val TIME_OUT = 3
        const val ORIG = 4
        const val DEST = 5
        const val REGISTRATION = 6
        const val TIME_IN = 7
        const val TOTAL_TIME = 8

        const val ONE_DAY_IN_SECONDS = 86400L
    }

}