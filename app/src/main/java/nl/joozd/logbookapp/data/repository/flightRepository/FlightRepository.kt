/*
 *  JoozdLog Pilot's Logbook
 *  Copyright (c) 2020 Joost Welle
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

import android.Manifest
import android.util.Log
import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.Transformations
import androidx.lifecycle.Transformations.distinctUntilChanged
import kotlinx.coroutines.*
import nl.joozd.logbookapp.model.dataclasses.Flight
import nl.joozd.logbookapp.data.room.JoozdlogDatabase
import nl.joozd.logbookapp.data.room.dao.FlightDao
import nl.joozd.logbookapp.data.room.model.toFlight
import nl.joozd.logbookapp.data.utils.FlightsListFunctions.makeListOfNamesAsync
import nl.joozd.logbookapp.App
import nl.joozd.logbookapp.data.calendar.CalendarFlightUpdater
import nl.joozd.logbookapp.data.comm.Cloud
import nl.joozd.logbookapp.data.sharedPrefs.Preferences
import nl.joozd.logbookapp.utils.TimestampMaker
import nl.joozd.logbookapp.utils.checkPermission
import nl.joozd.logbookapp.workmanager.JoozdlogWorkersHub
import java.time.Instant

//TODO reorder this and make direct DB functions private
//TODO make cloud functions originate from here and do their DB work here, not in [Cloud]
class FlightRepository(private val flightDao: FlightDao, private val dispatcher: CoroutineDispatcher = Dispatchers.IO
): CoroutineScope by MainScope() {
    private var undeleteFlight: Flight? = null
    private var undeleteFlights: List<Flight>? = null

    /********************************************************************************************
     * collection of all valid flights:
     ********************************************************************************************/

    private val _cachedFlights = MutableLiveData<List<Flight>>()
    init {
        // May be some delay in filling stuff listening to this
        launch {
            _cachedFlights.value = withContext(dispatcher){ flightDao.requestValidFlights().map {it.toFlight() }}
        }
        requestValidLiveFlightData().observeForever { _cachedFlights.value = it }
    }
    private var cachedFlightsList: List<Flight>?
        get() = _cachedFlights.value
        set(fff){
            _cachedFlights.value = fff
        }

    val liveFlights: LiveData<List<Flight>> = distinctUntilChanged(_cachedFlights)

    //Might become private, depending on how i will do cloud syncs
    suspend fun requestWholeDB(): List<Flight> = withContext(dispatcher){
        flightDao.requestAllFlights().map{it.toFlight()}
    }

    suspend fun fetchFlightByID(id: Int): Flight? {
        cachedFlightsList?.let {
            return it.firstOrNull {f -> f.flightID == id }
        }
        return withContext(dispatcher){ flightDao.fetchFlightByID(id)?.toFlight()}
    }

    /**
     * All names in those flights
     */
    private val _allNames = MutableLiveData<List<String>>()
    val allNames: LiveData<List<String>>
        get() = _allNames
    init{
        liveFlights.observeForever {
            launch{
                _allNames.value = makeListOfNamesAsync(it)
            }
        }
    }

    /**********************************************************************************************
     * Delete functions. Use delete(flight) or delete(flightsList)
     * Function will decide whether it can be just locally deleted or softdeleted for sync purposes
     **********************************************************************************************/

    /**
     * Delete flight from disk if not known to server, else set DELETEFLAG to 1 and update timestamp
     * soft-delete will update cache through Dao observer
     */
    fun delete(flight: Flight) {
        undeleteFlight = flight
        Log.d(this::class.simpleName, "Deleting flight $flight")
        if (flight.unknownToServer) deleteFlightHard(flight).also{ Log.d(this::class.simpleName, "hard-deleted flight $flight")}
        else save(
            flight.copy(
                DELETEFLAG = true,
                timeStamp = TimestampMaker.nowForSycPurposes
            )
        ).also{ Log.d(this::class.simpleName, "soft-deleted flight $flight")}
    }


    fun delete(id: Int) {
        launch(NonCancellable){
            fetchFlightByID(id)?.let { delete (it)} ?: Log.w("FlightRepository", "delete(id: Int): No flight found with id $id")
        }
    }

    /**
     * Same as deleteFlight, but with a list of multiple Flights
     */
    fun delete(flights: List<Flight>, sync: Boolean = true) {
        undeleteFlights = flights
        deleteHard(flights.filter { it.unknownToServer })
        save(flights.filter { !it.unknownToServer }.map { f ->
            f.copy(
                DELETEFLAG = true,
                timeStamp = TimestampMaker.nowForSycPurposes
            )
        }, sync)
    }

    fun undeleteFlight() = undeleteFlight?.let {save(it) }
    fun undeleteFlights() = undeleteFlights?.let { save(it) }

    /**
     * Empty whole database
     */
    fun clearDB() = launch (dispatcher + NonCancellable){
        launch(Dispatchers.Main) { cachedFlightsList = emptyList() }
        flightDao.clearDb()
    }

    /********************************************************************************************
     * Utility functions:
     ********************************************************************************************/

    fun getMostRecentFlightAsync() =
        async(dispatcher){
            cachedFlightsList?.let {
                it.filter {f -> !f.isPlanned && !f.isSim }.maxBy {f -> f.timeOut }
            } ?: flightDao.getMostRecentCompleted()?.toFlight()
        }

    fun getHighestIdAsync() = async(dispatcher) { flightDao.highestId() ?: 0 }

    fun iAmACaptainAsync() =  async(dispatcher) {
        getMostRecentFlightAsync().await()?.isPIC == true
    }



    /********************************************************************************************
     * Private functions:
     ********************************************************************************************/

    //gets all flights that are not marked DELETED
    suspend fun getAllFlights(): List<Flight> = cachedFlightsList ?: withContext(dispatcher) {
        flightDao.requestValidFlights().map{it.toFlight()}
    }

    private fun requestValidLiveFlightData() =
        Transformations.map(flightDao.requestNonDeletedLiveData()) { fff ->
            fff.map { f -> f.toFlight() }
        }

    /**
     * update cached data and save to disk
     */
    fun save(flight: Flight, sync: Boolean = true) {
        //update cached flights
        cachedFlightsList = ((cachedFlightsList?: emptyList()).filter { it.flightID != flight.flightID }
                + listOf(flight).filter { !it.DELETEFLAG })
            .sortedByDescending { it.timeOut }
        //Save flight to disk
        launch (dispatcher + NonCancellable) {
            flightDao.insertFlights(flight.toModel())
            if (sync) syncAfterChange()
        }
    }

    /**
     * update cached data and save to disk
     */
    fun save(flights: List<Flight>, sync: Boolean = true) {
        //update cached flights
        cachedFlightsList = ((cachedFlightsList ?: emptyList()).filter { it.flightID !in flights.map {it2 -> it2.flightID } }
                + flights.filter { !it.DELETEFLAG })
            .sortedByDescending { it.timeOut }
        //Save flights to disk
        launch (dispatcher + NonCancellable) {
            flightDao.insertFlights(*(flights.map { it.toModel() }.toTypedArray()))
            if (sync) syncAfterChange()
        }
    }

    /**
     * SaveFromRoster will remove flights on days that new planned flights are added (ignoring those that are the same)
     * and fixed flightIDs for new flights
     */
    suspend fun saveFromRoster(rosteredFlights: List<Flight>, period: ClosedRange<Instant>? = null, sync: Boolean = true) = withContext(dispatcher) {
        val highestID = getHighestIdAsync() // async, start looking for that while doing other stuff
        val sameFlights = getFlightsMatchingPlannedFlights(getAllFlights(), rosteredFlights)
        val flightsToRemove = getFlightsOnDays(getAllFlights(), dateRange = period, flightsOnDays = rosteredFlights).filter {it.isPlanned && it !in sameFlights && it.timeOut > Instant.now().epochSecond}
        val flightsToSave = getNonMatchingPlannedFlights(getAllFlights(), rosteredFlights)
        withContext(Dispatchers.Main){
            delete(flightsToRemove)
            val lowestNewID = highestID.await() + 1
            save(flightsToSave.mapIndexed { index: Int, f: Flight ->
                f.copy(flightID = lowestNewID + index)
            }, sync)
        }
    }
    /**
     * update cached data and delete from disk
     */
    private fun deleteFlightHard(flight: Flight) {
        cachedFlightsList = ((cachedFlightsList
            ?: emptyList()).filter { it.flightID != flight.flightID }).sortedByDescending { it.timeOut }
        launch (dispatcher + NonCancellable) {
            flightDao.delete(flight.toModel())
        }
    }

    /**
     * update cached data and delete multiple flights from disk
     */
    private fun deleteHard(flights: List<Flight>) {
        cachedFlightsList = ((cachedFlightsList
            ?: emptyList()).filter { it.flightID !in flights.map { f -> f.flightID } }).sortedByDescending { it.timeOut }
        launch (dispatcher + NonCancellable) {
            flightDao.deleteMultipleByID(flights.map { it.flightID })
        }
    }

    /********************************************************************************************
     * Sync functions:
     ********************************************************************************************/

    /**
     * This does two things:
     * first, it updates
     */
    fun syncIfNeeded(){
        launch {
            val needsServerSync =
                TimestampMaker.nowForSycPurposes - Preferences.lastUpdateTime > MIN_SYNC_INTERVAL
            val needsCalendarSync =
                TimestampMaker.nowForSycPurposes - Preferences.lastCalendarCheckTime > MIN_CALENDAR_CHECK_INTERVAL
            if ((needsCalendarSync || needsServerSync) && Preferences.getFlightsFromCalendar) {
                if (checkPermission(Manifest.permission.READ_CALENDAR)) {
                    val calendar = CalendarFlightUpdater()
                    calendar.getFlights()?.let {

                        saveFromRoster(it, calendar.period)
                    }
                }
            }
            if (needsServerSync)
                JoozdlogWorkersHub.synchronizeFlights(delay = false)
        }
    }

    /**
     * This will schedule a sync after a few minutes (to be used when a flight has been changed/saved
     */
    private fun syncAfterChange(){
        if (!Cloud.syncingFlights)
            JoozdlogWorkersHub.synchronizeFlights()
    }

    private val _syncProgress = MutableLiveData<Int>(-1)
    val syncProgress: LiveData<Int>
        get() = _syncProgress
    fun setSyncProgress(progress: Int){
        require (progress in (-1..100)) {"Progress reported to setAirportSyncProgress not in range -1..100"}
        launch (Dispatchers.Main) {
            _syncProgress.value = progress
        }
    }

    /********************************************************************************************
     * Companion object:
     ********************************************************************************************/

    companion object{
        private var singletonInstance: FlightRepository? = null
        fun getInstance(): FlightRepository = synchronized(this) {
            singletonInstance
                ?: run {
                    val dataBase = JoozdlogDatabase.getDatabase(App.instance)
                    val flightsDao = dataBase.flightDao()
                    singletonInstance =
                        FlightRepository(
                            flightsDao
                        )
                    singletonInstance!!
                }
        }

        const val MIN_SYNC_INTERVAL = 30*60 // seconds
        const val MIN_CALENDAR_CHECK_INTERVAL = 30 // seconds
    }



    //TODO remove when ready
    suspend fun updateNamesDivider(): Boolean{
        val allFlights = withContext(dispatcher) {getAllFlights() }.map{it.copy(name2 = it.name2.split(",").joinToString("|"))}
        save(allFlights.map{it.copy(timeStamp = TimestampMaker.nowForSycPurposes)})
        return true
    }
}