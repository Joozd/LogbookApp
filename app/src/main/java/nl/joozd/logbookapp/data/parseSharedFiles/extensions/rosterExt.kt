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

package nl.joozd.logbookapp.data.parseSharedFiles.extensions

import nl.joozd.logbookapp.data.parseSharedFiles.interfaces.Roster
import nl.joozd.logbookapp.data.parseSharedFiles.pdfparser.ProcessedRoster
import nl.joozd.logbookapp.data.repository.AirportRepository
import nl.joozd.logbookapp.data.repository.aircraftrepository.AircraftRepository

/**
 * Roster Postprocessing
 * From [Roster]:
 *  * Post-processing (not done in Roster but in whatever uses the Roster) should include:
 *  - Changing IATA to ICAO identifiers
 *  - Checking if registration is known, also searching for versions with/without spaces and/or hyphens and changing to known reg + type if found.
 */
suspend fun Roster.postProcess(): ProcessedRoster {
    val iataIcaoMap = AirportRepository.getInstance().getIataIcaoMapAsync().await()
    val newFlights = flights.map{ flight ->
        // In case airports are IATA format, switch them to ICAO.
        // I think there is no need to have that set by RosterParser as there is no overlap between (4 letter) ICAO and (3 letter) IATA codes.
        val orig = iataIcaoMap[flight.orig] ?: flight.orig
        val dest = iataIcaoMap[flight.dest] ?: flight.dest

        /*
         * Priority for aircraft data:
         * 1. If registration from [flight] found in AircraftRepository (Repo), use that registration with type from Repo, ignore any type from Flight
         * 2. Otherwise, use data from [flight]. Any unknown aircraft type data will be handled where it is used.
         */
        val foundAircraft = AircraftRepository.getAircraftFromRegistration(flight.registration)

        // result of lambda:
        flight.copy(
            flightID = -1,
            orig = orig,
            dest = dest,
            registration = foundAircraft?.registration ?: flight.registration,
            aircraftType = foundAircraft?.type?.shortName ?: flight.aircraftType,
            isPlanned = true
        )
    }
    return toProcessedRoster().copy(flights =  newFlights)
}