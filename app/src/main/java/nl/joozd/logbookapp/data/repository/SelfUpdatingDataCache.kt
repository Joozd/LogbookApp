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

package nl.joozd.logbookapp.data.repository

import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.flow.collect
import kotlinx.coroutines.launch
import nl.joozd.logbookapp.data.repository.aircraftrepository.AircraftDataCache
import nl.joozd.logbookapp.data.repository.aircraftrepository.AircraftRepository
import nl.joozd.logbookapp.data.repository.airportrepository.AirportDataCache
import nl.joozd.logbookapp.data.repository.airportrepository.AirportRepository

/**
 * Holds an Airport- and Aircraft Data Cache, and keeps them up to date
 * as long as coroutineScope is active.
 * Call with as narrow a scope as possible to prevent unnecessary work being done.
 * This is meant only for looking-up, not for keeping track of when something updates.
 * For that, observe respective Flows in Repositories.
 */
class SelfUpdatingDataCache(coroutineScope: CoroutineScope) {
    var airportDataCache = AirportDataCache.empty()
        private set

    var aircraftDataCache = AircraftDataCache.empty()
        private set

    init{
        coroutineScope.launch {
            AircraftRepository.instance.aircraftDataCacheFlow().collect{
                aircraftDataCache = it
            }
        }
        coroutineScope.launch {
            AirportRepository.instance.airportDataCacheFlow().collect{
                airportDataCache = it
            }
        }
    }
}