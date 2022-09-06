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

package nl.joozd.logbookapp.data.repository.helpers

import nl.joozd.logbookapp.model.dataclasses.Flight

/**
 * @param allFlights: All flights in logbook. Should incluse those with DELETEFLAG if using [newFlightsWithIDs]
 * @param rosterFlights: Flights from monthly overview to check [allFlights] against
 */
class RosterFlightsChecker(private val allFlights: List<Flight>, private val rosterFlights: List<Flight>) {
    private val timeBracket = ((rosterFlights.minByOrNull { it.timeOut }?.timeOut ?: 0) .. (rosterFlights.maxByOrNull { it.timeIn }?.timeIn ?: 0))
    private val originalFlights = allFlights.filter { it.timeIn in timeBracket || it.timeOut in timeBracket || (it.timeOut < timeBracket.minOrNull()!! && it.timeIn > timeBracket.maxOrNull()!!)}

    private val overlapsList: List<Pair<Flight, List<Flight>>> = rosterFlights.map {f -> f to findOverlapping(f) }


    /**
     * @param f: Flight that may or may not overlap partially or completely, (inclusing larger) with flights in [allFlights]
     * @return those flights in [allFlights]
     */
    private fun findOverlapping(f: Flight): List<Flight>{
        val d = (f.timeOut .. f.timeIn)
        return originalFlights.filter { it.timeOut in d
                || it.timeIn in d
                || (it.timeOut < d.minOrNull()!! && it.timeIn > d.maxOrNull()!!)
        }
    }

    /**
     * @return all flights from [rosterFlights] that do no overlap in any way with flights in [allFlights]
     */
    val newFlights: List<Flight>
        get() = overlapsList.filter{it.second.isEmpty()}.map { it.first }

    /**
     * @return all flights from [rosterFlights] that do not overlap in any way with flights in [allFlights]
     * It will update IDs to unused ones
     */
    val newFlightsWithIDs: List<Flight>
        get() = run{
            val highestID = (allFlights.maxByOrNull { it.flightID }?.flightID ?: 0) + 1
            newFlights.mapIndexed { index: Int, flight: Flight -> flight.copy (flightID = highestID + index)}
        }
}