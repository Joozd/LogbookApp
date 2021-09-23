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
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import nl.joozd.logbookapp.model.dataclasses.Flight
import nl.joozd.logbookapp.data.room.JoozdlogDatabase
import nl.joozd.logbookapp.data.room.dao.FlightDao
import nl.joozd.logbookapp.data.room.model.toFlight
import nl.joozd.logbookapp.data.utils.FlightsListFunctions.makeListOfNamesAsync
import nl.joozd.logbookapp.App
import nl.joozd.logbookapp.data.calendar.CalendarFlightUpdater
import nl.joozd.logbookapp.data.calendar.parsers.KlmIcalFlightsParser
import nl.joozd.logbookapp.data.comm.Cloud
import nl.joozd.logbookapp.data.parseSharedFiles.extensions.postProcess
import nl.joozd.logbookapp.data.parseSharedFiles.interfaces.CompletedFlights
import nl.joozd.logbookapp.data.parseSharedFiles.interfaces.Roster
import nl.joozd.logbookapp.data.repository.helpers.*
import nl.joozd.logbookapp.data.sharedPrefs.CalendarSyncTypes
import nl.joozd.logbookapp.data.sharedPrefs.Preferences
import nl.joozd.logbookapp.data.utils.FlightsListFunctions.makeListOfRegistrations
import nl.joozd.logbookapp.extensions.map
import nl.joozd.logbookapp.extensions.nullIfBlank
import nl.joozd.logbookapp.model.workingFlight.WorkingFlight
import nl.joozd.logbookapp.utils.TimestampMaker
import nl.joozd.logbookapp.utils.checkPermission
import nl.joozd.logbookapp.workmanager.JoozdlogWorkersHub
import java.lang.Exception
import java.time.Instant
import java.util.*

