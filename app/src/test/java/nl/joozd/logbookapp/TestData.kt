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

package nl.joozd.logbookapp

import nl.joozd.joozdlogcommon.AircraftType
import nl.joozd.logbookapp.data.export.FlightsRepositoryExporter
import nl.joozd.logbookapp.model.dataclasses.Flight
import java.io.File

object TestData {
    val aircraftTypes = listOf(
        AircraftType("Test Aircraft 1 (MP/ME)", "TAC1", multiPilot = true, multiEngine = true),
        AircraftType("Test Aircraft 2 (SP/SE)", "TAC2", multiPilot = false, multiEngine = false)
    )

    fun parseFlightsFile(): List<Flight> = FlightsRepositoryExporter.csvToFlights(File("C:\\temp\\joozdlog\\flights.csv").readText(), mock = true)
    fun flightsFileLines(): List<String> = File("C:\\temp\\joozdlog\\flights.csv").readLines()
}