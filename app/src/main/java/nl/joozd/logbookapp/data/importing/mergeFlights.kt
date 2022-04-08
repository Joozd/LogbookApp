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

package nl.joozd.logbookapp.data.importing

import nl.joozd.logbookapp.model.dataclasses.Flight


/**
 * Merge a list of paired flights.
 * Any non-empty data in first will overwrite that data in second for
 * - flightNumber
 * - times
 * - aircraft type
 * - registration
 * - names
 * - remarks
 * Other things will stay as they were in second flight.
 */
fun mergeFlights(flights: Collection<MatchingFlights>): List<Flight> =
    flights.map { it.newFlight.mergeOnto(it.knownFlight) }

/**
 * Merge a flight on device with a new Flight.
 * non-empty data (including "0 minutes") in [this] will overwrite:
 * - flightNumber
 * - times
 * - aircraft type
 * - registration
 * - names
 * - remarks
 * - multiPilotTime,
 * - ifrTime,
 * - nightTime,
 * - isPIC
 * - isPICUS
 * - isCoPilot,
 * Other things will stay as they were in [flightOnDevice]
 */
private fun Flight.mergeOnto(flightOnDevice: Flight): Flight =
    flightOnDevice.copy(
        timeOut = timeOut,
        timeIn = timeIn,
        flightNumber = flightNumber.nullIfBlank() ?: flightOnDevice.aircraftType,
        aircraftType = aircraftType.nullIfBlank() ?: flightOnDevice.aircraftType,
        registration = registration.nullIfBlank() ?: flightOnDevice.registration,
        name = name.nullIfBlank() ?: flightOnDevice.name,
        name2 = name2.nullIfBlank() ?: flightOnDevice.name2,
        remarks = remarks.nullIfBlank() ?: flightOnDevice.remarks,
        multiPilotTime = multiPilotTime,
        ifrTime = ifrTime,
        nightTime = nightTime,
        isPIC = isPIC,
        isPICUS = isPICUS,
        isCoPilot = isCoPilot
    )

private fun String.nullIfBlank(): String? = takeIf{ it.isNotBlank() }