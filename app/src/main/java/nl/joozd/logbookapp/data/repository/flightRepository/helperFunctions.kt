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

import nl.joozd.logbookapp.data.repository.helpers.isSameFlightAs
import nl.joozd.logbookapp.data.repository.helpers.isSamedPlannedFlightAs
import nl.joozd.logbookapp.extensions.nullIfBlank
import nl.joozd.logbookapp.model.dataclasses.Flight
import java.time.Instant
import java.time.ZoneOffset

/**
 * Gets all flight from [allFlights] that start between earliest out and latest in in [flightsOnDays]
 */
fun getFlightsOnDays(allFlights: List<Flight>, flightsOnDays: List<Flight>): List<Flight>{
    val earliestIn = flightsOnDays.minByOrNull { it.timeOut }?.tOut()?.toLocalDate()?.atStartOfDay()?.toInstant(ZoneOffset.UTC)?.epochSecond ?: 0L
    val latestOut = flightsOnDays.maxByOrNull { it.timeIn }?.tIn()?.toLocalDate()?.atStartOfDay()?.plusDays(1)?.toInstant(ZoneOffset.UTC)?.epochSecond ?: 0L
    val period = (earliestIn..latestOut)
    return allFlights.filter { it.timeOut in period }
}

/**
 * Gets all flight from [allFlights] that  start between earliest out and latest in in dateRange
 */
fun getFlightsOnDays(allFlights: List<Flight>, dateRange: ClosedRange<Instant>): List<Flight>{
    val period: ClosedRange<Long> =(dateRange.start.epochSecond .. dateRange.endInclusive.epochSecond)
    return allFlights.filter { it.timeOut in period }
}

/**
 * This function will take a list of roster flights and a list of saved flights and will return any saved flights that are updated.
 * It will update saved flights as follows:
 *      - First, it matches a rostered and a saved flight
 *      - If saved flight is Planned, it will ALWAYS overwrite names and aircraft data from roster
 *      - if saved flight is NOT Planned, it will ONLY overwrite names and aircraft data if it was empty.
 * FlightIDs (and all other data) will stay as they were in saved flight, repository will take care of timestamp on saving.
 */
fun updateFlightsWithRosterData(currentFlights: List<Flight>, rosterFlights: List<Flight>): List<Flight> =
    rosterFlights.map{ rf -> currentFlights.first { cf -> cf.isSameFlightAs(rf)} to rf} // make a list of currentFlight to rosterFlight Pairs. first is cf, second is rf.
        .mapNotNull {
            when {
                // Flights are the same -> do nothing
                it.first == it.second -> null

                // Currentflight is planned -> use rostered flight
                it.first.isPlanned -> it.second.let { rf -> it.first.copy(name = rf.name, name2 = rf.name2, registration = rf.registration, aircraftType = rf.aircraftType) }

                // CurrentFlight is not planned, and both names and both aircraft fields are not blank -> do nothing
                with (it.first) { listOf(name, name2, registration, aircraftType).all{ s -> s.isNotBlank()}} -> null

                // Flights are not the same, not planned and not all
                else -> it.second.let { rf ->
                    println("BANAANAPPEL")
                    with(it.first){
                        copy(
                            name = name.nullIfBlank() ?: rf.name,
                            name2 = name2.nullIfBlank() ?: rf.name2,
                            registration = registration.nullIfBlank() ?: rf.registration,
                            aircraftType = aircraftType.nullIfBlank() ?: rf.aircraftType
                        )
                }
            }
        }
    }.also{
        println("BOTERHAM updateFlightsWithRosterData saving ${it.size} flights: ${it.joinToString("\n") { with (it) { "$flightID: $orig - $dest"}}}")
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

/**
 * You can have back-to-back flights (ie. 01:00-01:30 and 01:30-02:00 don't overlap)
 */
fun getOverlappingFlights(allFlights: List<Flight>, flightsToCheck: List<Flight>): List<Flight> = flightsToCheck.filter {f ->
        allFlights.any{
            when{
                it.timeOut < f.timeOut -> it.timeIn > f.timeOut        // if it leaves before and arrives after other flight departed
                it.timeOut >= f.timeIn -> false                          // It leaves after other flight arrived
                else -> true
        }
    }
}


/**
 * Returns overlaps as list of newFlight to knownflight
 */
fun getOverlappingFlightsAsPairs(allFlights: List<Flight>, flightsToCheck: List<Flight>): List<Pair<Flight, Flight>> {
    val overlappingPairs = emptyList<Pair<Flight, Flight>>().toMutableList()
    flightsToCheck.forEach{f ->
        val overlaps = allFlights.filter {
            when{
                it.timeOut < f.timeOut -> it.timeIn > f.timeOut        // if it leaves before and arrives after other flight departed
                it.timeOut >= f.timeIn -> false                          // It leaves after other flight arrived
                else -> true
            }
        }
        overlappingPairs.addAll(overlaps.map{f to it})
    }
    return overlappingPairs
}