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

package nl.joozd.logbookapp.data.export

import android.util.Base64
import android.util.Log
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.MainScope
import kotlinx.coroutines.async
import nl.joozd.joozdlogcommon.BasicFlight
import nl.joozd.joozdlogcommon.BasicFlight_version4
import nl.joozd.joozdlogcommon.legacy.basicflight.BasicFlightVersionFunctions.upgrade4to5
import nl.joozd.logbookapp.data.repository.flightRepository.FlightRepository
import nl.joozd.logbookapp.model.dataclasses.Flight
import nl.joozd.logbookapp.utils.TimestampMaker
import java.time.Instant


/**
 * Exporter class for flights
 * TODO upgrade to V5
 */
class FlightsRepositoryExporter(val flightRepository: FlightRepository): CoroutineScope by MainScope() {
    private val allFlightsAsync = async { flightRepository.getAllFlights().filter{ !it.isPlanned} }

    suspend fun buildCsvString(): String = (listOf(FIRST_LINE_V4)  +
        allFlightsAsync.await().map{ it.toCsvV4() }).joinToString("\n")


    private fun Flight.toCsvV4(): String {
        return with (this.toBasicFlight()){
            listOf<String>(
                flightID.toString(),
                orig,
                dest,
                Instant.ofEpochSecond(timeOut).toString(),// from original Flight
                Instant.ofEpochSecond(timeIn).toString(), // from original Flight
                correctedTotalTime.toString(),
                nightTime.toString(),
                ifrTime.toString(),
                simTime.toString(),
                aircraftType,
                registration,
                name,
                name2,
                takeOffDay.toString(),
                takeOffNight.toString(),
                landingDay.toString(),
                landingNight.toString(),
                autoLand.toString(),
                flightNumber,
                remarks,
                isPIC.toString(),
                isPICUS.toString(),
                isCoPilot.toString(),
                isDual.toString(),
                isInstructor.toString(),
                isSim.toString(),
                isPF.toString(),
                isPlanned.toString(),
                // unknownToServer.toString(),
                autoFill.toString(),
                augmentedCrew.toString(),
                // DELETEFLAG,
                // timeStamp,
                Base64.encodeToString(signature.toByteArray(Charsets.UTF_8), Base64.NO_WRAP)
            ).joinToString(";") { it.replace(';', '|') }

        }
    }




    companion object {
        const val MOST_RECENT_VERSION = 5
        const val FIRST_LINE_V4 = "flightID;Origin;dest;timeOut;timeIn;correctedTotalTime;nightTime;ifrTime;simTime;aircraftType;registration;name;name2;takeOffDay;takeOffNight;landingDay;landingNight;autoLand;flightNumber;remarks;isPIC;isPICUS;isCoPilot;isDual;isInstructor;isSim;isPF;isPlanned;autoFill;augmentedCrew;signature"
        const val FIRST_LINE_V5 = "flightID;Origin;dest;timeOut;timeIn;correctedTotalTime;multiPilotTime;nightTime;ifrTime;simTime;aircraftType;registration;name;name2;takeOffDay;takeOffNight;landingDay;landingNight;autoLand;flightNumber;remarks;isPIC;isPICUS;isCoPilot;isDual;isInstructor;isSim;isPF;isPlanned;autoFill;augmentedCrew;signature"

        /**
         *  Read a csv with basicFlights to a list of Flights. Flight ID's will need to be assigned before saving.
         */
        fun csvToFlights(csvBasicFlights: String) = csvToFlights(csvBasicFlights.split('\n'))

        /**
         *  Read a csv with basicFlights to a list of Flights. Flight ID's will need to be assigned before saving.
         *  @param csvBasicFlights = list of lines each containing a csv encoded basicFlight
         */

        fun csvToFlights(csvBasicFlights: List<String>): List<Flight> = when (csvBasicFlights.first()){
            FIRST_LINE_V4 -> csvBasicFlights.drop(1).map{Flight(upgrade4to5(csvFlightToBasicFlightv4(it)))}
            FIRST_LINE_V5 -> csvBasicFlights.drop(1).map{Flight(csvFlightToBasicFlightv5(it))}
            else -> throw (IllegalArgumentException("Not a supported CSV format"))
        }




        private fun csvFlightToBasicFlightv4(csvFlight: String): BasicFlight_version4 = csvFlight.split(';').map{ it.replace('|', ';')}.let { v->
            require(BasicFlight_version4.VERSION.version == 4)
            BasicFlight_version4(
                flightID = -1,
                orig = v[1],
                dest = v[2],
                timeOut = Instant.parse(v[3]).epochSecond,
                timeIn = Instant.parse(v[4]).epochSecond,
                correctedTotalTime = v[5].toInt(),
                nightTime = v[6].toInt(),
                ifrTime = v[7].toInt(),
                simTime = v[8].toInt(),
                aircraft = v[9],
                registration = v[10],
                name = v[11],
                name2 = v[12],
                takeOffDay = v[13].toInt(),
                takeOffNight = v[14].toInt(),
                landingDay = v[15].toInt(),
                landingNight = v[16].toInt(),
                autoLand = v[17].toInt(),
                flightNumber = v[18],
                remarks = v[19],
                isPIC = v[20] == true.toString(),
                isPICUS = v[21] == true.toString(),
                isCoPilot = v[22] == true.toString(),
                isDual = v[23] == true.toString(),
                isInstructor = v[24] == true.toString(),
                isSim = v[25] == true.toString(),
                isPF = v[26] == true.toString(),
                isPlanned = v[27] == true.toString(),
                changed =true,
                autoFill = v[28] == true.toString(),
                augmentedCrew = v[29].toInt(),
                DELETEFLAG = false,
                timeStamp = TimestampMaker.nowForSycPurposes,
                signature = Base64.decode(v[30], Base64.NO_WRAP).toString(Charsets.UTF_8)
            )
        }

        private fun csvFlightToBasicFlightv5(csvFlight: String): BasicFlight = csvFlight.split(';').map{ it.replace('|', ';')}.let { v->
            require(BasicFlight.VERSION.version == 5)
            BasicFlight(
                flightID = -1,
                orig = v[1],
                dest = v[2],
                timeOut = Instant.parse(v[3]).epochSecond,
                timeIn = Instant.parse(v[4]).epochSecond,
                correctedTotalTime = v[5].toInt(),
                multiPilotTime = v[6].toInt(),
                nightTime = v[7].toInt(),
                ifrTime = v[8].toInt(),
                simTime = v[9].toInt(),
                aircraft = v[10],
                registration = v[11],
                name = v[12],
                name2 = v[13],
                takeOffDay = v[14].toInt(),
                takeOffNight = v[15].toInt(),
                landingDay = v[16].toInt(),
                landingNight = v[17].toInt(),
                autoLand = v[18].toInt(),
                flightNumber = v[19],
                remarks = v[20],
                isPIC = v[21] == true.toString(),
                isPICUS = v[22] == true.toString(),
                isCoPilot = v[23] == true.toString(),
                isDual = v[24] == true.toString(),
                isInstructor = v[25] == true.toString(),
                isSim = v[26] == true.toString(),
                isPF = v[27] == true.toString(),
                isPlanned = v[28] == true.toString(),
                changed =true,
                autoFill = v[29] == true.toString(),
                augmentedCrew = v[30].toInt(),
                DELETEFLAG = false,
                timeStamp = TimestampMaker.nowForSycPurposes,
                signature = Base64.decode(v[31], Base64.NO_WRAP).toString(Charsets.UTF_8)
            )
        }

    }
}