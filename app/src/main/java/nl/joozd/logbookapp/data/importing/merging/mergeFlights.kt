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

package nl.joozd.logbookapp.data.importing.merging

import nl.joozd.listmerger.ListMerger
import nl.joozd.logbookapp.data.importing.MatchingFlights
import nl.joozd.logbookapp.extensions.nullIfBlank
import nl.joozd.logbookapp.extensions.nullIfZero
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
fun Flight.mergeOnto(flightOnDevice: Flight, keepIdOfFlightOnDevice: Boolean = true): Flight =
    flightOnDevice.copy(
        flightID = if (keepIdOfFlightOnDevice) flightOnDevice.flightID else flightID,
        timeOut = timeOut,
        timeIn = timeIn,
        flightNumber = flightNumber.nullIfBlank() ?: flightOnDevice.flightNumber,
        aircraftType = aircraftType.nullIfBlank() ?: flightOnDevice.aircraftType,
        registration = registration.nullIfBlank() ?: flightOnDevice.registration,
        name = name.nullIfBlank() ?: flightOnDevice.name,
        name2 = name2.nullIfBlank() ?: flightOnDevice.name2,
        remarks = remarks.nullIfBlank() ?: flightOnDevice.remarks,
        multiPilotTime = chooseMultiPilotTime(this, flightOnDevice),
        ifrTime = chooseIfrTime(this, flightOnDevice),
        nightTime = chooseNightTime(this, flightOnDevice),
        isPIC = decideIsPic(this, flightOnDevice),
        isPICUS = decideIsPicus(this, flightOnDevice),
        isCoPilot = decideIfCopilot(this, flightOnDevice)
    )

fun mergeFlightsLists(
    masterFlights: List<Flight>,
    otherFlights: List<Flight>,
) = ListMerger(
    masterList = masterFlights,
    otherList = otherFlights,
    compareStrategy = OrigDestAircraftAndTimesCompareStrategy(),
    mergingStrategy = MergeOntoMergingStrategy(),
    idUpdatingStrategy = IncrementFlightIDStrategy((masterFlights + otherFlights).maxOf{it.flightID}, masterFlights)
).merge()

/* Try to be smart about getting the wanted data:
 * - if autofill, set it to new data if not zero, or old data if new data is zero.
 * - if not autofill, set duration if it is either 0 or whole flight to the same ratio.
 * -- If not 0 or whole flight, keep manually entered value.
 */
private fun chooseMultiPilotTime(newData: Flight, oldData: Flight): Int =
    if (oldData.autoFill)
        newData.multiPilotTime.nullIfZero() ?: oldData.multiPilotTime
    else when (oldData.multiPilotTime){
        0 -> 0
        oldData.duration() -> newData.duration()
        else -> oldData.multiPilotTime
    }

private fun chooseIfrTime(newData: Flight, oldData: Flight): Int =
    if (oldData.autoFill)
        newData.ifrTime.nullIfZero() ?: oldData.ifrTime
    else when (oldData.ifrTime){
        0 -> 0
        oldData.duration() -> newData.duration()
        else -> oldData.ifrTime
    }

private fun chooseNightTime(newData: Flight, oldData: Flight): Int =
    if (oldData.autoFill)
        newData.ifrTime.nullIfZero() ?: oldData.nightTime
    else when (oldData.nightTime){
        0 -> 0
        oldData.duration() -> newData.duration()
        else -> oldData.nightTime
    }


private fun decideIsPic(newData: Flight, oldData: Flight): Boolean =
    newData.isPIC || oldData.isPIC

private fun decideIsPicus(newData: Flight, oldData: Flight): Boolean =
    newData.isPICUS || oldData.isPICUS

private fun decideIfCopilot(newData: Flight, oldData: Flight): Boolean =
    if (chooseMultiPilotTime(newData, oldData) == 0) false
    else !decideIsPic(newData, oldData)

