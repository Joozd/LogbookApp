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

package nl.joozd.logbookapp.data.parseSharedFiles.importsParser

import android.util.Log
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import nl.joozd.logbookapp.data.parseSharedFiles.interfaces.ImportedLogbook
import nl.joozd.logbookapp.extensions.atEndOfDay
import nl.joozd.logbookapp.extensions.atStartOfDay
import nl.joozd.logbookapp.model.dataclasses.Flight
import nl.joozd.logbookapp.utils.TimestampMaker
import java.io.InputStream
import java.time.Instant
import java.time.LocalDate
import java.time.LocalTime
import java.time.ZoneOffset
import java.time.format.DateTimeFormatter

class LogTenProParser(private val lines: List<String>): ImportedLogbook {
    private val _errorLines = emptyList<String>().toMutableList()

    override val needsCleaning = false

    override val validImportedLogbook: Boolean by lazy{
        lines.isNotEmpty() && lines.first().containsAll(DATE_IDENT, FLIGHT_NUMBER_IDENT, ORIG_IDENT, DEST_IDENT, NAME2_IDENT, TIME_OUT_IDENT, TIME_IN_IDENT, NAME_IDENT, NAME2_IDENT, TOTAL_TIME_IDENT,
                                                        PIC_IDENT, COPILOT_IDENT, COPILOT_TIME_IDENT, PIC_TIME_IDENT, NIGHT_TIME_IDENT, LANDING_DAY_IDENT, LANDING_NIGHT_IDENT, REGISTRATION_IDENT,
                                                        SIM_TIME_IDENT, MULTIPILOT_IDENT, TAKEOFF_DAY_IDENT, TAKEOFF_NIGHT_IDENT, IFR_TIME_IDENT, PF_IDENT, REMARKS_IDENT, TYPE_IDENT)
    }

    /**
     * List of flights
     * null means a line that failed to import but didn't break the other flights
     */
    override val flights: List<Flight>
        get() = buildFlights() ?: emptyList()
    override val period: ClosedRange<Instant>
        get() =
            if (flights.isEmpty()) Instant.EPOCH..Instant.EPOCH
            else Instant.ofEpochSecond(flights.first().timeOut).atStartOfDay(ZoneOffset.UTC)..Instant.ofEpochSecond(flights.last().timeIn).atEndOfDay(ZoneOffset.UTC)

    override fun close() {
        //intentionally left blank
    }

    override val errorLines: List<String>
        get() = _errorLines
    override val isValid: Boolean
        get() = validImportedLogbook

    /**
     * Build a list of Flights from a list of Strings
     * All lines presenting an error are sent to [_errorLines]
     */
    private fun buildFlights(): List<Flight>?{
        if (!validImportedLogbook) return null
        val positions = Positions(lines.first())
        return lines.drop(1).map{line -> makeFlight(line, positions)?: null.also{_errorLines.add(line)}  }.filterNotNull()
    }

    private fun makeFlight(line: String, positions: Positions): Flight?{
        val entries = splitToWords(line).also{
            Log.d("makeFlight()", "This flight has ${it.size} data points")
        }
        return try {
            //make time out/in from date and parsing
            val now = TimestampMaker.nowForSycPurposes

            val timeFormatter = DateTimeFormatter.ofPattern("HH:mm")
            val dateFormatter = DateTimeFormatter.ofPattern("yyyy-MM-dd")

            val localTimeOut = LocalTime.parse(entries[positions.timeOut], timeFormatter)
            val localTimeIn = LocalTime.parse(entries[positions.timeIn], timeFormatter)
            val date = LocalDate.parse(entries[positions.date], dateFormatter)

            val timeOut = localTimeOut.atDate(date).toInstant(ZoneOffset.UTC).epochSecond
            val timeIn = localTimeIn.atDate(date).toInstant(ZoneOffset.UTC).epochSecond.let { if (it > timeOut) timeOut else timeOut + ONE_DAY_IN_SECONDS }
            with(positions) {
                Flight(
                    -1,
                    flightNumber = entries[flightNumber],
                    orig = entries[orig],
                    dest = entries[dest],
                    name = entries[name],
                    name2 = entries[name2],
                    timeOut = timeOut,
                    timeIn = timeIn,
                    correctedTotalTime = entries[totalTime].floatingHoursToMinutes(),
                    nightTime = entries[nightTime].floatingHoursToMinutes(),
                    simTime = entries[simTime].floatingHoursToMinutes(),
                    multiPilotTime = entries[multiPilot].floatingHoursToMinutes(),
                    landingDay = entries[landingDay].makeInt(),
                    landingNight = entries[landingNight].makeInt(),
                    takeOffDay = entries[takeoffDay].makeInt(),
                    takeOffNight = entries[takeoffNight].makeInt(),
                    ifrTime = entries[ifrTime].floatingHoursToMinutes(),
                    isPF = entries[isPF].isNotEmpty(),
                    isPIC = entries[isPic].isNotEmpty(),
                    isCoPilot = entries[isCopilot].isNotEmpty(),
                    remarks = entries[remarks],
                    registration = entries[registration],
                    aircraftType = entries[type],
                    isSim = entries[simTime].isNotEmpty(),
                    isPlanned = false,
                    unknownToServer = true,
                    autoFill = false,
                    timeStamp = now
                )
            }
        }

         catch(e: Exception){
            Log.w("LogTenProParser", "caught error in line $line: \n${e.stackTraceToString()}")
            null
        }
    }

