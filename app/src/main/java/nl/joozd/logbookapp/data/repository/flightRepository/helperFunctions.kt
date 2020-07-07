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

package nl.joozd.logbookapp.data.repository.flightRepository

import android.util.Range
import nl.joozd.logbookapp.data.repository.helpers.isSamedPlannedFlightAs
import nl.joozd.logbookapp.model.dataclasses.Flight
import java.time.Instant

/**
 * Gets all flight from [allFlights] that either :
 * - start between earliest in and latest out in [flightsOnDays]
 * - start between earliest in and latest out in dateRange
 */
fun getFlightsOnDays(allFlights: List<Flight>, flightsOnDays: List<Flight>? = null, dateRange: ClosedRange<Instant>? = null): List<Flight>{
    val period: ClosedRange<Long> = when {
        dateRange != null -> (dateRange.start.epochSecond .. dateRange.endInclusive.epochSecond)
        flightsOnDays != null -> {
            if (flightsOnDays.isEmpty())
                return emptyList()
            val earliestIn = flightsOnDays.minBy { it.timeIn }?.timeOut ?: 0L
            val latestOut = flightsOnDays.maxBy { it.timeOut }?.timeOut ?: 0L
            (earliestIn..latestOut)
        }
        else -> (0L..0L)
    }
    return allFlights.filter { it.timeIn in period }
}

/**
 * Gets a list of flights from [allFlights] that are the same as one or more in [plannedFlights]
 */

fun getFlightsMatchingPlannedFlights(allFlights: List<Flight>, plannedFlights: List<Flight>): List<Flight> {
    val sameDayFlights = getFlightsOnDays(allFlights, plannedFlights)
    return sameDayFlights.filter { pf -> plannedFlights.any { af -> af.isSamedPlannedFlightAs(pf) } }
}

/**
 * Gets a list of flights from [plannedFlights] that are NOT the same as one or more in [allFlights]
 */
fun getNonMatchingPlannedFlights(allFlights: List<Flight>, plannedFlights: List<Flight>): List<Flight>{
    val sameDayFlights = getFlightsOnDays(allFlights, plannedFlights)
    return plannedFlights.filter { pf -> sameDayFlights.none { af -> af.isSamedPlannedFlightAs(pf) } }
}
