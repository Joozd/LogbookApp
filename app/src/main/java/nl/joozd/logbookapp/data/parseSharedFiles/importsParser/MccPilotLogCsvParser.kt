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

package nl.joozd.logbookapp.data.parseSharedFiles.importsParser

import android.util.Log
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import nl.joozd.logbookapp.data.parseSharedFiles.interfaces.ImportedLogbook
import nl.joozd.logbookapp.extensions.atEndOfDay
import nl.joozd.logbookapp.extensions.atStartOfDay
import nl.joozd.logbookapp.extensions.makeLocalDateSmart
import nl.joozd.logbookapp.extensions.makeLocalTime
import nl.joozd.logbookapp.model.dataclasses.Flight
import nl.joozd.logbookapp.utils.TimestampMaker
import java.io.InputStream
import java.lang.Exception
import java.time.Instant
import java.time.LocalDateTime
import java.time.ZoneOffset

/**
 * Takes a list of lines from a CSV file,
 * checks if it is an MCC Pilot Log CSV file as I know it
 * and turns it into a list of [Flight]s
 *
 * You can also get it (async) from an InputStream with [ofInputStream]
 * @param csvLines: List of lines from a CSV file
 * @param lowestId: Lowest usable ID to avoid conflicts with database
 */
class MccPilotLogCsvParser(private val csvLines: List<String>, private val lowestId: Int = 0): ImportedLogbook{
    override val needsCleaning = true

    /*********************************************************************************************
     * Private parts
     *********************************************************************************************/

    private val now = TimestampMaker().nowForSycPurposes

    private val _isValid = csvLines.first() == MCC_PILOT_LOG_CSV_IDENTIFIER

    private val _errorLines = emptyList<String>().toMutableList()

    private fun parseCSV(): List<Flight>? = if (!_isValid) null else {
        csvLines.drop(1).mapIndexed { index, line -> parseLine(line, index + lowestId) }.filterNotNull()
    }

    /**
     * If somebody uses semicolons in remarks, it will break the CSV layout
     * This will attempt to fix it by assuming all extra semicolons should be in the remarks field
     */
    private fun fixExtraSemicolonsInDescription(input: List<String>): List<String>{
        val mutableInput = input.toMutableList()
        while (mutableInput.size > NUMBER_OF_FIELDS){
            mutableInput[POSITION_REMARKS] = "${mutableInput[POSITION_REMARKS]};${mutableInput[POSITION_REMARKS+1]}"
            mutableInput.removeAt(POSITION_REMARKS+1)
        }
        return mutableInput
    }



    private fun parseLine(line: String, id: Int): Flight?{
        val words = fixExtraSemicolonsInDescription(line.split(';'))
        val date = words[POSITION_DATE].makeLocalDateSmart()
        val timeOut = LocalDateTime.of(date, words[POSITION_TIME_OUT].makeLocalTime()).toInstant(ZoneOffset.UTC).epochSecond
        val timeIn = LocalDateTime.of(date, words[POSITION_TIME_IN].makeLocalTime()).toInstant(ZoneOffset.UTC).epochSecond.let{
            if (it < timeOut) it + ONE_DAY else it
        }
        try {
            //TODO do something about balance forward flights? Or don't. I don't know.
            return if (words[POSITION_SIM].isNotBlank()) Flight(
                flightID = id,
                timeOut = date.atStartOfDay().toEpochSecond(ZoneOffset.UTC) + ONE_DAY/2, // noon on [date]
                timeIn = date.atStartOfDay().toEpochSecond(ZoneOffset.UTC) + ONE_DAY/2 + 210*60, // three and a half hours later, not used but better to fill
                simTime = words[POSITION_TOTAL_TIME].toInt(),
                aircraftType = words[POSITION_AIRCRAFT_TYPE],
                name = words[POSITION_NAME_PILOT1],
                name2 = words[POSITION_NAME_PILOT2],
                takeOffDay = words[POSITION_TAKEOFF_DAY].toInt(),
                takeOffNight = words[POSITION_TAKEOFF_NIGHT].toInt(),
                landingDay = words[POSITION_LANDING_DAY].toInt(),
                landingNight = words[POSITION_LANDING_NIGHT].toInt(),
                autoLand = words[POSITION_AUTOLANDS].toInt(),
                remarks = words[POSITION_REMARKS],
                isSim = true,
                isPlanned = false,
                unknownToServer = true,
                autoFill = true,
                timeStamp = now
            )
            else Flight(
                flightID = id,
                orig = words[POSITION_ORIG],
                dest = words[POSITION_DEST],
                timeOut = timeOut,
                timeIn = timeIn,
                correctedTotalTime = words[POSITION_TOTAL_TIME].toInt(),
                nightTime = words[POSITION_NIGHT_TIME].toInt(),
                ifrTime = words[POSITION_TIME_IFR].toInt(),
                simTime = 0,
                aircraftType = words[POSITION_AIRCRAFT_TYPE],
                registration = words[POSITION_REGISTRATION],
                name = words[POSITION_NAME_PILOT1],
                name2 = words[POSITION_NAME_PILOT2],
                takeOffDay = words[POSITION_TAKEOFF_DAY].toInt(),
                takeOffNight = words[POSITION_TAKEOFF_NIGHT].toInt(),
                landingDay = words[POSITION_LANDING_DAY].toInt(),
                landingNight = words[POSITION_LANDING_NIGHT].toInt(),
                autoLand = words[POSITION_AUTOLANDS].toInt(),
                flightNumber = words[POSITION_FLIGHTNUMBER],
                remarks = words[POSITION_REMARKS],
                isPIC = words[POSITION_PIC_TIME].toInt() > 0,
                isPICUS = words[POSITION_PICUS_TIME].toInt() > 0,
                isCoPilot = words[POSITION_COPILOT_TIME].toInt() > 0,
                isDual = words[POSITION_DUAL_TIME].toInt() > 0,
                isInstructor = words[POSITION_INSTRUCTOR_TIME].toInt() > 0,
                isSim = false,
                isPF = words[POSITION_PF] == "True",
                isPlanned = false,
                unknownToServer = true,
                autoFill = true,
                timeStamp = now
            )
        } catch (exception: Exception){ // if any values are in the wrong position so I end up trying to do "EHAM".toInt() or something
            Log.w("parseLine()", "Could not parse line #$id: $line")
            _errorLines.add(line)
            return null
        }
    }

