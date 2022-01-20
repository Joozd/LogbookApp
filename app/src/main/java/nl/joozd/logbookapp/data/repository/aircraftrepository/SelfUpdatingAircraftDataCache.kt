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
import nl.joozd.joozdlogcommon.AircraftType
import nl.joozd.logbookapp.data.dataclasses.Aircraft
import nl.joozd.logbookapp.data.repository.helpers.formatRegistration
import nl.joozd.logbookapp.utils.DispatcherProvider

/**
 * This caches data for Aircraft; it will update cached data when new data becomes available.
 * NOTE: Either provide it with a [coroutineScope] that has the same lifetime as this object,
 * or close it after using.
 * If failing to do so, it will not be garbage collected and become a memory leak.
 */
class SelfUpdatingAircraftDataCache(
    private val coroutineScope: CoroutineScope // Use coroutineScope with same lifetime as creating object (e.g. viewModelScope)
): CloseableAircraftDataCache {

    //Main Caches; functions will get from this and collectors will update this.
    private var cachedAircraftTypeData: List<AircraftType>? = null

    // registrations are formatted with formatRegistration
    private var registrationToAircraftMap: Map<String, Aircraft> = emptyMap()

    //This is the job that runs when updating
    private val initialUpdateJob = coroutineScope.launch(Dispatchers.IO) {
        registrationToAircraftMap = AircraftRepository.getInstance().registrationToAircraftMap()
    }

    //I know this isn't used but I like to know where these jobs are.
    private val collectionJobs = listOf(
        launchAircraftTypeFlowCollector(),
        launchAircraftMapCollector()
    )

    override suspend fun waitForInitialDataLoad() = initialUpdateJob.join()

    //This can return a partial map if ran while map is being filled
    override fun getRegistrationToAircraftMapOrEmptyMapIfNotLoadedYet(): Map<String, Aircraft> {
        return registrationToAircraftMap
    }

    override fun getAircraftTypes(): List<AircraftType> = cachedAircraftTypeData ?: emptyList()

    override fun close() {
        collectionJobs.forEach {
            it.cancel()
        }
        initialUpdateJob.cancel()
    }

    override fun getAircraftFromRegistration(registration: String?): Aircraft? =
        registration?.let {
            registrationToAircraftMap[formatRegistration(registration)]
        }

    private fun launchAircraftTypeFlowCollector() =
        AircraftRepository.getInstance().aircraftTypesFlow.onEach {
            cachedAircraftTypeData = it
        }.launchIn(coroutineScope + DispatcherProvider.io())

    private fun launchAircraftMapCollector() =
        AircraftRepository.getInstance().aircraftMapFlow.onEach {
            registrationToAircraftMap = it
        }.launchIn(coroutineScope + DispatcherProvider.io())
}