/*
 * JoozdLog Pilot's Logbook
 * Copyright (C) 2020 Joost Welle
 *
 *     This program is free software: you can redistribute it and/or modify
 *     it under the terms of the GNU Affero General Public License as
 *     published by the Free Software Foundation, either version 3 of the
 *     License, or (at your option) any later version.
 *
 *     This program is distributed in the hope that it will be useful,
 *     but WITHOUT ANY WARRANTY; without even the implied warranty of
 *     MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 *     GNU Affero General Public License for more details.
 *
 *     You should have received a copy of the GNU Affero General Public License
 *     along with this program.  If not, see https://www.gnu.org/licenses
 */

package nl.joozd.logbookapp.data.utils

import nl.joozd.logbookapp.data.dataclasses.Flight
import nl.joozd.logbookapp.data.sharedPrefs.Preferences


fun flightAirportsToIata(flights: List<Flight>, pairs: List<Pair<String, String>>): List<Flight> {
    var fixedFlights: List <Flight> = emptyList()
    val airportsMap = pairs.associate { it }
    flights.forEach {
        fixedFlights += it.copy(orig = airportsMap[it.orig] ?: it.orig, dest = airportsMap[it.dest] ?: it.dest)
    }
    return fixedFlights
}

/*
fun flightAirportToIata(flight: Flight, airportsMap: Map<String, String>): Flight {
    return flight.copy(orig = airportsMap[flight.orig] ?: flight.orig, dest = airportsMap[flight.dest] ?: flight.dest)
}
*/

fun flightAirportToIataIfThatIsSet(flight: Flight, airportsMap: Map<String, String>): Flight =
    if (Preferences.useIataAirports)
        flight.copy(orig = airportsMap[flight.orig] ?: flight.orig, dest = airportsMap[flight.dest] ?: flight.dest)
    else flight
