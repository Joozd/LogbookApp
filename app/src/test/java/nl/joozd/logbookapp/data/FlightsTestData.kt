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

package nl.joozd.logbookapp.data

import nl.joozd.logbookapp.data.export.FlightsRepositoryExporter
import nl.joozd.logbookapp.model.dataclasses.Flight
import java.io.File
import java.time.Instant

object FlightsTestData {
    val prototypeFlight = Flight().copy (isPlanned = false)
    private val now = Instant.now().epochSecond
    val mostRecentCompletedFlight = prototypeFlight.copy(flightID = 1, orig = "EHAM", dest = "EBBR", timeOut = now + 999999, timeIn = now + 999999 + 3600, timeStamp = 1000, unknownToServer = false)
    val mostRecentTimestampFlight = prototypeFlight.copy(flightID = 2, orig = "EHGG", dest = "EHHV", timeOut = now - 10000, timeIn = now - 7000, timeStamp = 999999, unknownToServer = false)
    val deletedFlight = prototypeFlight.copy(flightID = 3, timeOut = now + 6000, timeIn = now + 9000, DELETEFLAG = true, timeStamp = 5000, unknownToServer = false)
    val unknownToServerFlight = mostRecentTimestampFlight.copy(flightID = 7, unknownToServer = true, timeStamp = 4000)
    val plannedFlight = mostRecentTimestampFlight.copy(flightID = 8, timeOut = now + 10000, timeIn = now + 12000, unknownToServer = true, isPlanned = true, timeStamp = 10000)
    val flightWithoutID = prototypeFlight.copy(flightID = Flight.FLIGHT_ID_NOT_INITIALIZED, orig = "EHAM", dest = "EBBR", timeStamp = 1000, unknownToServer = false)

    val flights = listOf(mostRecentCompletedFlight, mostRecentTimestampFlight, deletedFlight, unknownToServerFlight, plannedFlight, flightWithoutID)

    val injectableFlights = listOf(mostRecentCompletedFlight, mostRecentTimestampFlight, deletedFlight, unknownToServerFlight, plannedFlight)

    fun parseFlightsFile(): List<Flight> = FlightsRepositoryExporter.csvToFlights(File("C:\\temp\\joozdlog\\flights.csv").readText(), mock = true)
    fun flightsFileLines(): List<String> = File("C:\\temp\\joozdlog\\flights.csv").readLines()
}