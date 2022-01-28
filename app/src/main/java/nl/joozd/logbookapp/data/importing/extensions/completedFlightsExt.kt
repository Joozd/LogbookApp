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

package nl.joozd.logbookapp.data.importing.extensions

import nl.joozd.logbookapp.data.importing.interfaces.CompletedFlights
import nl.joozd.logbookapp.data.importing.pdfparser.ProcessedCompleteFlights
import nl.joozd.logbookapp.data.repository.aircraftrepository.AircraftRepository
import nl.joozd.logbookapp.data.repository.airportrepository.AirportRepository
import nl.joozd.logbookapp.data.repository.flightRepository.FlightRepositoryImpl
import nl.joozd.logbookapp.data.repository.helpers.setNightTime

/**
 * Process Completed Flights:
 * - make sure Airports are ICAO format
 * - Look for aircraft registrations/types
 * - set isPlanned flag to false
 * - Save to model Class so original CompletedFlights can be closed
 */
suspend fun CompletedFlights.postProcess(): ProcessedCompleteFlights {
    val aircraftDataCache = AircraftRepository.instance.getAircraftDataCache()
    val airportDataCache = AirportRepository.instance.getAirportDataCache()

    val lastFlightWasIFR = mostRecentCompletedFlightInRepositoryIsIFR()

    val newFlights = flights.map { flight ->
        // In case airports are IATA format, switch them to ICAO.
        // I think there is no need to have that set by RosterParser as there is no overlap between (4 letter) ICAO and (3 letter) IATA codes.
        val orig = airportDataCache.iataToIcao(flight.orig) ?: flight.orig
        val dest = airportDataCache.iataToIcao(flight.dest) ?: flight.dest

        val foundAircraft = aircraftDataCache.getAircraftFromRegistration(flight.registration)

        // result of lambda:
        flight.copy(
            orig = orig,
            dest = dest,
            registration = foundAircraft?.registration ?: flight.registration,
            aircraftType = foundAircraft?.type?.shortName ?: flight.aircraftType,
            ifrTime = if (lastFlightWasIFR || flight.ifrTime > 0) flight.calculatedDuration else 0,
            isPlanned = false
        ).setNightTime(airportDataCache)
    }
    return toProcessedCompletedFlights().copy(flights = newFlights)
}

private suspend fun mostRecentCompletedFlightInRepositoryIsIFR() =
    (FlightRepositoryImpl.instance.getMostRecentCompletedFlight()?.ifrTime ?: 1) > 0


