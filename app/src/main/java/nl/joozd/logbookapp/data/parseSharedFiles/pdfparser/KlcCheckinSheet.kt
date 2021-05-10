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

import android.util.Log
import androidx.core.text.isDigitsOnly
import com.itextpdf.text.pdf.PdfReader
import com.itextpdf.text.pdf.parser.PdfTextExtractor
import com.itextpdf.text.pdf.parser.SimpleTextExtractionStrategy
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import nl.joozd.logbookapp.data.parseSharedFiles.interfaces.Roster
import nl.joozd.logbookapp.extensions.atEndOfDay
import nl.joozd.logbookapp.extensions.atStartOfDay
import nl.joozd.logbookapp.model.dataclasses.Flight
import java.io.InputStream
import java.time.*
import java.time.format.DateTimeFormatter
import java.util.*

/**
 * Parses a checkin sheet into flights
 * TODO change when name is SELF (search for WIP_SELF)
 */

class KlcCheckinSheet(roster: String?): Roster {
    private val aircraftRegex = "/([A-Z]{5})".toRegex()
    private val dateFormat = DateTimeFormatter.ofPattern("ddMMM", Locale.US)
    private val timeFormat = DateTimeFormatter.ofPattern("HHmm")


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

    private fun makeFlightsList(): List<Flight> {
        if (!isValid) return emptyList()
        val lines = sheetAsText!!.split('\n').map{it.trim()}
        val startOfFlightLines = lines.indexOf(START_OF_FLIGHTS)+1
        val flightLines = lines.drop(startOfFlightLines).take(lines.indexOf(END_OF_FLIGHTS) - startOfFlightLines)
            .filter{it.split(' ')[TIME_OUT].all{it.isDigit()}} // bit hacky way to catch lines that are actually a flight (not RESH/RESK for instance)
        val crewLines = getCrewLines(lines)

        return addNames(flightLines.map{line -> lineToFlight(line)}, crewLines, myName = getMyName(lines))
    }

    //TODO add registration to flight?
    private fun lineToFlight(line: String): Flight{
        Log.d("Line", line)

        val words = line.split(' ')
        val date = MonthDay.parse(words[DATE], dateFormat).atYear(Year.now().value)
        val tOut = LocalDateTime.of(date, LocalTime.parse(words[TIME_OUT],timeFormat)).toInstant(ZoneOffset.UTC).epochSecond
        val tIn = (LocalDateTime.of(date, LocalTime.parse(words[TIME_IN],timeFormat)).toInstant(ZoneOffset.UTC).epochSecond).let{
            if (it > tOut) it else it + ONE_DAY
        }
        val registration = aircraftRegex.find(line)?.groupValues?.get(1)?.let{
            it.take(2) + "-" + it.drop(2) // change "PHEZP"to "PH-EZP" to match forced types from server
        } ?: ""
        return Flight(-1, flightNumber = words[FLIGHTNUMBER], orig = words[ORIG], dest = words[DEST], timeOut = tOut, timeIn = tIn, registration = registration)
    }

    /**
     * Adds names found in [lines] to corresponding Flights
     */
    private fun addNames(flights: List<Flight>, lines: Sequence<String>, myName: String): List<Flight>{
        val newFlights = ArrayList<Flight>()

        // a map of all found crew as (psn to name)
        val crewList = HashMap<Int, String>()

        // List of most recently found crew, to be used untill a new crew is found
        var currentCrew = emptyList<String>()

        lines.forEach{ line ->
            Log.d("addNames", "line $line")
            /**
             * Every line is a flight
             * examples:
             * 04Sep KL991 44692 53917 63034 74061 DINTEN VAN SPIJK, REBECCA
             * 04Sep KL992
             *
             * If there are numbers in a line, that means a crew change.
             * If a number is followed by another number, that function stays the same person
             * If it followed by [A-Z] that is a new function, whose name is all text until next number
             * Names are written in reverse order, separated by commas (VRIES, DE, HENK), all caps
             * should become "Ullllll, lll lll lll, Ulllll Ulll Ulll" (U = uppercase, l = lowercase)
             */
            // update crewList
            line.putNamesInMap(crewList, myName)
            if (line.getNumbers().isNotEmpty()){
                currentCrew = line.getNumbers().map{psn -> crewList[psn] ?: "ERROR"}
            }
            val name = currentCrew.firstOrNull() ?: ""
            val name2 = if (currentCrew.size > 1) currentCrew.drop(1).joinToString(";") else ""

            //Now, we have all names for the flight that goes with this line. Next: Find that flight


            //dateString = 09Sep, flightNumber = KL123
            val (dateString, flightNumber) = line.split(' ').let {it[0] to it[1]}
            val date = MonthDay.parse(dateString, dateFormat).atYear(Year.now().value)

            flights.firstOrNull{it.tOut().toLocalDate() == date && it.flightNumber == flightNumber}?.let{
                newFlights.add(it.copy(name = name, name2 = name2, isPIC = name == MY_NAME))
            }
        }
        return newFlights
    }