    /*********************************************************************************************
     * Public parts
     *********************************************************************************************/

    override val validImportedLogbook: Boolean
        get() = _isValid

    /**
     * Consider getting this async on a Dispatchers.Default coroutine in case of large logbooks
     */
    override val flights: List<Flight>
        get() = parseCSV() ?: emptyList()
    override val period: ClosedRange<Instant>
        get() =
            if (flights.isEmpty()) Instant.EPOCH..Instant.EPOCH
            else Instant.ofEpochSecond(flights.first().timeOut).atStartOfDay(ZoneOffset.UTC)..Instant.ofEpochSecond(flights.last().timeIn).atEndOfDay(ZoneOffset.UTC)

    override fun close() {
        /// intentionally left blank
    }

    override val errorLines: List<String>
        get() = _errorLines
    override val isValid: Boolean
        get() = _isValid


    /*********************************************************************************************
     * Companion Object
     *********************************************************************************************/

    companion object{
        suspend fun ofInputStream(inputStream: InputStream, lowestId: Int = 0) = withContext(Dispatchers.IO) {
            MccPilotLogCsvParser(inputStream.reader().readLines(), lowestId)
        }

        private const val MCC_PILOT_LOG_CSV_IDENTIFIER = "\"mcc_DATE\";\"Is_PREVEXP\";\"AC_IsSIM\";\"FlightNumber\";\"AF_DEP\";\"TIME_DEP\";\"TIME_DEPSCH\";\"AF_ARR\";\"TIME_ARR\";\"TIME_ARRSCH\";\"AC_MODEL\";\"AC_REG\";\"PILOT1_ID\";\"PILOT1_NAME\";\"PILOT1_PHONE\";\"PILOT1_EMAIL\";\"PILOT2_ID\";\"PILOT2_NAME\";\"PILOT2_PHONE\";\"PILOT2_EMAIL\";\"PILOT3_ID\";\"PILOT3_NAME\";\"PILOT3_PHONE\";\"PILOT3_EMAIL\";\"PILOT4_ID\";\"PILOT4_NAME\";\"PILOT4_PHONE\";\"PILOT4_EMAIL\";\"TIME_TOTAL\";\"TIME_PIC\";\"TIME_PICUS\";\"TIME_SIC\";\"TIME_DUAL\";\"TIME_INSTRUCTOR\";\"TIME_EXAMINER\";\"TIME_NIGHT\";\"TIME_RELIEF\";\"TIME_IFR\";\"TIME_ACTUAL\";\"TIME_HOOD\";\"TIME_XC\";\"PF\";\"TO_DAY\";\"TO_NIGHT\";\"LDG_DAY\";\"LDG_NIGHT\";\"AUTOLAND\";\"HOLDING\";\"LIFT\";\"INSTRUCTION\";\"REMARKS\";\"APP_1\";\"APP_2\";\"APP_3\";\"Pax\";\"DEICE\";\"FUEL\";\"FUELUSED\";\"DELAY\";\"FLIGHTLOG\";\"TIME_TO\";\"TIME_LDG\";\"TIME_AIR\""
        private const val NUMBER_OF_FIELDS = 63


        private const val POSITION_DATE = 0
        private const val POSITION_BALANCE_FORWARD = 1
        private const val POSITION_SIM = 2
        private const val POSITION_FLIGHTNUMBER = 3
        private const val POSITION_ORIG = 4
        private const val POSITION_TIME_OUT = 5
        // unused "scheduled" = 6
        private const val POSITION_DEST = 7
        private const val POSITION_TIME_IN = 8
        // unused "scheduled" = 9
        private const val POSITION_AIRCRAFT_TYPE = 10
        private const val POSITION_REGISTRATION = 11
        private const val POSITION_NAME_PILOT1 = 13
        private const val POSITION_NAME_PILOT2 = 17
        private const val POSITION_TOTAL_TIME = 28
        private const val POSITION_PIC_TIME= 29
        private const val POSITION_PICUS_TIME = 30
        private const val POSITION_COPILOT_TIME = 31
        private const val POSITION_DUAL_TIME = 32
        private const val POSITION_INSTRUCTOR_TIME = 33
        private const val POSITION_NIGHT_TIME = 35
        private const val POSITION_TIME_IFR = 37
        private const val POSITION_PF = 41 // "True" or "False"
        private const val POSITION_TAKEOFF_DAY = 42
        private const val POSITION_TAKEOFF_NIGHT = 43
        private const val POSITION_LANDING_DAY = 44
        private const val POSITION_LANDING_NIGHT = 45
        private const val POSITION_AUTOLANDS = 46
        private const val POSITION_REMARKS = 50


        private const val ONE_DAY = 86400L // seconds


    }


}