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

package nl.joozd.joozdlogimporter.supportedFileTypes.extractors

import nl.joozd.joozdlogcommon.BasicFlight
import nl.joozd.joozdlogimporter.interfaces.PlannedFlightsExtractor
import java.time.*
import java.time.format.DateTimeFormatter
import java.util.*

class KlcBriefingSheetExtractor: PlannedFlightsExtractor {
    override fun getPeriodFromLines(lines: List<String>): ClosedRange<Long>? {
        val flights = getFlightsFromLines(lines) ?: return null
        val start = flights.minOfOrNull { it.timeOut } ?: return null
        val end = flights.maxOfOrNull { it.timeIn } ?: return null
        return (Instant.ofEpochSecond(start).epochSecond..Instant.ofEpochSecond(end).epochSecond)
    }

    override fun extractFlightsFromLines(lines: List<String>): Collection<BasicFlight>? {
        val flightsWithoutCrew = getFlightsFromLines(lines) ?: return null
        val crewLines = getCrewLines(lines) ?: return null
        val myName = getMyName(lines)

        return addNamesToFlights(flightsWithoutCrew, crewLines, myName)
    }

    private fun getFlightsFromLines(lines: List<String>): List<BasicFlight>? {
        val start = lines.indexOf(START_OF_FLIGHTS)+1
        val end = lines.indexOf(END_OF_FLIGHTS)
        if (start < 0  || end < 0) return null
        return lines.subList(start, end)
            .filter{l ->
                l.split(' ').getOrNull(TIME_OUT)?.all{it.isDigit()} ?: false
            } // bit hacky way to catch lines that are actually a flight (not RESH/RESK for instance)
            .map { lineToFlight(it) }
    }

    /*
     * This grabs the part starting with "Crew Info" and then returns all lines with names information
     * (= all lines starting with a date + flightnumber)
     *
     * Example data:
     *
     * Crew Info
     * Date Flight CP FO SE ST
     * PREV EVENT NO PREV EVENT NO PREV EVENT NO PREV EVENT NO PREV EVENT
     * 17Feb KL1199 44692 WELLE, JOOST 59852 JONGEPIER, SHAHANE 67769 CROES, TESSA 42919 JAHNIG, RON
     * 18Feb KL1196
     * 19Feb KL1554 44692 59852 67769 42919
     * 19Feb KL1739
     * 19Feb KL1740
     * NEXT EVENT NO NEXT EVENT NO NEXT EVENT NO NEXT EVENT NO NEXT EVENT
     * ADDITIONAL INFORMATION
     */
    private fun getCrewLines(lines: List<String>): List<String>? {
        if (START_OF_CREW_INFO !in lines) return null
        // Matches lines starting with a date (eg 17Feb)
        val nameRegex = """^\d\d[A-Z][a-z]{2}.*""".toRegex()
        return lines.drop(lines.indexOf(START_OF_CREW_INFO))
            .filter { it matches nameRegex }
    }

    private fun getMyName(lines: List<String>): String {
        val l = lines.first()
        return l.substring(MY_NAME_START.length..l.indexOf(MY_NAME_END)).trim().uppercase()
    }

    private fun addNamesToFlights(flights: List<BasicFlight>, crewLines: List<String>, myName: String): List<BasicFlight>{
        val newFlights = ArrayList<BasicFlight>(flights.size)

        // a map of all found crew as (psn to name)
        val crewPsnToNameMap = makeCrewPsnToNameMap(crewLines, myName)

        // List of most recently found crew, to be used until a new crew is found
        var currentCrew = emptyList<String>()

        crewLines.forEach{ line ->
            /*
             * Every line is a flight
             * examples:
             * 04Sep KL991 44692 53917 63034 74061 DINTEN VAN SPIJK, REBECCA
             * 04Sep KL992
             *
             * If there are numbers in a line, that means a crew change.
             * If a number is followed by another number, that function stays the same person
             * If it followed by [A-Z] that is a new function, whose name is all text until next number
             * Names are written in reverse order, separated by commas (VRIES, DE, HENK), all caps
             * should become "Xxxxxxx, xxx xxx xxx, Xxxxxx Xxxx Xxxx" (X = uppercase, x = lowercase)
             */
            // update crewList

            if (line.getNumbers().isNotEmpty()){
                currentCrew = line.getNumbers().map{psn -> crewPsnToNameMap[psn] ?: "ERROR"}
            }
            val name = currentCrew.firstOrNull() ?: ""
            val name2 = if (currentCrew.size > 1) currentCrew.drop(1).joinToString(";") else ""

            //Now, we have all names for the flight that goes with this line. Next: Find that flight


            //dateString = 09Sep, flightNumber = KL123
            val (dateString, flightNumber) = line.split(' ').let {it[0] to it[1]}
            val date = getDate(dateString)

            flights.firstOrNull{it.hasDate(date) && it.flightNumber == flightNumber}?.let{
                newFlights.add(it.copy(name = name, name2 = name2, isPIC = name == MY_NAME))
            }
        }
        return newFlights
    }

