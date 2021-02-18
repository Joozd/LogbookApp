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
import android.os.Looper
import android.util.Log
import androidx.lifecycle.*
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
import nl.joozd.logbookapp.data.parseSharedFiles.extensions.postProcess
import nl.joozd.logbookapp.data.parseSharedFiles.interfaces.Roster
import nl.joozd.logbookapp.data.repository.helpers.isSameFlightAs
import nl.joozd.logbookapp.data.repository.helpers.isSameFlightAsWithMargins
import nl.joozd.logbookapp.data.repository.helpers.isSameFlightOnSameDay
import nl.joozd.logbookapp.data.sharedPrefs.Preferences
import nl.joozd.logbookapp.data.utils.FlightsListFunctions.makeListOfRegistrations
import nl.joozd.logbookapp.model.workingFlight.WorkingFlight
import nl.joozd.logbookapp.utils.TimestampMaker
import nl.joozd.logbookapp.utils.checkPermission
import nl.joozd.logbookapp.workmanager.JoozdlogWorkersHub
import java.time.Instant
import java.util.*

//TODO reorder this and make direct DB functions private
//TODO make cloud functions originate from here and do their DB work here, not in [Cloud]
class FlightRepository(private val flightDao: FlightDao, private val dispatcher: CoroutineDispatcher = Dispatchers.IO
): CoroutineScope by MainScope() {
    /********************************************************************************************
     * Private parts:
     ********************************************************************************************/



    private var undeleteFlight: Flight? = null
    private var undeleteFlights: List<Flight>? = null

    private val _savedFlight = MutableLiveData<SingleUseFlight>()

    private val _workingFlight = MutableLiveData<WorkingFlight?>()

    private val _cachedFlights = MediatorLiveData<List<Flight>>().apply{
        // Fill it before first observer arrives so it is cached right away
        launch { // set value on main thread
            value = withContext(dispatcher) { flightDao.requestValidFlights().map { it.toFlight() } }
            addSource(requestValidLiveFlightData()) { value = it }
        }
    }
    /**
     * All names in flights
     */
    private val _allNames = MediatorLiveData<List<String>>().apply{
        addSource(_cachedFlights) {
            launch {
                value = makeListOfNamesAsync(it)
            }
        }
    }
    /**
     * All registrations in flights
     */
    private val _usedRegistrations= MediatorLiveData<List<String>>().apply{
        addSource(_cachedFlights){
            value = makeListOfRegistrations(it)
        }
    }

    /**
     * NOTE: Set this on main thread. Using postValue can cause race conditions
     */
    private var cachedFlightsList: List<Flight>?
        get() = _cachedFlights.value
        set(fff){
            _cachedFlights.value = fff
        }

    private fun requestValidLiveFlightData() =
        flightDao.requestNonDeletedLiveData().map { fff ->
            fff.map { f -> f.toFlight() }
        }


    /**
     * update cached data and delete from disk
     */
    private fun deleteFlightHard(flight: Flight) = launch {
        cachedFlightsList = ((cachedFlightsList
            ?: emptyList()).filter { it.flightID != flight.flightID }).sortedByDescending { it.timeOut }
        launch (dispatcher + NonCancellable) {
            flightDao.delete(flight.toModel())
        }
    }

    /**
     * update cached data and delete multiple flights from disk
     */
    private fun deleteHard(flights: List<Flight>) = launch {
        cachedFlightsList = ((cachedFlightsList
            ?: emptyList()).filter { it.flightID !in flights.map { f -> f.flightID } }).sortedByDescending { it.timeOut }
        launch (dispatcher + NonCancellable) {
            flightDao.deleteMultipleByID(flights.map { it.flightID })
        }
    }

    /**
     * This will schedule a sync after a few minutes (to be used when a flight has been changed/saved
     */
    private fun syncAfterChange(){
        if (!Cloud.syncingFlights)
            JoozdlogWorkersHub.synchronizeFlights(delay = true)
    }

    /**
     * Returns all flights in DB which start in [period]
     */
    private suspend fun getFlightsOnDays(period: ClosedRange<Instant>) = getFlightsOnDays(getAllFlights(), period)

    private val _syncProgress = MutableLiveData<Int>(-1)

    private val _notLoggedIn = MutableLiveData<Boolean>()

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

    /********************************************************************************************
     * Public parts:
     ********************************************************************************************/

    /********************************************************************************************
     * Observables:
     ********************************************************************************************/

    /**
     * Flight to be edited in EditFlightFragmnt + dialogs
     */
    val workingFlight: LiveData<WorkingFlight?>
        get() = _workingFlight
    val wf
        get() = workingFlight.value!! // shortcut.

    /**
     * saved flight set through [notifyFlightSaved]
     */
    val savedFlight: LiveData<SingleUseFlight>
        get() = _savedFlight

    //list of valid flights (not soft-deleted ones)
    val liveFlights: LiveData<List<Flight>> = distinctUntilChanged(_cachedFlights)

    val syncProgress: LiveData<Int>
        get() = _syncProgress

    val notLoggedIn: LiveData<Boolean>
        get() = _notLoggedIn

    val allNames: LiveData<List<String>>
        get() = _allNames

    val usedRegistrationsLiveData: LiveData<List<String>>
        get() = _usedRegistrations

    /********************************************************************************************
     * Vars and vals
     ********************************************************************************************/

    /**
     * All registrations used in all flights
     */
    val usedRegistrations: List<String>
        get() = usedRegistrationsLiveData.value ?: makeListOfRegistrations(cachedFlightsList?: emptyList()) //  cachedFlightsList must be filled before accessing this

    /********************************************************************************************
     * Public functions
     ********************************************************************************************/

    fun setWorkingFlight(f: WorkingFlight?){
        if (Looper.myLooper() != Looper.getMainLooper()) launch(Dispatchers.Main){
            Log.w("setWorkingFlight", "setWorkingFlight called on a background thread, setting workingFlight async on main")
            _workingFlight.value = f
        }
        else _workingFlight.value = f
    }


    /**
     * Delete functions. Use delete(flight) or delete(flightsList)
     * Function will decide whether it can be just locally deleted or softdeleted for sync purposes
     */

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


    /**
     * Same but find it first by looking up it's ID
     */
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
    fun clearDB() = launch {
        cachedFlightsList = emptyList()
        launch(dispatcher + NonCancellable) {
            flightDao.clearDb()
        }
    }


    /**
     * Save functions. Can be used on main thread, function will take care of background thingies
     */

    /**
     * put a flight in [undoSaveFlight] so you can undo saving a flight or check any changes made
     */
    var undoSaveFlight: Flight? = null

    /**
     * Close working flight (set it's liveData to null)
     */
    fun closeWorkingFlight(){
        _workingFlight.value = null
    }

    /**
     * update cached data and save to disk
     * This will update Flight.timeStamp
     * @param flight: Flight to save
     * @param sync: Whether or not to sync to server after saving
     * @param notify: Whether or not to update [savedFlight]
     */
    fun save(flight: Flight, sync: Boolean = true, notify: Boolean = false, updateIDs: Boolean = false) = launch {
        //assign available FlightID if requested
        val f = if (updateIDs) flight.copy(flightID = lowestFreeFlightID(), timeStamp = TimestampMaker.nowForSycPurposes) else flight.copy (timeStamp = TimestampMaker.nowForSycPurposes)

        //update cached flights
        cachedFlightsList = ((cachedFlightsList?: emptyList()).filter { it.flightID != f.flightID }
                + listOf(f).filter { !it.DELETEFLAG })
            .sortedByDescending { it.timeOut }
        //Save flight to disk
        launch (dispatcher + NonCancellable) {
            flightDao.insertFlights(f.toModel())
            if (notify) launch(Dispatchers.Main) {
                _savedFlight.value = SingleUseFlight(f).also{
                    Log.d("FLightReposiroty.save()","Notifying!")
                } }
            if (sync) syncAfterChange()
        }
    }

    /**
     * update cached data and save to disk
     * @see [save]
     */
    fun save(flights: List<Flight>, sync: Boolean = true, updateIDs: Boolean = false) = launch {
        //assign available FlightIDs if requested
        val ff = if (updateIDs) flights.mapIndexed { index, flight -> flight.copy(flightID = lowestFreeFlightID() + index, timeStamp = TimestampMaker.nowForSycPurposes) } else flights.map { it.copy (timeStamp = TimestampMaker.nowForSycPurposes) }

        //update cached flights
        cachedFlightsList = ((cachedFlightsList ?: emptyList()).filter { it.flightID !in ff.map { it2 -> it2.flightID } }
                + ff.filter { !it.DELETEFLAG })
            .sortedByDescending { it.timeOut }
        //Save flights to disk
        launch (dispatcher + NonCancellable) {
            flightDao.insertFlights(*(ff.map { it.toModel() }.toTypedArray()))
            Log.d("Saved", "Saved ${ff.size} flights!")
            if (sync) syncAfterChange()
        }
    }

    /*
    /**
     * SaveFromRoster will remove flights on days that new planned flights are added (ignoring those that are the same)
     * and fixed flightIDs for new flights
     */
    @Deprecated("use [saveRoster]")
    fun saveFromRoster(rosteredFlights: List<Flight>, period: ClosedRange<Instant>? = null, sync: Boolean = true) = launch(NonCancellable) {
        Log.d("saveFromRoster()", "rosteredFlights: $rosteredFlights")
        val highestID =
            getHighestIdAsync() // async, start looking for that while doing other stuff
        val sameFlights = getFlightsMatchingPlannedFlights(getAllFlights(), rosteredFlights)
        Log.d("saveFromRoster()", "sameFlights: $sameFlights")
        val flightsToRemove =
            getFlightsOnDays(getAllFlights(), dateRange = period, flightsOnDays = rosteredFlights)
                .filter { it.isPlanned && it !in sameFlights && it.timeOut > Instant.now().epochSecond }
        Log.d("saveFromRoster()", "flightsToRemove: $flightsToRemove")
        val flightsToSave = getNonMatchingPlannedFlights(getAllFlights(), rosteredFlights)
        Log.d("saveFromRoster()", "flightsToSave: $flightsToSave")
        delete(flightsToRemove)
        val lowestNewID = highestID.await() + 1
        save(flightsToSave.mapIndexed { index: Int, f: Flight ->
            f.copy(flightID = lowestNewID + index, timeStamp = TimestampMaker.nowForSycPurposes)
        }, sync)
    }
    */

    /**
     * Save flights from a Roster
     * It will remove saved flights in [roster].period if they are not the same as any flights in [roster].flights
     * It will save flights in [roster].flights that are not the same as any that are already saved in DB.
     */
    suspend fun saveRoster(roster: Roster) = withContext(Dispatchers.IO + NonCancellable){
        require(roster.isValid) { "Cannot parse an invalid roster! You should have checked this!" }
        val rosterFlights = roster.flights
        val flightsInPeriod = getFlightsOnDays(roster.period)
        // Save all flights that are not also in DB
        val flightsToSave = rosterFlights.filter {rosteredFlight -> flightsInPeriod.none { it.isSameFlightAs(rosteredFlight) }}

        // delete all flights that are not also in roster and that are isPlanned
        val flightsToDelete = flightsInPeriod.filter { savedFlight -> rosterFlights.none { it.isSameFlightAs(savedFlight) } && savedFlight.isPlanned }

        delete(flightsToDelete, false) // sync will happen after saving
        save(flightsToSave, sync = true, updateIDs = true)
    }

    /**
     * Sync functions:
     */


    /**
     * This does two things:
     * first, it updates
     * TODO fix this documentation
     */
    fun syncIfNeeded(){
        launch {
            val needsServerSync =
                TimestampMaker.nowForSycPurposes - Preferences.lastUpdateTime > MIN_SYNC_INTERVAL
                        || getFlightsChangedAfter(Preferences.lastUpdateTime).isNotEmpty()
            val needsCalendarSync =
                TimestampMaker.nowForSycPurposes - Preferences.lastCalendarCheckTime > MIN_CALENDAR_CHECK_INTERVAL
            if ((needsCalendarSync || needsServerSync) && Preferences.getFlightsFromCalendar) {
                if (checkPermission(Manifest.permission.READ_CALENDAR)) {
                    val calendar = CalendarFlightUpdater()
                    calendar.getRoster()?.let {
                        saveRoster(it.postProcess())
                    }
                }
            }
            if (needsServerSync)
                JoozdlogWorkersHub.synchronizeFlights(delay = false)
        }
    }

    /**
     * To be called by worker class to set sync progress
     */
    fun setSyncProgress(progress: Int){
        require (progress in (-1..100)) {"Progress reported to setAirportSyncProgress not in range -1..100"}
        launch{
            _syncProgress.value = progress
        }
    }

    fun setNotLoggedInFlag(notLoggedIn: Boolean = false){
        launch {
            _notLoggedIn.value = notLoggedIn
        }
    }

    /********************************************************************************************
     * Public functions requiring async computing
     ********************************************************************************************/

    //gets all flights that are not marked DELETED
    suspend fun getAllFlights(): List<Flight> = cachedFlightsList ?: withContext(dispatcher) {
        flightDao.requestValidFlights().map{it.toFlight()}
    }

    /**
     * Get all flights with a timestamp higher than or equal to [timeStamp]
     */
    suspend fun getFlightsChangedAfter(timeStamp: Long) =
        flightDao.getFLightsWithTimestampAfter(timeStamp).map{it.toFlight()}


    fun getMostRecentFlightAsync() =
        async(dispatcher){
            cachedFlightsList?.let {
                it.filter {f -> !f.isPlanned && !f.isSim }.maxByOrNull {f -> f.timeOut }
            } ?: flightDao.getMostRecentCompleted()?.toFlight()
        }

    /**
     * Gets highest used FlightID or 0 if no flights in DB
     */
    fun getHighestIdAsync() = async(dispatcher) { flightDao.highestId() ?: 0 }


    /**
     * Gets the lowest free FlightID
     */
    suspend fun lowestFreeFlightID(): Int = withContext(Dispatchers.IO){
        cachedFlightsList?.maxByOrNull { it.flightID }?.flightID ?: getHighestIdAsync().await()
    }

    fun iAmACaptainAsync() =  async(dispatcher) {
        getMostRecentFlightAsync().await()?.isPIC == true
    }

    /**
     * Find flights that are conflicting with flights that are already known.
     * @return list of flights that overlap with flights in DB and are not the same
     */
    suspend fun findConflicts(flightsToCheck: List<Flight>, allowMargins: Boolean = true): List<Pair<Flight, Flight>> {
        val allFlights = getFlightsOnDays(getAllFlights(), flightsToCheck) // other flights are no conflict anyway
        val unknownNewFlights = flightsToCheck.filter { f ->
            allFlights.none {
                if (allowMargins)
                    it.isSameFlightAsWithMargins(f, Preferences.maxChronoAdjustment * 60L)
                else it.isSameFlightAs(f)
            }
        }
        return getOverlappingFlightsAsPairs(allFlights, unknownNewFlights)
    }

    suspend fun findNewFlights(flightsToCheck: List<Flight>): List<Flight> {
        val allFlights = getFlightsOnDays(getAllFlights(), flightsToCheck) // other flights are no conflict anyway
        return flightsToCheck.filter {
            it !in getOverlappingFlights(allFlights, flightsToCheck)
                    && it !in findMatches(flightsToCheck, true).map{it.first}
        }
    }


    /**
     * Find flights that are already known.
     * @return list of pairs of flights that overlap with flights in DB and are the same
     * (newFlight to knownFlight)
     */
    suspend fun findMatches(flightsToCheck: List<Flight>, checkEntireDay: Boolean = true, checkRegistrations: Boolean = false): List<Pair<Flight, Flight>>{
        val allFlights = getFlightsOnDays(getAllFlights(), flightsToCheck) // other flights are no match anyway
        val matches = flightsToCheck.filter { f ->
            allFlights.any {
                (if (checkEntireDay)
                    it.isSameFlightOnSameDay(f)
                else it.isSameFlightAs(f))
                        && if (checkRegistrations) (it.registration == f.registration).also{ m ->
                    if (!m) Log.d("findMatches", "Mismatched registration: ${it.registration} != ${f.registration}")
                } else true // if [checkRegistrations] registrations need to be the same as well
            }
        }
        return matches.map{ f-> f to allFlights.first{
            if (checkEntireDay)
                it.isSameFlightOnSameDay(f)
            else it.isSameFlightAs(f)
        }}
    }

    /********************************************************************************************
     * Interfaces:
     ********************************************************************************************/

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
}