    private fun getMyName(lines: List<String>) = lines.first().drop(MY_NAME_START.length).let{truncated->
        truncated.take(truncated.indexOf(MY_NAME_END))
    }


    /**
     * Private helper functions for parsing crew names
     */
    private fun getCrewLines(lines: List<String>) =
        """\d\d[A-Z][a-z]{2}.*""".toRegex()
            .findAll(lines.drop(lines.indexOf(START_OF_CREW_INFO)).joinToString("\n"))
            .map{it.value}

    private fun String.getNumbers(): List<Int> = split(' ').filter{it.isDigitsOnly()}.map{it.toInt()}

    private fun String.putNamesInMap(map: MutableMap<Int, String>, myName: String) {
        getNumbers().forEach {
            Log.d("putNamesInMap", "number $it")
            //rawName is all text from current Number until first next Digit
            val rawName = drop(indexOf(it.toString()) + (it.toString().length)).let { s ->
                Log.d("putNamesInMap", "S: $s")
                s.take(s.firstOrNull {c -> c.isDigit() }?.let {digit -> s.indexOf(digit) } ?: 999)
            }.trim().also{
                Log.d("putNamesInMap", "rawname $it")
            }
            if (rawName.isNotBlank())
                map[it] = rawNameToName(rawName, myName).toString()
        }
    }

    /**
     * Takes a RawName ("JANSEN, JAN", of "VRIES, VAN DE, HENK"
     * and turns it into a Name
     */
    private fun rawNameToName(rawName: String, myName: String): Name =
        with (Name.ofList(rawName.split(','))){
            if (checkMyName == myName) Name(MY_NAME) else this
        }


    /**
     * Helper class for names.
     */
    private data class Name(val first: String = "", val last: String = "", val middle: String = "") {
        val checkMyName = (if (middle.isNotEmpty()) "$first $last, $middle" else "$first $last")
            .uppercase(Locale.ROOT)

        override fun toString() = listOf(first, middle.lowercase(Locale.ROOT), last).filter { !it.isBlank() }.joinToString(" ")

        companion object {
            /**
             * A complete name is 2 or 3 names long (Jan-Henk Nicolaas, van de, Wilde Wetering)
             * If 2 names, all are words are Capitalized, else only first and last
             */
            fun ofList(names: List<String>): Name {
                return when (names.size) {
                    0 -> Name()
                    1 -> Name(names.first().withCapital())
                    2 -> Name(capitalizeAllWords(names[1]), capitalizeAllWords(names[0]))
                    else -> // 3 or more, ignore any words past [2]
                        Name(capitalizeAllWords(names[2]), capitalizeAllWords(names[0]), names[1].lowercase(Locale.ROOT))
                }
            }

            private fun capitalizeAllWords(line: String): String = line.split(' ')
                .filter { !it.isBlank() } // remove any extra spaces
                .joinToString(" ") {
                    it.split('-')
                        .joinToString("-") { it.withCapital() }
                }.trim()

            private fun String.withCapital(): String = lowercase(Locale.ROOT).capitalize(Locale.ROOT)
        }

    }

    /*********************************************************************************************
     * Public parts
     *********************************************************************************************/


    /**
     * Identifier of the carrier.
     * See companion object.
     */
    override val carrier: String
        get() = Roster.KLC_CHECKIN_SHEET

    /**
     * true if the data provided to this parser seems to be valid
     */
    override val isValid: Boolean
        get() = sheetAsText != null && CHECK_LINE_1 in sheetAsText && START_OF_FLIGHTS in sheetAsText && END_OF_FLIGHTS in sheetAsText

    /**
     * List of all flights in this roster.
     * Airports can be ICAO or IATA format
     */
    override val flights: List<Flight> by lazy {
        makeFlightsList()
    }

    /**
     * The period covered by this roster
     * Should start at start of day and end at end of day
     */
    override val period: ClosedRange<Instant> by lazy{
        val startEpochSecond = flights.minByOrNull { it.timeOut }?.timeOut
        val lastEpochSecond = flights.maxByOrNull { it.timeIn }?.timeIn
        if (startEpochSecond == null || lastEpochSecond == null) (Instant.EPOCH..Instant.EPOCH)
        else {
            (Instant.ofEpochSecond(startEpochSecond).atStartOfDay() .. Instant.ofEpochSecond(lastEpochSecond).atEndOfDay())
        }
    }

    /**
     * This Roster doesn't keep an inputStream open.
     */
    override fun close() {
        // Intentionally left blank
    }


    companion object {
        const val ONE_DAY = 86400 // seconds

        const val CHECK_LINE_1 = "Cockpit Briefing for"
        const val START_OF_FLIGHTS =
            "Date Flt Callsign Dep Arr Blk Grd Pax Payload Eq Prev Next Special Info CTOT"
        const val END_OF_FLIGHTS = "Daily Summary"
        const val START_OF_CREW_INFO = "Crew Info"
        const val MY_NAME_START = "Cockpit Briefing for "
        const val MY_NAME_END = " KLC AUTO BRIEFING"

        const val MY_NAME = "SELF"

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