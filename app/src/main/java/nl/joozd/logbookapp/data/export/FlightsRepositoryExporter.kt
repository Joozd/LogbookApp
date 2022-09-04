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

package nl.joozd.logbookapp.data.export

import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.async
import nl.joozd.joozdlogcommon.BasicFlight
import nl.joozd.logbookapp.data.repository.flightRepository.FlightRepository
import nl.joozd.logbookapp.model.dataclasses.Flight
import nl.joozd.logbookapp.utils.delegates.dispatchersProviderMainScope

/**
 * Exporter class for flights
 * @param flightRepository: Flight Repository to use
 */
class FlightsRepositoryExporter(
    val flightRepository: FlightRepository = FlightRepository.instance
): CoroutineScope by dispatchersProviderMainScope() {
    private val allFlightsAsync = async { flightRepository.getAllFlights().filter{ !it.isPlanned && !it.DELETEFLAG } }

    suspend fun buildCsvString(): String =
        buildCsvString(allFlightsAsync.await())

    private fun buildCsvString(flights: List<Flight>): String =
        BasicFlight.CSV_IDENTIFIER_STRING + "\n" + flights.joinToString("\n") { it.toBasicFlight().toCsv() }
}