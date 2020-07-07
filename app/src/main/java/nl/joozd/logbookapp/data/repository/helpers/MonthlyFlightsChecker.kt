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

package nl.joozd.logbookapp.data.repository.helpers

import nl.joozd.logbookapp.data.sharedPrefs.Preferences
import nl.joozd.logbookapp.model.dataclasses.Flight
import kotlin.math.absoluteValue

/**
 * @param allFlights: All flights in logbook. Should incluse those with DELETEFLAG if using [newFlightsWithIDs]
 * @param monthlyFlights: Flights from monthly overview to check [allFlights] against
 * @param tolerance: Amount of minutes that can be updated without notice
 */
class MonthlyFlightsChecker(private val allFlights: List<Flight>, private val monthlyFlights: List<Flight>, private val tolerance: Int) {
    private val timeBracket = ((monthlyFlights.minBy{ it.timeOut }?.timeOut ?: 0) .. (monthlyFlights.maxBy{ it.timeIn }?.timeIn ?: 0))
    private val originalFlights = allFlights.filter { !it.DELETEFLAG && (it.timeIn in timeBracket || it.timeOut in timeBracket || (it.timeOut < timeBracket.min()!! && it.timeIn > timeBracket.max()!!))}

    /**
     * List of Pairs [monthlyFlights] to all matching [allFlights]
     */
    private val overlapsList: List<Pair<Flight, List<Flight>>> = monthlyFlights.map {f -> f to findOverlapping(f) }

    /**
     * Flights can be updated if they have the same
     *  - orig,
     *  - dest,
     *  - flightNumber, and
     *   * timeOut and timeIn match within [tolerance] minutes
     *
     *  Updated flights will keep their flightID!
     */
    val updatedFlights: List<Flight>
        get() = overlapsList
            .filter {overlap ->
                overlap.second.size == 1
                && overlap.second.all{ it.matchesCloseEnough(overlap.first)} }
            .map { it.second.first().copy (timeOut = it.first.timeOut, timeIn = it.first.timeIn, registration = it.first.registration, aircraftType = it.first.aircraftType) } // can add aircraft without checking because it is checked in [matchesCloseEnough()]

    /**
     * @return list of Flights that match exactly with
     */
    val correctFlights: List<Flight>
        get() = overlapsList.filter {overlap -> overlap.second.all{ it.isSameAs(overlap.first)} }.map { it.first }


    /**
     * @return list of conflicting flight pairs (first from [monthlyFlights], second from [allFlights])
     */
    val conflictingFlights: List<Pair<Flight, Flight>>
        get() = overlapsList.map{ overlap ->
            overlap.first to overlap.second
                .filter{!it.matchesCloseEnough(overlap.first)} }        // now we have an overlap with only non-matches
            .map{overlap -> overlap.second.map { overlap.first to it}}
            .flatten()


    /**
     * @return all flights from [monthlyFlights] that do no overlap in any way with flights in [allFlights]
     */
    val newFlights: List<Flight>
        get() = overlapsList.filter{it.second.isEmpty()}.map { it.first }

    /**
     * @return all flights from [monthlyFlights] that do no overlap in any way with flights in [allFlights]
     * It will update IDs to unused ones
     */
    val newFlightsWithIDs: List<Flight>
        get() = run{
            val highestID = (allFlights.maxBy { it.flightID }?.flightID ?: 0) + 1
            newFlights.mapIndexed { index: Int, flight: Flight -> flight.copy (flightID = highestID + index)}
        }


    /**
     * @param f: Flight that may or may not overlap partially or completely, (inclusing larger) with flights in [allFlights]
     * @return those flights in [allFlights]
     */
    private fun findOverlapping(f: Flight): List<Flight>{
        val d = (f.timeOut .. f.timeIn)
        return originalFlights.filter { it.timeOut in d
                || it.timeIn in d
                || (it.timeOut < d.min()!! && it.timeIn > d.max()!!)
        }
    }

    private fun Flight.isSameAs(f: Flight) =
        orig == f.orig
        && dest == f.dest
        && timeOut == f.timeOut
        && timeIn == f.timeIn
        && flightNumber == f.flightNumber
        && if (Preferences.updateAircraftWithoutAsking) true else {
            registration == f.registration
            && aircraftType == f.aircraftType
        }

    private fun Flight.matchesCloseEnough(f: Flight) =
        orig == f.orig
        && dest == f.dest
        && (timeOut-f.timeOut).absoluteValue <= tolerance * 60
        && timeIn-f.timeIn.absoluteValue <= tolerance * 60
        && flightNumber == f.flightNumber
        && if (Preferences.updateAircraftWithoutAsking) true else {
            registration == f.registration
            && aircraftType == f.aircraftType
        }
}