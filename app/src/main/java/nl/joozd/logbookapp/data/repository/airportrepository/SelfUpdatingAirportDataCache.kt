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

package nl.joozd.logbookapp.data.repository.airportrepository

import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.flow.launchIn
import kotlinx.coroutines.flow.onEach
import kotlinx.coroutines.plus
import nl.joozd.logbookapp.data.dataclasses.Airport
import nl.joozd.logbookapp.utils.DispatcherProvider

class SelfUpdatingAirportDataCache(
    private val coroutineScope: CoroutineScope, // Use coroutineScope with same lifetime as creating object (e.g. viewModelScope) or this will leak.
    airportDataCache: AirportDataCache
): AirportDataCache {
    private val cache = castOrConstructUpdateableAirportDataCache(airportDataCache)

    init{
        launchAirportFlowCollector()
    }

    private fun castOrConstructUpdateableAirportDataCache(cache: AirportDataCache)
            : UpdateableAirportDataCache =
        if (cache is UpdateableAirportDataCache) cache
        else UpdateableAirportDataCache(cache)

    private fun launchAirportFlowCollector() =
        AirportRepository.getInstance().airportsFlow().onEach {
            cache.update(it)
        }.launchIn(coroutineScope + DispatcherProvider.io())

    override fun getAirports(): List<Airport> = cache.getAirports()
}