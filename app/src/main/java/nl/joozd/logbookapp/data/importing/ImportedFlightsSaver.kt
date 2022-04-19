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

import nl.joozd.joozdlogimporter.dataclasses.ExtractedCompleteLogbook
import nl.joozd.joozdlogimporter.dataclasses.ExtractedCompletedFlights
import nl.joozd.joozdlogimporter.dataclasses.ExtractedPlannedFlights
import nl.joozd.logbookapp.data.importing.results.SaveCompleteLogbookResult
import nl.joozd.logbookapp.data.importing.results.SaveCompletedFlightsResult
import nl.joozd.logbookapp.data.importing.results.SavePlannedFlightsResult
import nl.joozd.logbookapp.data.repository.aircraftrepository.AircraftRepository
import nl.joozd.logbookapp.data.repository.airportrepository.AirportRepository
import nl.joozd.logbookapp.data.repository.flightRepository.FlightRepository
import nl.joozd.logbookapp.data.repository.flightRepository.FlightRepositoryWithUndo

interface ImportedFlightsSaver {
    /**
     * Save a complete logbook (from a backup or import)
     */
    suspend fun save(completeLogbook: ExtractedCompleteLogbook): SaveCompleteLogbookResult
    suspend fun save(completedFlights: ExtractedCompletedFlights): SaveCompletedFlightsResult
    suspend fun save(plannedFlights: ExtractedPlannedFlights): SavePlannedFlightsResult

    companion object{
        val instance by lazy { make() }

        fun make(
            flightsRepo: FlightRepository = FlightRepositoryWithUndo.instance,
            airportsRepo: AirportRepository = AirportRepository.instance,
            aircraftRepo: AircraftRepository = AircraftRepository.instance
        ): ImportedFlightsSaver{
            return ImportedFlightsSaverImpl(flightsRepo, airportsRepo, aircraftRepo)
        }
    }
}