//TODO reorder this and make direct DB functions private
//TODO make cloud functions originate from here and do their DB work here, not in [Cloud]
class FlightRepository(private val flightDao: FlightDao, private val dispatcher: CoroutineDispatcher = Dispatchers.IO
): CoroutineScope by MainScope() {
    /********************************************************************************************
     * Private parts:
     ********************************************************************************************/

    private val saveMutex = Mutex()

    private val undoTracker = UndoTracker()

    private val nextFlightID = FlightIDProvider()

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
    private fun deleteFlightHard(flight: Flight, addToUndo: Boolean = false) = launch {
        println("Hard-deleting flight. AddToUndo is $addToUndo")
        if (addToUndo){
            undoTracker.addDeleteEvent(flight)
        }
        cachedFlightsList = ((cachedFlightsList
            ?: emptyList()).filter { it.flightID != flight.flightID }).sortedByDescending { it.timeOut }
        launch (dispatcher + NonCancellable) {
            saveMutex.withLock {
                flightDao.delete(flight.toModel())
            }
        }
    }

    /**
     * update cached data and delete multiple flights from disk
     */
    private fun deleteHard(flights: List<Flight>, addToUndo: Boolean = false) = launch {
        if (addToUndo){
            undoTracker.addDeleteEvent(flights)
        }
        Log.d("DeleteHard", "Cached flights before: ${cachedFlightsList?.size}")
        cachedFlightsList = ((cachedFlightsList
            ?: emptyList()).filter { it.flightID !in flights.map { f -> f.flightID } }).sortedByDescending { it.timeOut }
        Log.d("DeleteHard", "Cached flights after: ${cachedFlightsList?.size}")
        launch (dispatcher + NonCancellable) {
            saveMutex.withLock {
                Log.d("DeleteHard","Deleting ${flights.size} flights")
                flightDao.deleteMultipleByID(flights.map { it.flightID })
            }
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
     * @param period: Period in which these flights should be
     */
    private suspend fun getFlightsStartingInPeriod(period: ClosedRange<Instant>) =
        getAllFlights().filter { it.timeOut in period.map { instant -> instant.epochSecond } }

    private val _syncProgress = MutableLiveData(-1)

    private val _notLoggedIn = MutableLiveData<Boolean>()

    //Might become private, depending on how i will do cloud syncs
    suspend fun requestWholeDB(): List<Flight> = withContext(dispatcher){
        flightDao.requestAllFlights().map{it.toFlight()}
    }

    /**
     * Get a flight from its ID, or null if no flight found by that ID
     */
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

    fun getWorkingFlight(): WorkingFlight = workingFlight.value ?: error ("WorkingFlight not initialized")

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

    val undoAvailableLiveData: LiveData<Boolean>
        get() = undoTracker.undoAvailableLiveData

    val undoAvailable: Boolean
        get() = undoTracker.undoAvailable

    val redoAvailable: Boolean
        get() = undoTracker.redoAvailable

    val redoAvailableLiveData: LiveData<Boolean>
        get() = undoTracker.redoAvailableLiveData

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

    /**
     * The ONLY way to set working flight to a Flight.
     */
    fun setWorkingFlight(f: WorkingFlight?){
        if (Looper.myLooper() != Looper.getMainLooper()) launch(Dispatchers.Main){
            Log.w("setWorkingFlight", "setWorkingFlight called on a background thread, setting workingFlight async on main")
            _workingFlight.value = f
        }
        else _workingFlight.value = f
    }

    /**
     * Close working flight (stop observing all Mediators and set it's liveData to null)
     */
    fun closeWorkingFlight(){
        _workingFlight.value = null
    }

    suspend fun lowestFreeFlightID() = nextFlightID()


    /**
     * Delete functions. Use delete(flight) or delete(flightsList)
     * Function will decide whether it can be just locally deleted or softdeleted for sync purposes
     */

    /**
     * Delete flight from disk if not known to server, else set DELETEFLAG to 1 and update timestamp
     * soft-delete will update cache through Dao observer
     */
    fun delete(flight: Flight, addToUndo: Boolean = false) {
        Log.d(this::class.simpleName, "Deleting flight $flight. AddToUndo is $addToUndo")
        if (flight.unknownToServer) deleteFlightHard(flight, addToUndo).also{ Log.d(this::class.simpleName, "hard-deleted flight $flight")}
        else save(
            flight.copy(
                DELETEFLAG = true,
                timeStamp = TimestampMaker().nowForSycPurposes,
            ),
            addToUndo = addToUndo
        ).also{ Log.d(this::class.simpleName, "soft-deleted flight $flight")}
    }


    /**
     * Same but find it first by looking up it's ID
     */
    fun delete(id: Int, addToUndo: Boolean = false) {
        launch(NonCancellable){
            fetchFlightByID(id)?.let { delete (it, addToUndo) } ?: Log.w("FlightRepository", "delete(id: Int): No flight found with id $id")
        }
    }
    /**
     * Same as deleteFlight, but with a list of multiple Flights
     */
    fun delete(flights: List<Flight>, sync: Boolean = true, addToUndo: Boolean = false) {
        deleteHard(flights.filter { it.unknownToServer }, addToUndo = addToUndo)
        save(flights.filter { !it.unknownToServer }.map { f ->
            f.copy(
                DELETEFLAG = true
            )
        }, sync, addToUndo = addToUndo)
    }

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
     * put a flight in [changedFlight] so you can check any changes made
     */
    var changedFlight: Flight? = null

    /**
     * Undo last undoable event if able
     */
    fun undo() = undoTracker.undo()

    /**
     * Redo last undoable event if able
     */
    fun redo() = undoTracker.redo()

    /**
     * update cached data and save to disk
     * This will update Flight.timeStamp
     * @param flight: Flight to save
     * @param updateID:
     *      true: always make a new ID
     *      false: never make a new ID
     *      null (default): make a new ID if id <= 0
     * @param sync: Whether or not to sync to server after saving
     * @param addToUndo: If this save action should be undoable
     */
    fun save(flight: Flight, sync: Boolean = true, updateID: Boolean? = null, addToUndo: Boolean = false) = launch {
        saveMutex.withLock {
            //assign available FlightID if requested
            val f = if (updateID == true || (flight.flightID < 0) && updateID == null) flight.copy(flightID = lowestFreeFlightID(), timeStamp = TimestampMaker().nowForSycPurposes) else flight.copy(timeStamp = TimestampMaker().nowForSycPurposes)

            // Add to undo if needed:
            if (addToUndo){
                // If this got a freshly assigned ID, undo means delete because [fetchFlightByID] will return null
                undoTracker.addSaveEvent(f, fetchFlightByID(f.flightID))
            }



            //update cached flights
            cachedFlightsList = ((cachedFlightsList ?: emptyList()).filter { it.flightID != f.flightID }
                    + listOf(f).filter { !it.DELETEFLAG })
                .sortedByDescending { it.timeOut }

            //Save flight to disk
            //This part is not locked (it will suspend and @withLock will end before this finishes), but the lock should keep the cache up-to-date.
            launch(dispatcher + NonCancellable) {
                flightDao.insertFlights(f.toModel())
                if (sync) syncAfterChange()
            }
        }
    }

    /**
     * update cached data and save to disk
     * @see [save]
     * @updateIDs:
     *      true: always make a new ID
     *      false: never make a new ID
     *      null (default): make a new ID if id <= 0
     */
    fun save(flights: List<Flight>, sync: Boolean = true, updateIDs: Boolean? = null, addToUndo: Boolean = false) = launch {
        saveMutex.withLock {
            //assign available FlightIDs if requested
            val ff = if (updateIDs != false) flights.map { flight ->
                flight.copy(flightID = if (flight.flightID <= 0 || updateIDs == true) nextFlightID() else flight.flightID, timeStamp = TimestampMaker().nowForSycPurposes)
            } else flights.map {
                it.copy(timeStamp = TimestampMaker().nowForSycPurposes)
            }

            // Add to undo if needed:
            if (addToUndo){
                undoTracker.addSaveEvent(ff, ff.mapNotNull{ f -> fetchFlightByID(f.flightID)})
            }

            //update cached flights
            cachedFlightsList = ((cachedFlightsList ?: emptyList()).filter { it.flightID !in ff.map { it2 -> it2.flightID } }
                    + ff.filter { !it.DELETEFLAG })
                .sortedByDescending { it.timeOut }
            //Save flights to disk
            launch(dispatcher + NonCancellable) {
                flightDao.insertFlights(*(ff.map { it.toModel() }.toTypedArray()))
                Log.d("Saved", "Saved ${ff.size} flights!")
                if (sync) syncAfterChange()
            }
        }
    }

    /**
     * Save flights from a roster
     * This will compare currently saved flights with flights in roster.
     *  - Exact matches (orig, dest, tin, tout, flightNumber) will be kept
     *      ->  These will be compared for some other data as well (name, name2, remarks, registration)
     *          If any of those fields has data in roster, it will overwrite data in db if different
     *  - Other flights in period will be deleted(hard) if planned, or ignored if completed.
     *  - Rostered flights that are not exact matches + matches with extra data will be saved to DB
     *  @param roster: a Roster with flights to be planned
     *  @param canUndo: If true, this action will be given to [undoTracker]
     *  NOTE If any times have been changed (like off-blocks time was entered when going off-blocks) a flight will not be recognized as being the same
     *          The thought here is that if you want to enter extra data like crew names from a roster, you usually do that before modifying times,
     *          and if you import a roster after modifying times, you probably do that because you want the original times back.
     */
    suspend fun saveRoster(roster: Roster, canUndo: Boolean = false) = withContext(Dispatchers.IO + NonCancellable){
        val plannedFlightsInDB = getFlightsStartingInPeriod(roster.period).filter { it.isPlanned }
        //unchanged flights, these will not be deleted
        val unchangedFlights = plannedFlightsInDB.filter { pf -> roster.flights.any { rf-> rf.isSameFlightAs(pf)}}

        //These flights (the flights that are not unchanged) will be deleted.
        val flightsToDelete = plannedFlightsInDB.filter { it !in unchangedFlights }

        //These flights will be saved from roster (roster flights that are not in [unchangedFlights]
        val rosterFlightsToSave = roster.flights.filter { rf -> unchangedFlights.none{ pf -> pf.isSameFlightAs(rf)}}

        //These flights will also be saved, overwriting existing flights (unchanged flights that got extra info from roster)
        val updatedFlights = updateFlightsWithRosterData(unchangedFlights, roster.flights)

        val flightsToSave = (rosterFlightsToSave + updatedFlights).map{
            if (it.flightID >0 )it else it.copy (flightID = nextFlightID() )
        }

        if (canUndo){
            val deleteEvent = DeleteEvent(flightsToDelete)
            val saveEvent = SaveEvent(flightsToSave, flightsToSave.mapNotNull{ f -> fetchFlightByID(f.flightID)})
            undoTracker.addRosterImportEvent(RosterImportEvent(saveEvent, deleteEvent))
        }

        delete(flightsToDelete, sync = false, addToUndo = false) // sync will happen after saving
        save(flightsToSave, addToUndo = false)                   // this will trigger sync
    }

    /**
     * Save flights from a [CompletedFlights] object
     * It will save flights in [completedFlights] or update flights already saved that are the same with updated data
     * updated data can be:
     *  - Flight number
     *  - time out
     *  - time in
     *  - Registration
     *  - Aircraft Type
     * @return the amount of conflicts found (doesn't handle conflicts, only detects them)
     */
    suspend fun saveCompletedFlights(completedFlights: CompletedFlights, canUndo: Boolean = true): SaveCompleteFlightsResult = withContext(Dispatchers.IO + NonCancellable){
        require(completedFlights.isValid) { "Cannot parse an invalid roster! You should have checked this!" }
        val importFlights = completedFlights.flights
        val flightsInPeriod = getFlightsStartingInPeriod(completedFlights.period)

        // New flights are those who do not have (a planned version of) it in logbook already
        val newFlights = importFlights.filter { importFlight ->flightsInPeriod.none { importFlight.isUpdatedVersionOf(it) || importFlight.isSameFlightAs(it) }}
            .map{ it.copy (flightID = nextFlightID())}

        println("I FOUND ${newFlights.size} NEW FLIGHTS FLIGHTS: ${newFlights.map{it.shortString() +"\n"}}")

        //Flights that will be updated, for undo purposes
        val oldFlightsThatWillBeUpdated = flightsInPeriod.filter { fip -> importFlights.any { it.isUpdatedVersionOf(fip)}}

        // Flights To Update are those who do have a planned version of it in logbook already.
        val flightsToUpdate = importFlights.filter { importFlight -> oldFlightsThatWillBeUpdated.any { importFlight.isUpdatedVersionOf(it) }}.map{ importFlight ->
            importFlight.mergeInto(oldFlightsThatWillBeUpdated.first{ importFlight.isUpdatedVersionOf(it) })
        }
        println("I FOUND ${flightsToUpdate.size} FLIGHTS TO UPDATE: ${flightsToUpdate.map{it.shortString() +"\n"}}")

        //Conflicts tracking
        //A conflict is a flight that overlaps another flight
        //Planned flights are not considered as conflicts
        val conflicts = flightsInPeriod.filter { fip -> newFlights.any{ importFlight -> importFlight.overlaps(fip) && !importFlight.isSameFlightAs(fip) } }

        if (canUndo){
            undoTracker.addSaveEvent(newFlights + flightsToUpdate, oldFlightsThatWillBeUpdated)
        }

        // Save new flights
        save(newFlights + flightsToUpdate, updateIDs = false, sync = true)

        val remainingPlanned = flightsInPeriod.filter { it.isPlanned && it !in oldFlightsThatWillBeUpdated }

        return@withContext SaveCompleteFlightsResult(conflicts.size, remainingPlanned.size)
    }

    /**
     * Sync functions:
     */


    /**
     * This does two things:
     * first, it checks if a server and/or calendar update is needed.
     * If needed, updates calendar (only planned flights)     *
     * If needed, synchs with server (no planned flights)
     *      Server Sync will create an account on server if [Preferences.username] is empty
     */
    fun syncIfNeeded(){
        Log.i("FlightRepo", "Starting Synchronization")
        launch {
            val needsServerSync =
                TimestampMaker().nowForSycPurposes - Preferences.lastUpdateTime > MIN_SYNC_INTERVAL   // interval for checking remote changes has passed
                        || getFlightsChangedAfter(Preferences.lastUpdateTime).isNotEmpty()          // OR local changes since last sync

            val needsCalendarSync = TimestampMaker().nowForSycPurposes > Preferences.nextCalendarCheckTime

            println("needsCalendarSync: $needsCalendarSync && Preferences.useCalendarSync: ${Preferences.useCalendarSync}")
            if (needsCalendarSync && Preferences.useCalendarSync) {
                println("KOEKWAUS JONGE")
                when (Preferences.calendarSyncType) {
                    CalendarSyncTypes.CALENDAR_SYNC_DEVICE -> {
                        if (checkPermission(Manifest.permission.READ_CALENDAR)) {
                            CalendarFlightUpdater().getRoster()
                        } else null
                    }
                    CalendarSyncTypes.CALENDAR_SYNC_ICAL -> {
                        println("jajaja")
                        Preferences.calendarSyncIcalAddress.nullIfBlank()?.let{ urlString ->
                            println(urlString)
                            try {
                                KlmIcalFlightsParser.ofString(urlString)
                            } catch (e: Exception){
                                Log.w("iCal sync", "Caught exception $e")
                                null
                            }
                        }
                    }
                    else -> null
                } ?.let { roster ->
                    println("FOUND ROSTER with period ${roster.period}")
                    roster.flights.map {it.shortString()}.forEach{
                        println(it)
                    }
                    saveRoster(roster.postProcess())
                    Preferences.nextCalendarCheckTime = roster.validUntil.epochSecond
                }
            }
            if (needsServerSync && Preferences.useCloud)
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
    private suspend fun getFlightsChangedAfter(timeStamp: Long) =
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
    private fun getHighestIdAsync() = async(dispatcher) { flightDao.highestId() ?: 0 }


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
                    it.isSameCompletedFlight(f, Preferences.maxChronoAdjustment * 60L)
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
     * Classes:
     ********************************************************************************************/



    /**
     * This keeps track of changes that can be undone, and does the undoing
     * Things that CAN be undone:
     *      - User initiated things like editing, creating or deleting a flight
     *      - User initiated imports from PdfParserActivity
     * Things that CANNOT be undone:
     *      - Server initiated actions, like syncing, or deleting SOFT_DELETED flights after sync.
     *      TODO: Think about whether completed flights should ever be hard-deleted or only soft, for retrieval purposes
     *
     * Undo() will undo an action and move it onto [redoStack]
     * Redo() will undo an undo and move the action back to [undoStack]
     */
    private inner class UndoTracker() {
        val undoAvailableLiveData: LiveData<Boolean>
            get() = _undoAvailable
        val redoAvailableLiveData: LiveData<Boolean>
            get() = _redoAvailable

        /**
         * keeps track if undo is available. sets [_undoAvailable] livedata if changed.
         */
        var undoAvailable: Boolean = false
            private set(it){
                println("SET UNDO_AVAILABLE to $it")
                field = it
                if (_undoAvailable.value != it) _undoAvailable.postValue(it)
            }

        /**
         * keeps track if redo is available. sets [_redoAvailable] livedata if changed.
         */
        var redoAvailable: Boolean = false
            private set(it){
                println("SET REDO_AVAILABLE to $it")
                field = it
                _redoAvailable.postValue(it)
            }

        private val _undoAvailable = MutableLiveData(undoAvailable)
        private val _redoAvailable = MutableLiveData(redoAvailable)

        // Stack of undo events
        private val undoStack = mutableListOf<UndoableEvent>()

        // Stack of redo events. Must be cleared when a new undo event is added
        // Any undo event that is undone is added here so it can be redone
        private val redoStack = mutableListOf<UndoableEvent>()

        /**
         * Add a SAVED_FLIGHT event to queue. This includes creating new and soft deletes.
         * This will clear the REDO queue
         * @param newFlights: List of flights saved
         * @param oldFlights: List of flights as they were before saving
         */
        fun addSaveEvent(newFlights: List<Flight>, oldFlights: List<Flight>) {
            pushUndoEvent(SaveEvent(newFlights, oldFlights))
            clearRedoStack()
        }

        fun addSaveEvent(newFlight: Flight, oldFlight: Flight?) = addSaveEvent(listOf(newFlight), listOfNotNull(oldFlight))

        /**
         * Add a HARD DELETE event
         */
        fun addDeleteEvent(flights: List<Flight>){
            pushUndoEvent(DeleteEvent(flights))
        }
        fun addDeleteEvent(flight: Flight) = addDeleteEvent(listOf(flight))

        fun addRosterImportEvent(rosterImportEvent: RosterImportEvent){
            pushUndoEvent(rosterImportEvent)
        }

        /**
         * Undo top UndoableEvent in [undoStack]
         * Add it to [redoStack]
         * @param redoable: Put undone event on redo stack if true
         */
        fun undo(redoable: Boolean = true): Boolean{
            return when (val event = popUndo()){
                is DeleteEvent -> {
                    Log.d("Undo", "DeleteEvent Undo $redoable")
                    save(event.oldFlights, addToUndo = false)
                    if (redoable)
                        pushRedoEvent(event)
                    true
                }
                is SaveEvent -> {
                    Log.d("Undo", "SaveEvent Undo $redoable")
                    // delete flights that were newly created
                    val newlyCreatedFlights = event.newFlights.filter { it.flightID !in event.oldFlights.map{old -> old.flightID}}
                    Log.d("NewlyCreatedFlights", "${newlyCreatedFlights.size}, ${newlyCreatedFlights.map{it.flightID} }")


                    deleteHard(newlyCreatedFlights, addToUndo = false)

                    // save old version of flights that were changed
                    save(event.oldFlights, addToUndo = false)
                    if (redoable)
                        pushRedoEvent(event)
                    true
                }
                is RosterImportEvent -> {
                    Log.d("Undo", "RosterImportEvent Undo $redoable")
                    //put deletes and updates on undo stack
                    pushUndoEvent(event.deleted)
                    pushUndoEvent(event.saved)
                    //undo deleting and saving
                    undo(false)
                    undo(false)
                    pushRedoEvent(event)
                    true
                }
                else -> {
                    Log.w("Undo", "Invalid event: $event")
                    false
                }
            }
        }

        /**
         * Redo an undone action.
         * @param undoable: true if this redo can be undone. Meant for internal use (ie. a RosterImportEvent that is being redone)
         */
        fun redo(undoable: Boolean = true): Boolean{
            if (redoStack.isEmpty()) return false
            return when(val event = popRedo()){
                is DeleteEvent -> {
                    Log.d("Redo", "DeleteEvent Redo $undoable")
                    deleteHard(event.oldFlights, addToUndo = false) // this can put it back on undo stack, but doing it manually is faster than rebuilding the event
                    if (undoable) pushUndoEvent(event, false)
                    true
                }
                is SaveEvent -> {
                    Log.d("Redo", "SaveEvent Redo $undoable")
                    save(event.newFlights, addToUndo = false) // this can put it back on undo stack, but doing it manually is faster than rebuilding the event
                    if (undoable) pushUndoEvent(event, false)
                    true
                }
                is RosterImportEvent -> {
                    Log.d("Redo", "RosterImportEvent Redo $undoable")
                    // put deleted and saved redo event on top of stack and redo them
                    pushRedoEvent(event.deleted)
                    pushRedoEvent(event.saved)
                    redo(false) // don't put on undo stack as the RosterImportEvent will be pushed to it
                    redo(false) // don't put on undo stack as the RosterImportEvent will be pushed to it
                    pushUndoEvent(event, false)
                    true
                }
                else -> {
                    Log.w("Redo", "Invalid event: $event")
                    false
                }
            }
        }

        private fun clearRedoStack(){
            redoStack.clear()
            redoAvailable = false
        }

        /**
         * Remove last item from undo stack and return it.
         * Also keeps [undoAvailable] up-to-date
         */
        private fun popUndo(): UndoableEvent?{
            val r = undoStack.removeLastOrNull()
            undoAvailable = undoStack.isNotEmpty()
            return r
        }

        /**
         * Remove last item from redo stack and return it.
         * Also keeps [redoAvailable] up-to-date
         */
        private fun popRedo(): UndoableEvent?{
            val r = redoStack.removeLastOrNull()
            redoAvailable = redoStack.isNotEmpty()
            return r
        }

        /**
         * Push an event onto undo stack.
         * Keeps [undoAvailable] op-to-date
         * @param event: Event to put on undo stack
         * @param emptyRedoStack: If true, will empty redo stack. Should only be false when a redo item is put on redo stack from undo stack.
         */
        private fun pushUndoEvent(event: UndoableEvent, emptyRedoStack: Boolean = true){
            undoStack.add(event)
            if (emptyRedoStack)
                clearRedoStack()
            undoAvailable = undoStack.size > 0
        }

        /**
         * Push an event onto redo stack.
         * Keeps [redoAvailable] op-to-date
         */
        private fun pushRedoEvent(event: UndoableEvent){
            redoStack.add(event)
            redoAvailable = redoStack.size > 0
        }
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
    }

    /**
     * Worker class that will always provide an available flight ID in a thread-safe way
     * van be used like this:
     *      private val getID = FlightIDProvider()
     *      getID()
     */
    private inner class FlightIDProvider {
        private var currentLowestAvailable: Int = - 999
        private var initialized = false
        val mutex = Mutex()

        suspend operator fun invoke(): Int = mutex.withLock {
            // set currentLowestAvailable to lowest free ID if no initialized yet
            if (!initialized) {
                currentLowestAvailable = getNextAvailable()
                initialized = true
            }

            return currentLowestAvailable++
        }
        /**
         * Gets the lowest free FlightID
         */
        private suspend fun getNextAvailable(): Int = withContext(Dispatchers.IO){
            (cachedFlightsList?.maxByOrNull { it.flightID }?.flightID ?: getHighestIdAsync().await()) + 1
        }
    }

    class SaveCompleteFlightsResult(val conflicts: Int, val plannedRemaining: Int)
}