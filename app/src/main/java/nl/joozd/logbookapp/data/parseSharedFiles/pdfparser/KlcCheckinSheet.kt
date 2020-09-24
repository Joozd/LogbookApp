/*
 *  JoozdLog Pilot's Logbook
 *  Copyright (c) 2020 Joost Welle
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

package nl.joozd.logbookapp.data.parseSharedFiles.pdfparser

import com.itextpdf.text.pdf.PdfReader
import com.itextpdf.text.pdf.parser.PdfTextExtractor
import com.itextpdf.text.pdf.parser.SimpleTextExtractionStrategy
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.async
import kotlinx.coroutines.withContext
import nl.joozd.logbookapp.data.parseSharedFiles.interfaces.Roster
import nl.joozd.logbookapp.extensions.atEndOfDay
import nl.joozd.logbookapp.extensions.atStartOfDay
import nl.joozd.logbookapp.model.dataclasses.Flight
import java.io.InputStream
import java.time.*
import java.time.format.DateTimeFormatter
import java.util.*
import kotlin.coroutines.CoroutineContext

/**
 * Parses a checkin sheet into flights
 */
class KlcCheckinSheet(roster: String?): Roster {

    /**
     * a "line"looks like:
     * 04Sep KL1076 KLM36U MAN 1245 AMS 1405 01:20 01:10 99 9405 E90/PHEXD 1075 1205 1549 1440 -
     * and has these values:
     * Date Flt Callsign Dep Arr Blk Grd Pax Payload Eq Prev Next Special Info CTOT
     */

    /*********************************************************************************************
     * Private parts
     *********************************************************************************************/

    private val sheetAsText: String? = roster

    private fun makeFlightsList(): List<Flight>? {
        if (!isValid) return null
        val lines = sheetAsText!!.split('\n').map{it.trim()}
        val startOfFlightLines = lines.indexOf(START_OF_FLIGHTS)+1
        val flightLines = lines.drop(startOfFlightLines).take(lines.indexOf(END_OF_FLIGHTS) - startOfFlightLines)
        return flightLines.map{line ->
            lineToFlight(line)
        }
    }

    //TODO add registration to flight?
    private fun lineToFlight(line: String): Flight{
        val dateFormat = DateTimeFormatter.ofPattern("ddMMM", Locale.US)
        val timeFormat = DateTimeFormatter.ofPattern("HHmm")

        val words = line.split(' ')
        val date = MonthDay.parse(words[DATE], dateFormat).atYear(Year.now().value)
        val tOut = LocalDateTime.of(date, LocalTime.parse(words[TIME_OUT],timeFormat)).toInstant(ZoneOffset.UTC).epochSecond
        val tIn = (LocalDateTime.of(date, LocalTime.parse(words[TIME_IN],timeFormat)).toInstant(ZoneOffset.UTC).epochSecond).let{
            if (it > tOut) it else it + ONE_DAY
        }
        return Flight(-1, orig = words[ORIG], dest = words[DEST], timeOut = tOut, timeIn = tIn)
    }


    /*********************************************************************************************
     * Public parts
     *********************************************************************************************/


    /**
     * Identifier of the carrier.
     * See companion object.
     */
    override val carrier: String?
        get() = Roster.KLC_CHECKIN_SHEET

    /**
     * true if the data provided to this parser seems to be valid
     */
    override val isValid: Boolean
        get() = sheetAsText != null && CHECK_LINE_1 in sheetAsText!! && START_OF_FLIGHTS in sheetAsText!! && END_OF_FLIGHTS in sheetAsText!!

    /**
     * List of all flights in this roster.
     * Airports can be ICAO or IATA format
     */
    override val flights: List<Flight>? by lazy {
        makeFlightsList()
    }

    /**
     * The period covered by this roster
     * Should start at start of day and end at end of day
     */
    override val period: ClosedRange<Instant>? by lazy{
        val startEpochSecond = flights?.minByOrNull { it.timeOut }?.timeOut
        val lastEpochSecond = flights?.minByOrNull { it.timeIn }?.timeIn
        if (startEpochSecond == null || lastEpochSecond == null) null
        else {
            (Instant.ofEpochSecond(startEpochSecond).atStartOfDay() .. Instant.ofEpochSecond(lastEpochSecond).atEndOfDay())
        }
    }


    companion object {
        const val ONE_DAY = 86400 // seconds

        const val CHECK_LINE_1 = "Cockpit Briefing for"
        const val START_OF_FLIGHTS =
            "Date Flt Callsign Dep Arr Blk Grd Pax Payload Eq Prev Next Special Info CTOT"
        const val END_OF_FLIGHTS = "Daily Summary"

        const val DATE = 0
        const val FLIGHTNUMBER = 1

        // callsign = 2
        const val ORIG = 3
        const val TIME_OUT = 4
        const val DEST = 5
        const val TIME_IN = 6
        // block = 7
        // ground = 8 NOT ALWAYS THERE so cannot do by position after this
        // For AIRCRAFT will need a regex


        suspend fun ofInputStream(inputStream: InputStream): KlcCheckinSheet {
            @Suppress("BlockingMethodInNonBlockingContext") val reader = try {
                withContext(Dispatchers.IO){ PdfReader(inputStream) } // Dispatchers.IO doesn't have a problem with blocking methods
            } catch (e: Exception) {
                return KlcCheckinSheet(null)
            }
            if (reader.numberOfPages == 0) return KlcCheckinSheet(null)
            val roster = (1..reader.numberOfPages).joinToString("\n") { page ->
                PdfTextExtractor.getTextFromPage(reader, page, SimpleTextExtractionStrategy())
            }
            return KlcCheckinSheet(roster)
        }
    }

}