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

package nl.joozd.logbookapp.data.repository.flightRepository


import kotlinx.coroutines.*
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import nl.joozd.logbookapp.core.background.SyncCenter
import nl.joozd.logbookapp.data.dataclasses.FlightData
import nl.joozd.logbookapp.model.dataclasses.Flight
import nl.joozd.logbookapp.data.room.JoozdlogDatabase
import nl.joozd.logbookapp.data.room.model.toFlight
import nl.joozd.logbookapp.utils.DispatcherProvider
import nl.joozd.logbookapp.utils.TimestampMaker
import nl.joozd.logbookapp.utils.delegates.dispatchersProviderMainScope


class FlightRepositoryImpl(
    injectedDatabase: JoozdlogDatabase?
) : FlightRepositoryWithDirectAccess, FlightRepositoryWithSpecializedFunctions, CoroutineScope by dispatchersProviderMainScope() {
    private constructor(): this (null)

    private val database = injectedDatabase ?: JoozdlogDatabase.getInstance() // this way we can detect if a DB is injected
    private val flightDao = database.flightDao()
    private val idGenerator = IDGenerator()

    override suspend fun getFlightByID(flightID: Int): Flight? =
        flightDao.getFlightById(flightID)?.toFlight()

    override suspend fun getFlightsByID(ids: Collection<Int>): List<Flight> =
        ids.chunked(999).map { i -> // otherwise room gets angry
            flightDao.getFlightsByID(i).map { it.toFlight() }
        }.flatten()

    override suspend fun getFlightsStartingInEpochSecondRange(range: ClosedRange<Long>) {
        flightDao.getFlightsStartingBetween(range.start, range.endInclusive).map{ it.toFlight() }
    }

    override suspend fun getAllFlightsInDB(): List<Flight> =
        flightDao.getAllFlights().map { it.toFlight() }

    override fun allFlightsFlow(): Flow<List<Flight>> =
        flightDao.validFlightsFlow().map { it.toFlights() }

    override suspend fun getAllFlights(): List<Flight> =
        getValidFlightsFromDao()

    override suspend fun getFLightDataCache(): FlightDataCache =
        FlightDataCache.make(getValidFlightsFromDao())

    override fun flightDataCacheFlow(): Flow<FlightDataCache> =
        flightDao.validFlightsFlow().map {
            FlightDataCache.make(it.toFlights())
        }

    override suspend fun save(flight: Flight) {
        save(listOf(flight))
    }

    override suspend fun save(flights: Collection<Flight>) {
        saveWithIDAndTimestamp(flights)
    }

    override suspend fun saveDirectToDB(flight: Flight) {
        saveDirectToDB(listOf(flight))
    }

    override suspend fun saveDirectToDB(flights: Collection<Flight>) {
        //If size too big, it will chunk and retry.
        if (flights.size > MAX_SQL_BATCH_SIZE)
            flights.chunked(MAX_SQL_BATCH_SIZE).forEach { withContext(DispatcherProvider.io()) { saveDirectToDB(it) } }

        else withContext(DispatcherProvider.io()) {
            withContext(DispatcherProvider.io()) { flightDao.save(flights.map { it.toData() }) }
        }
    }

    override suspend fun delete(flight: Flight) {
        delete(listOf(flight))
    }

    override suspend fun delete(flights: Collection<Flight>) {
        val flightsToDeleteHard = flights.filter { it.unknownToServer }
        val flightsToDeleteSoft = flights.filter { !it.unknownToServer }
        deleteHard(flightsToDeleteHard)
        deleteSoft(flightsToDeleteSoft)
    }

    override suspend fun generateAndReserveNewFlightID(highestTakenID: Int): Int =
        idGenerator.generateID(highestTakenID)

    override suspend fun deleteHard(flight: Flight) {
        deleteHard(listOf(flight))
    }

    override suspend fun deleteHard(flights: Collection<Flight>) {
        //If size too big, it will chunk and retry.
        if (flights.size > MAX_SQL_BATCH_SIZE)
            flights.chunked(MAX_SQL_BATCH_SIZE).forEach { deleteHard(it)}

        else withContext(DispatcherProvider.io()) {
            flightDao.delete(flights.map { it.toData() })
        }
    }

    private suspend fun deleteSoft(flights: Collection<Flight>) = withContext(DispatcherProvider.io()){
        val softDeletedFlights = flights.map {it.copy(DELETEFLAG = true) }
        saveWithIDAndTimestamp(softDeletedFlights)
    }

    override suspend fun getMostRecentTimestampOfACompletedFlight(): Long? =
        withContext(DispatcherProvider.io()) {
            flightDao.getMostRecentTimestampOfACompletedFlight()
        }

    override suspend fun getMostRecentCompletedFlight(): Flight? =
        withContext(DispatcherProvider.io()){
            flightDao.getMostRecentCompleted()?.toFlight()
    }


    private fun List<FlightData>.toFlights() =
        this.map { it.toFlight() }

    /*
     * - Adds timestamp to flight. Only do this when something actually changes for this flight (e.g. it is deleted)
     * - if flightID is set to Flight.NOT_INITIALIZED it will generate a new ID and set it.
     * - This will also set TaskFlags.syncFlights as saving something with an updated timestamp will always warrant a sync.
     */
    private suspend fun saveWithIDAndTimestamp(flights: Collection<Flight>){
        saveDirectToDB(
            flights.updateIDsIfNeeded().updateTimestampsToNow()
        )
        SyncCenter().syncFlights()
    }

    private fun Collection<Flight>.updateTimestampsToNow(): List<Flight> {
        val now = TimestampMaker().nowForSycPurposes
        return map {
            it.copy(timeStamp = now)
        }
    }

    private suspend fun Collection<Flight>.updateIDsIfNeeded(): List<Flight> {
        //make sure generated fLightIDs are incremented far enough
        forceLowestFreeIdToBeHigherThanHighestIdIn(this)

        return map {
            it.copy(flightID = makeNewIDIfCurrentNotInitialized(it))
        }
    }

    private fun forceLowestFreeIdToBeHigherThanHighestIdIn(flights: Collection<Flight>) {
        flights.maxOfOrNull { it.flightID }?.let { idGenerator.setMostRecentHighestIdToAtLeast(it) }
    }

    private suspend fun makeNewIDIfCurrentNotInitialized(flight: Flight): Int =
        (if (flight.flightID == Flight.FLIGHT_ID_NOT_INITIALIZED)
            idGenerator.generateID(0)
        else flight.flightID)

    private suspend fun getValidFlightsFromDao() = withContext(Dispatchers.IO) {
        flightDao.getValidFlights().toFlights()
    }

    /**
     * Generate unique IDs.
     */
    private inner class IDGenerator{
        private var mostRecentHighestID: Int = Flight.FLIGHT_ID_NOT_INITIALIZED

        private val mutex = Mutex()

        suspend fun generateID(highestTakenID: Int): Int{
            mutex.withLock {
                if (mostRecentHighestID == Flight.FLIGHT_ID_NOT_INITIALIZED)
                    mostRecentHighestID = flightDao.highestUsedID() ?: 0
                mostRecentHighestID = maxOf(mostRecentHighestID, highestTakenID)
                return (++mostRecentHighestID)
            }
        }

        fun setMostRecentHighestIdToAtLeast(minimumHigestID: Int){
            mostRecentHighestID = maxOf(mostRecentHighestID, minimumHigestID)
        }
    }

    companion object{
        const val MAX_SQL_BATCH_SIZE = 999
        val instance by lazy { FlightRepositoryImpl() }
    }
}