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

package nl.joozd.logbookapp.data.repository.aircraftrepository

import kotlinx.coroutines.*
import kotlinx.coroutines.flow.launchIn
import kotlinx.coroutines.flow.onEach
import nl.joozd.logbookapp.utils.DispatcherProvider

/**
 * This caches data for Aircraft; it will update cached data when new data becomes available.
 * NOTE: Either provide it with a [CoroutineScope] that has the same lifetime as this object,
 * or this will leak.
 * If there is no suitable CoroutineScope available (like in a function), don't use this.
 */
class SelfUpdatingAircraftDataCache(
    private val coroutineScope: CoroutineScope, // Use coroutineScope with same lifetime as creating object (e.g. viewModelScope) or this will leak.
    aircraftDataCache: AircraftDataCache
): AircraftDataCache {
    private val cache = castOrConstructUpdateableAircraftDataCache(aircraftDataCache)

    init {
        launchAircraftTypeFlowCollector()
        launchAircraftMapCollector()
    }

    override fun getRegistrationToAircraftMap() =
        cache.getRegistrationToAircraftMap()

    override fun getAircraftTypes()=
        cache.getAircraftTypes()

    override fun getAircraftFromRegistration(registration: String?)=
        cache.getAircraftFromRegistration(registration)

    private fun castOrConstructUpdateableAircraftDataCache(cache: AircraftDataCache)
    : UpdateableAircraftDataCache =
        if (cache is UpdateableAircraftDataCache) cache
        else UpdateableAircraftDataCache(cache)

    private fun launchAircraftTypeFlowCollector() =
        AircraftRepository.getInstance().aircraftTypesFlow.onEach {
            cache.updateTypes(it)
        }.launchIn(coroutineScope + DispatcherProvider.io())

    private fun launchAircraftMapCollector() =
        AircraftRepository.getInstance().aircraftMapFlow.onEach {
            cache.updateAircraftMap(it)
        }.launchIn(coroutineScope + DispatcherProvider.io())
}