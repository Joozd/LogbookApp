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

import kotlinx.coroutines.flow.Flow
import nl.joozd.logbookapp.data.dataclasses.Airport
import nl.joozd.logbookapp.data.room.JoozdlogDatabase

interface AirportRepository {
    /**
     * Provide a flow with updates when airport Database gets updated
     */
    fun airportsFlow(): Flow<List<Airport>>

    /**
     * Provide an initialized [AirportDataCache]
     */
    suspend fun getAirportDataCache(): AirportDataCache

    /**
     * Provide a flow of updated AirportDataCaches
     */
    fun airportDataCacheFlow(): Flow<AirportDataCache>

    /**
     * Get last loaded AirportDataCache.
     * As airportData doesn't change very often this should probably be usable
     * but should be replaced with fresh data as soon as it can be loaded.
     * Only meant to bridge the second or two that takes, as airportData can take a bit to load.
     */
    fun getStaleOrEmptyAirportDataCache(): AirportDataCache

    suspend fun getAirportByIcaoIdentOrNull(ident: String): Airport?

    /**
     * Set the progress of an ongoing Airport Sync operation for [getAirportSyncProgressFlow] to emit
     */
    fun setAirportSyncProgress(progress: Int)

    /**
     * Get a flow that emits when [setAirportSyncProgress] has set progress
     */
    fun getAirportSyncProgressFlow(): Flow<Int>

    /**
     * Replace current airport database with [newAirports]
     */
    suspend fun replaceDbWith(newAirports: Collection<Airport>)



    companion object {
        private var INSTANCE: AirportRepository? = null
        fun getInstance() =
            INSTANCE ?: AirportRepositoryImpl(JoozdlogDatabase.getInstance()).also { INSTANCE = it }

        fun mock(mockDatabase: JoozdlogDatabase) = AirportRepositoryImpl(mockDatabase)
    }
}