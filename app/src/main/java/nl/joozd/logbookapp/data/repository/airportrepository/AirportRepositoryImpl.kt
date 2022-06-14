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
import kotlinx.coroutines.flow.*
import nl.joozd.logbookapp.data.dataclasses.Airport
import nl.joozd.logbookapp.data.repository.Preloader
import nl.joozd.logbookapp.data.room.JoozdlogDatabase
import nl.joozd.logbookapp.utils.DispatcherProvider
import nl.joozd.logbookapp.utils.delegates.dispatchersProviderMainScope

class AirportRepositoryImpl(
    dataBase: JoozdlogDatabase
): AirportRepository, CoroutineScope by dispatchersProviderMainScope()  {
    private val airportDao = dataBase.airportDao()

    override val hasData: StateFlow<Boolean> = MutableStateFlow(false)
    init{
        MainScope().launch {
            if (airportDao.airportsFlow().firstOrNull() == null)
                updateAirports(Preloader().getPreloadedAirports())
            (hasData as MutableStateFlow).value = true
        }
    }


    override fun airportsFlow() = airportDao.airportsFlow()

    override suspend fun getAirportDataCache(): AirportDataCache =
        makeAndStoreAirportDataCache(getAirports())

    override fun airportDataCacheFlow(): Flow<AirportDataCache> =
        airportsFlow().map { makeAndStoreAirportDataCache(it) }

    override suspend fun getAirportByIcaoIdentOrNull(ident: String): Airport? =
        withContext(DispatcherProvider.io()) {
            airportDao.searchAirportByIdent(ident)
        }

    override suspend fun getAirportByIataIdentOrNull(ident: String): Airport? =
        withContext(DispatcherProvider.io()) {
            airportDao.searchAirportByIata(ident)
        }

    /**
     * Replace current airport database with [newAirports]
     */
    override suspend fun updateAirports(newAirports: Collection<Airport>) = withContext(DispatcherProvider.io()) {
        airportDao.clearDb()
        airportDao.save(newAirports)
    }

    private suspend fun getAirports(): List<Airport> = withContext(DispatcherProvider.io()) {
        airportDao.requestAllAirports()
    }

    private fun makeAndStoreAirportDataCache(it: List<Airport>) =
        AirportDataCache.make(it)
}

