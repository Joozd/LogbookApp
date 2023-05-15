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
import nl.joozd.joozdlogcommon.utilities.checkForDuplicates
import nl.joozd.logbookapp.data.dataclasses.FlightData
import nl.joozd.logbookapp.model.dataclasses.Flight
import nl.joozd.logbookapp.data.room.JoozdlogDatabase
import nl.joozd.logbookapp.data.room.model.toFlight
import nl.joozd.logbookapp.utils.DispatcherProvider
import nl.joozd.logbookapp.utils.delegates.dispatchersProviderMainScope


class FlightRepositoryImpl(
    injectedDatabase: JoozdlogDatabase?
) : FlightRepositoryWithSpecializedFunctions, CoroutineScope by dispatchersProviderMainScope() {
    private constructor(): this (null)

    private val database = injectedDatabase ?: JoozdlogDatabase.getInstance() // this way we can detect if a DB is injected
    private val flightDao = database.flightDao()
    private val idGenerator = IDGenerator()

    private val registeredDataChangedListeners = HashSet<FlightRepository.OnDataChangedListener>()

    private val noParallelAccessMutex = Mutex()

    override suspend fun getFlightByID(flightID: Int): Flight? =
        flightDao.getFlightById(flightID)?.toFlight()

    override suspend fun getFlightsByID(ids: Collection<Int>): List<Flight> =
        ids.chunked(999).map { i -> // otherwise room gets angry
            flightDao.getFlightsByID(i).map { it.toFlight() }
        }.flatten()

    override suspend fun getFlightsStartingInEpochSecondRange(range: ClosedRange<Long>) {
        flightDao.getFlightsStartingBetween(range.start, range.endInclusive).map{ it.toFlight() }
    }

    override fun allFlightsFlow(): Flow<List<Flight>> =
        flightDao.validFlightsFlow().map { it.toFlights() }

    override suspend fun getAllFlights(): List<Flight> =
        getValidFlightsFromDao()

    override fun flightDataCacheFlow(): Flow<FlightDataCache> =
        flightDao.validFlightsFlow().map {
            FlightDataCache.make(it.toFlights())
        }

    override suspend fun save(flight: Flight) {
        save(listOf(flight))
    }

    override suspend fun save(flights: Collection<Flight>) {
        noParallelAccessMutex.withLock {
            saveWithIDAndTimestamp(flights)
            registeredDataChangedListeners.forEach { listener ->
                listener.onFlightRepositoryChanged(flights.map { it.flightID })
            }
        }
    }

    override suspend fun delete(flight: Flight) {
        delete(listOf(flight))
    }

    override suspend fun delete(flights: Collection<Flight>) {
        noParallelAccessMutex.withLock {
            flights.chunked(MAX_SQL_BATCH_SIZE).forEach { flights -> flightDao.delete(flights.map { it.toData() }) }
        }
    }

    override suspend fun deleteById(ids: Collection<Int>) {
        ids.chunked(MAX_SQL_BATCH_SIZE).forEach { chunkedIDs ->flightDao.deleteById(chunkedIDs) }
    }

    override suspend fun deleteById(id: Int) = deleteById(listOf(id))

    override suspend fun generateAndReserveNewFlightID(highestTakenID: Int): Int =
        idGenerator.generateID(highestTakenID)


    override suspend fun clear() {
        noParallelAccessMutex.withLock {
            flightDao.clear()
        }
    }

    override suspend fun getMostRecentCompletedFlight(): Flight? =
        withContext(DispatcherProvider.io()){
            flightDao.getMostRecentCompleted()?.toFlight()
    }

    override suspend fun removeDuplicates(): Int {
        val ff = getAllFlights().map { it.toBasicFlight() }
        val duplicates = withContext (DispatcherProvider.default()) { checkForDuplicates(ff) }
        delete(duplicates.map { Flight(it) })
        return duplicates.size
    }

    private fun List<FlightData>.toFlights() =
        this.map { it.toFlight() }

    /*
     * - Adds timestamp to flight. Only do this when something actually changes for this flight (e.g. it is deleted)
     * - if flightID is set to Flight.NOT_INITIALIZED it will generate a new ID and set it.
     * - This will also set TaskFlags.syncFlights as saving something with an updated timestamp will always warrant a sync.
     */
    private suspend fun saveWithIDAndTimestamp(flights: Collection<Flight>) =
        flights.chunked(MAX_SQL_BATCH_SIZE).forEach { chunkOfFlights ->
            withContext(DispatcherProvider.io()) {
                flightDao.save(chunkOfFlights.updateIDsIfNeeded().map {
                    it.toData()
                })
            }
        }


    private suspend fun Collection<Flight>.updateIDsIfNeeded(): List<Flight> {
        //make sure generated fLightIDs are incremented far enough
        forceLowestFreeIdToBeHigherThanHighestIdIn(this)

        return map {
            it.copy(flightID = makeNewIDIfCurrentNotInitialized(it))
        }
    }

    private suspend fun forceLowestFreeIdToBeHigherThanHighestIdIn(flights: Collection<Flight>) {
        flights.maxOfOrNull { it.flightID }?.let { idGenerator.setMostRecentHighestIdToAtLeast(it + 1) }
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
                checkLowestFreeID()
                mostRecentHighestID = maxOf(mostRecentHighestID, highestTakenID)
                return (++mostRecentHighestID)
            }
        }

        suspend fun setMostRecentHighestIdToAtLeast(minimumHighestID: Int){
            checkLowestFreeID()
            mostRecentHighestID = maxOf(mostRecentHighestID, minimumHighestID)
        }

        private suspend fun checkLowestFreeID() {
            if (mostRecentHighestID == Flight.FLIGHT_ID_NOT_INITIALIZED)
                mostRecentHighestID = flightDao.highestUsedID() ?: 0
        }
    }

    companion object{
        const val MAX_SQL_BATCH_SIZE = 999
        val instance by lazy { FlightRepositoryImpl() }
    }
}