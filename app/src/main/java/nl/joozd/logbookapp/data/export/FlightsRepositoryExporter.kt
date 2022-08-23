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

package nl.joozd.logbookapp.data.export

import android.util.Base64
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.async
import nl.joozd.joozdlogcommon.BasicFlight
import nl.joozd.joozdlogcommon.BasicFlight_version4
import nl.joozd.joozdlogcommon.legacy.basicflight.BasicFlightVersionFunctions.upgrade4to5
import nl.joozd.logbookapp.data.repository.flightRepository.FlightRepository
import nl.joozd.logbookapp.model.dataclasses.Flight
import nl.joozd.logbookapp.utils.TimestampMaker
import nl.joozd.logbookapp.utils.delegates.dispatchersProviderMainScope
import java.time.Instant


/**
 * Exporter class for flights
 * @param flightRepository: Flight Repository to use
 * @param mock: True if testing without Android environment (bypasses Preferences)
 */
class FlightsRepositoryExporter(
    val flightRepository: FlightRepository,
    private val mock: Boolean = false
): CoroutineScope by dispatchersProviderMainScope() {
    private val allFlightsAsync = async { flightRepository.getAllFlights().filter{ !it.isPlanned} }

    suspend fun buildCsvString(): String =
        FIRST_LINE_V5 + "\n" + allFlightsAsync.await().joinToString("\n") { it.toCsvV5() }

    private fun Flight.toCsvV5(): String {
        return with (this.toBasicFlight()){
            listOf<String>(
                flightID.toString(),
                orig,
                dest,
                Instant.ofEpochSecond(timeOut).toString(),// from original Flight
                Instant.ofEpochSecond(timeIn).toString(), // from original Flight
                correctedTotalTime.toString(),
                multiPilotTime.toString(),
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
        const val FIRST_LINE_V5 = "flightID;Origin;dest;timeOut;timeIn;correctedTotalTime;multiPilotTime;nightTime;ifrTime;simTime;aircraftType;registration;name;name2;takeOffDay;takeOffNight;landingDay;landingNight;autoLand;flightNumber;remarks;isPIC;isPICUS;isCoPilot;isDual;isInstructor;isSim;isPF;isPlanned;autoFill;augmentedCrew;signature"
    }
}