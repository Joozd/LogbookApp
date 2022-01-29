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


import kotlinx.coroutines.*
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.update
import nl.joozd.logbookapp.data.dataclasses.Airport
import nl.joozd.logbookapp.data.room.JoozdlogDatabase

class AirportRepositoryImpl(
    dataBase: JoozdlogDatabase
): AirportRepository, CoroutineScope by MainScope()  {
    private val airportDao = dataBase.airportDao()
    // used for getStaleOrEmptyAirportDataCache
    private var mostRecentlyLoadedAirportDataCache: AirportDataCache = AirportDataCache.make(emptyList())

    // a progress of -1 means no airport sync in progress.
    private val airportSyncProgressMutableStateFlow = MutableStateFlow(-1)

    override fun airportsFlow() = airportDao.airportsFlow()

    override suspend fun getAirportDataCache(): AirportDataCache =
        makeAndStoreAirportDataCache(getAirports())

    override fun airportDataCacheFlow(): Flow<AirportDataCache> =
        airportsFlow().map { makeAndStoreAirportDataCache(it) }

    override suspend fun getAirportByIcaoIdentOrNull(ident: String): Airport? =
        airportDao.searchAirportByIdent(ident)

    override fun setAirportSyncProgress(progress: Int) {
        airportSyncProgressMutableStateFlow.update { progress }
    }

    override fun getAirportSyncProgressFlow(): Flow<Int> = airportSyncProgressMutableStateFlow

    /**
     * Replace current airport database with [newAirports]
     */
    override suspend fun replaceDbWith(newAirports: Collection<Airport>) {
        airportDao.clearDb()
        airportDao.save(newAirports)
    }

    private suspend fun getAirports(): List<Airport> = airportDao.requestAllAirports()

    private fun makeAndStoreAirportDataCache(it: List<Airport>) =
        AirportDataCache.make(it).also{
            mostRecentlyLoadedAirportDataCache = it
        }
}