    private fun makeCrewPsnToNameMap(crewLines: List<String>?, myName: String): Map<Int, String> =
        HashMap<Int,String>().apply {
            crewLines?.forEach { line ->
                putNames(line, myName)
            }
        }

    private fun MutableMap<Int, String>.putNames(line: String, myName: String) {
        val numbers = line.getNumbers()
        if (numbers.isEmpty()) return
        numbers.indices.forEach { i ->
            val n = numbers[i].toString()
            val raw = if (i != numbers.indices.last) {
                val next = numbers[i + 1].toString()
                //raw is all text between this number and the next
                line.substring(line.indexOf(n) + n.length until line.indexOf(next)).trim()
            }
            else line.drop(line.indexOf(n) + n.length).trim()

            if (raw.isNotBlank())
                set(numbers[i], rawNameToName(raw, myName).toString())
        }
    }


    /**
     * Takes a RawName ("JANSEN, JAN", of "VRIES, VAN DE, HENK"
     * and turns it into a Name
     * If name == myName, makes it [MY_NAME]
     */
    private fun rawNameToName(rawName: String, myName: String): Name =
        with (Name.ofList(rawName.split(',').map{ it.trim() })){
            if (formattedAsMyName == myName) Name(MY_NAME) else this
        }

    // Gets all words that are only numbers, "hallo ab123 456 7" will return [456, 7]
    private fun String.getNumbers(): List<Int> = split(' ')
        .filter{ s -> s.all { it.isDigit() } }
        .map{it.toInt()}

    private fun lineToFlight(line: String): BasicFlight{
        val aircraftRegex = "/([A-Z]{5})".toRegex()

        val words = line.split(' ')
        val date = getDate(words[DATE])
        val tOut = getTime(date, words[TIME_OUT])
        val tIn = getTimeIn(date, words[TIME_IN], tOut)
        val registration = aircraftRegex.find(line)?.groupValues?.get(1)?.let{
            it.take(2) + "-" + it.drop(2) // change "PHEZP"to "PH-EZP" to match forced types from server
        } ?: ""
        return BasicFlight.PROTOTYPE.copy(
            flightNumber = words[FLIGHTNUMBER],
            orig = words[ORIG],
            dest = words[DEST],
            timeOut = tOut,
            timeIn = tIn,
            registration = registration
        )
    }

    private fun getTime(date: LocalDate, timeString: String): Long {
        val formatter = DateTimeFormatter.ofPattern("HHmm")
        val time = LocalTime.parse(timeString, formatter)
        return ZonedDateTime.of(date, time, ZoneOffset.UTC).toEpochSecond()
    }

    private fun getTimeIn(date: LocalDate, timeString: String, tOut: Long): Long {
        val t = getTime(date, timeString)
        return if (t > tOut) t else t + ONE_DAY
    }

    private fun getDate(dateString: String): LocalDate {
        val dateFormat = DateTimeFormatter.ofPattern("ddMMM", Locale.US)
        return MonthDay.parse(dateString, dateFormat).atYear(Year.now().value)
    }

    private fun BasicFlight.hasDate(wantedDate: LocalDate) =
        LocalDateTime.ofEpochSecond(timeOut, 0, ZoneOffset.UTC).toLocalDate() == wantedDate

        companion object{
        const val ONE_DAY = 86400 // seconds

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
    }

}