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

import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.StateFlow
import nl.joozd.joozdlogcommon.AircraftType
import nl.joozd.joozdlogcommon.ForcedTypeData
import nl.joozd.logbookapp.data.dataclasses.Aircraft
import nl.joozd.logbookapp.data.repository.flightRepository.FlightRepository
import nl.joozd.logbookapp.data.room.JoozdlogDatabase

interface AircraftRepository {
    val dataLoaded: StateFlow<Boolean>
    /**
     * Provide a Flow with updates to AircraftTypes in DB
     */
    fun aircraftTypesFlow(): Flow<List<AircraftType>>

    /**
     * Provide a Flow with updates to Registrations To Aircraft map
     * This map holds all known registrations in DB paired with their [Aircraft] data
     */
    fun aircraftMapFlow(): Flow<Map<String, Aircraft>>

    /**
     * Provide a Registrations To Aircraft map
     * This map holds all known registrations in DB paired with their [Aircraft] data
     */
    suspend fun registrationToAircraftMap(): Map<String, Aircraft>

    /**
     * Provide a Flow that emits a new AircraftDataCache object every time
     * an updated one is available.
     */
    fun aircraftDataCacheFlow(): Flow<AircraftDataCache>

    suspend fun getAircraftDataCache(): AircraftDataCache

    /**
     * Get an [AircraftType] from it's short name from DB, null if not found.
     */
    suspend fun getAircraftTypeByShortName(typeShortName: String): AircraftType?

    /**
     * Get and aircraft by registration
     */
    suspend fun getAircraftFromRegistration(registration: String): Aircraft?

    /**
     * Replace entire Types DB with [newTypes]
     */
    suspend fun updateAircraftTypes(newTypes: List<AircraftType>)

    /**
     * Replace entire Preloaded Registrations DB with [newForcedTypes]
     */
    suspend fun updateForcedTypes(newForcedTypes: List<ForcedTypeData>)

    /**
     * Get an aircraft from its registration
     */
    //suspend fun getAircraftFromRegistration(registration: String): Aircraft?

    /**
     * Save and aircraft to DB
     */
    //suspend fun saveAircraft(aircraft: Aircraft)

    companion object{
        val instance: AircraftRepository by lazy {
            AircraftRepositoryImpl(JoozdlogDatabase.getInstance())
        }

        fun mock(mockDatabase: JoozdlogDatabase,
                 mockFlightRepository: FlightRepository = FlightRepository.mock(mockDatabase)
        ): AircraftRepository = AircraftRepositoryImpl(mockDatabase, mockFlightRepository)
    }
}