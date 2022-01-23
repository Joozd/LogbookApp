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

package nl.joozd.logbookapp.model.viewmodels.activities.mainActivity

import androidx.lifecycle.viewModelScope
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.launch
import nl.joozd.logbookapp.data.repository.aircraftrepository.AircraftDataCache
import nl.joozd.logbookapp.data.repository.airportrepository.AirportDataCache
import nl.joozd.logbookapp.data.repository.flightRepository.FlightDataCache
import nl.joozd.logbookapp.model.viewmodels.JoozdlogViewModel

class MainActivityViewModelNew: JoozdlogViewModel() {
    // We keep an up-to-date reference to all airports, flights and aircraft in this viewModel.
    // Fragments can take them from here and pass them on to their own viewModel.
    var airportDataCache: AirportDataCache = AirportDataCache.empty(); private set
    var flightDataCache: FlightDataCache = FlightDataCache.empty(); private set
    var aircraftDataCache: AircraftDataCache = AircraftDataCache.empty(); private set
    private val cacheUpdateJobs = listOf( // I like knowing where to find those even if val is not used.
        collectUpdatedFlightDataCaches(),
        collectUpdatedAirportDataCaches(),
        collectUpdatedAircraftDataCaches()
    )

    // Using the List from flightDataCache saves us from keeping the same list twice in memory
    val flightsToDisplay = flightRepository.flightDataCacheFlow().map { it.flights }


    private fun collectUpdatedFlightDataCaches() =
        viewModelScope.launch {
            flightRepository.flightDataCacheFlow().collect {
                flightDataCache = it
            }
        }

    private fun collectUpdatedAirportDataCaches() =
        viewModelScope.launch {
            airportRepository.airportDataCacheFlow().collect {
                airportDataCache = it
            }
        }

    private fun collectUpdatedAircraftDataCaches() =
        viewModelScope.launch {
            aircraftRepository.aircraftDataCacheFlow().collect {
                aircraftDataCache = it
            }
        }
}