    /**
     * Helper function to check if all required headers are in the header string
     */
    @Suppress("SameParameterValue") // yes I know. Its for me.
    private fun String.containsAll(vararg requirements: String): Boolean =
        requirements.all{it.trim() in splitToWords(this) }

    /**
     * Split a header line (tab-separated values) to a list of strings and trim them.
     */
    private fun splitToWords(line: String): List<String> = line.split('\t').map{ it.trim() }

    /**
     * Take a value like '1,7' and return that amount of hours as minutes (102 minutes)
     */
    private fun String.floatingHoursToMinutes(): Int = if (this.isEmpty()) 0 else (this.replace(',', '.').toFloat()*60).toInt()

    /**
     * Parse an int, 0 if empty.
     */
    private fun String.makeInt() = if (this.isEmpty()) 0 else this.toInt()

    /**
     * Take a header line and transform that to the positions of the data we are looking for.
     * All indices should have been checked through [validImportedLogbook]
     */
    private inner class Positions(headerLine: String){
        private val headers = splitToWords(headerLine).also{
            Log.d("Positions", "Found ${it.size} headers")
        }
        val date = headers.indexOf(DATE_IDENT).also{Log.d("Position", "index found at $it")}
        val flightNumber = headers.indexOf(FLIGHT_NUMBER_IDENT)
        val orig = headers.indexOf(ORIG_IDENT)
        val dest = headers.indexOf(DEST_IDENT)
        val name = headers.indexOf(NAME_IDENT).also{Log.d("Position", "index found at $it")}
        val name2 = headers.indexOf(NAME2_IDENT)
        val timeOut = headers.indexOf(TIME_OUT_IDENT)
        val timeIn = headers.indexOf(TIME_IN_IDENT)
        val totalTime = headers.indexOf(TOTAL_TIME_IDENT)
        val picTime = headers.indexOf(PIC_TIME_IDENT)
        val coPilotTime = headers.indexOf(COPILOT_TIME_IDENT).also{Log.d("Position", "index found at $it")}
        val nightTime = headers.indexOf(NIGHT_TIME_IDENT)
        val simTime = headers.indexOf(SIM_TIME_IDENT)
        val multiPilot = headers.indexOf(MULTIPILOT_IDENT)
        val landingDay = headers.indexOf(LANDING_DAY_IDENT)
        val landingNight = headers.indexOf(LANDING_NIGHT_IDENT)
        val takeoffDay = headers.indexOf(TAKEOFF_DAY_IDENT)
        val takeoffNight = headers.indexOf(TAKEOFF_NIGHT_IDENT).also{Log.d("Position", "index found at $it")}
        val ifrTime = headers.indexOf(IFR_TIME_IDENT)
        val isPF = headers.indexOf(PF_IDENT)
        val isPic = headers.indexOf(PIC_IDENT)
        val isCopilot = headers.indexOf(COPILOT_IDENT)
        val remarks = headers.indexOf(REMARKS_IDENT).also{Log.d("Position", "index found at $it")}
        val registration = headers.indexOf(REGISTRATION_IDENT).also{Log.d("Position", "index found at $it")}
        val type = headers.indexOf(TYPE_IDENT)


    }



    companion object{
        private const val DATE_IDENT = "flight_flightDate"
        private const val FLIGHT_NUMBER_IDENT = "flight_flightNumber"
        private const val ORIG_IDENT = "flight_from"
        private const val DEST_IDENT = "flight_to"
        private const val NAME_IDENT = "flight_selectedCrewPIC"
        private const val NAME2_IDENT = "flight_selectedCrewSIC"
        private const val TIME_OUT_IDENT = "flight_actualDepartureTime"
        private const val TIME_IN_IDENT = "flight_actualArrivalTime"
        private const val TOTAL_TIME_IDENT = "flight_totalTime" // Float in Hours
        private const val PIC_TIME_IDENT = "flight_pic" // float, >0 = true
        private const val COPILOT_TIME_IDENT = "flight_sic" // Float in Hours
        private const val NIGHT_TIME_IDENT = "flight_night" //idem
        private const val SIM_TIME_IDENT = "flight_simulator" // idem
        private const val MULTIPILOT_IDENT = "flight_multiPilot" // float in hours
        private const val LANDING_DAY_IDENT = "flight_dayLandings"
        private const val LANDING_NIGHT_IDENT = "flight_nightLandings"
        private const val TAKEOFF_DAY_IDENT = "flight_dayTakeoffs"
        private const val TAKEOFF_NIGHT_IDENT = "flight_nightTakeoffs"
        private const val IFR_TIME_IDENT = "flight_ifr"
        private const val PF_IDENT = "flight_pilotFlyingCapacity"
        private const val PIC_IDENT = "flight_picCapacity"
        private const val COPILOT_IDENT = "flight_sicCapacity"
        private const val REMARKS_IDENT = "flight_remarks"
        private const val REGISTRATION_IDENT = "aircraft_aircraftID"
        private const val TYPE_IDENT = "aircraftType_type"

        private const val ONE_DAY_IN_SECONDS = 86400


        suspend fun ofInputStream(inputStream: InputStream) = withContext(Dispatchers.IO) {
            LogTenProParser(inputStream.bufferedReader().readLines())
        }
    }
}