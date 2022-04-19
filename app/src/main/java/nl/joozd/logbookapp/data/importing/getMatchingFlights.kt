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
 * Match flights from [knownFlights] and [newFlights].
 * Matches will be returned as pairs - flight1 to flight2
 * This matches same times, orig and dest.
 * If multiple flights match, it will only match the first found for every flight in flights1.
 */
fun getMatchingFlightsExactTimes(
    knownFlights: Collection<Flight>,
    newFlights: Collection<Flight>
): List<MatchingFlights> =
    knownFlights.mapNotNull { knownFlight ->
        knownFlight.findExactTimesMatchIn(newFlights)?.let{ newFlight ->
            MatchingFlights(
                knownFlight = knownFlight,
                newFlight = newFlight
            )
        }
    }

/**
 * returns a list of those [flightsLookingForMatch] that do not match any flights in [flightsToMatchTo]
 * when matching times, orig and dest.
 */
fun getNonMatchingFlightsExactTimes(
    flightsToMatchTo: Collection<Flight>,
    flightsLookingForMatch: Collection<Flight>
): List<Flight> =
    flightsLookingForMatch.filter { !it.containsExactTimesMatchIn(flightsToMatchTo) }


/**
 * Match flights from [knownFlights] and [newFlights].
 * Matches will be returned as MatchingFlights - flight1 to flight2
 * This matches same day, flightNumber, orig and dest.
 * If multiple flights match, it will only match the first found for every flight in flights1.
 */
fun getMatchingFlightsSameDay(
    knownFlights: Collection<Flight>,
    newFlights: Collection<Flight>
): List<MatchingFlights> =
    knownFlights.mapNotNull { knownFlight ->
        knownFlight.findSameDayMatchIn(newFlights)?.let { newFlight ->
            MatchingFlights(
                knownFlight = knownFlight,
                newFlight = newFlight
            )
        }
    }

/**
 * returns a list of those [newFlights] that do not match any flights in [flightsToMatchTo]
 * when matching day, flightNumber, orig and dest.
 */
fun getNonMatchingFlightsSameDay(
    flightsToMatchTo: Collection<Flight>,
    newFlights: Collection<Flight>
): List<Flight> =
    newFlights.filter { !it.containsSameDayMatchIn(flightsToMatchTo) }


private fun Flight.containsExactTimesMatchIn(
    flightsToMatchTo: Collection<Flight>,
) = flightsToMatchTo.any { it matchesTimeOrigAndDestWith this }

private fun Flight.findExactTimesMatchIn(flightsToMatchTo: Collection<Flight>) =
    flightsToMatchTo.firstOrNull { it matchesTimeOrigAndDestWith this }

private fun Flight.findSameDayMatchIn(flightsToMatchTo: Collection<Flight>) =
    flightsToMatchTo.firstOrNull { it matchesDateFlightnumberOrigAndDestWith this }

private fun Flight.containsSameDayMatchIn(
    flightsToMatchTo: Collection<Flight>,
) = flightsToMatchTo.any { it matchesDateFlightnumberOrigAndDestWith this }

private infix fun Flight.matchesDateFlightnumberOrigAndDestWith(other: Flight): Boolean =
    this.orig.trim() == other.orig.trim()
            && this.dest.trim() == other.dest.trim()
            && this.flightNumber.trim() == other.flightNumber.trim()
            && this.date() == other.date()


private infix fun Flight.matchesTimeOrigAndDestWith(other: Flight): Boolean =
    this.orig == other.orig
    && this.dest == other.dest
    && this.timeOut == other.timeOut
    && this.timeIn == other.timeIn