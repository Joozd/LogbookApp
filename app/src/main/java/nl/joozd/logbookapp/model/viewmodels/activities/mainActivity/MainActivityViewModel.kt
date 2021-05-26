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

package nl.joozd.logbookapp.model.viewmodels.activities.mainActivity



import android.content.Intent
import android.content.Intent.ACTION_VIEW
import android.net.Uri
import android.util.Log
import androidx.lifecycle.*
import androidx.lifecycle.Transformations.distinctUntilChanged
import kotlinx.coroutines.*
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import nl.joozd.logbookapp.data.comm.Cloud
import nl.joozd.logbookapp.data.comm.InternetStatus
import nl.joozd.logbookapp.data.comm.UserManagement
import nl.joozd.logbookapp.data.export.JoozdlogExport
import nl.joozd.logbookapp.data.repository.GeneralRepository
import nl.joozd.logbookapp.data.repository.helpers.overlaps
import nl.joozd.logbookapp.data.sharedPrefs.Preferences
import nl.joozd.logbookapp.data.sharedPrefs.errors.Errors
import nl.joozd.logbookapp.data.sharedPrefs.errors.ScheduledErrors
import nl.joozd.logbookapp.extensions.*
import nl.joozd.logbookapp.model.dataclasses.DisplayFlight
import nl.joozd.logbookapp.model.dataclasses.Flight
import nl.joozd.logbookapp.model.feedbackEvents.FeedbackEvents.MainActivityEvents
import nl.joozd.logbookapp.model.helpers.FlightConflictChecker
import nl.joozd.logbookapp.model.viewmodels.JoozdlogActivityViewModel
import nl.joozd.logbookapp.model.workingFlight.WorkingFlight
import nl.joozd.logbookapp.utils.CoroutineTimerTask
import nl.joozd.logbookapp.workmanager.JoozdlogWorkersHub
import java.time.*
import java.util.*

class MainActivityViewModel: JoozdlogActivityViewModel() {

    /*********************************************************************************************
     * Private parts
     *********************************************************************************************/

    private val icaoIataMap
        get() = airportRepository.icaoIataMap.value ?: emptyMap()

    private val rawFlights
        get() = flightRepository.liveFlights.value ?: emptyList()

    // TODO do this work in adapter instead of on all flights that are never shown
    private val flightsList
        get() = flightsToDisplayFlightsWithErrorCheck(searchFlights(rawFlights))

    private val _backupInterval: LiveData<Int> = Preferences.backupIntervalLiveData

    private val _showBackupNotice = MediatorLiveData<Boolean>().apply{
        value = backupDialogShouldBeShown()
        addSource(_backupInterval){
            value = backupDialogShouldBeShown()
        }
        addSource(Preferences.mostRecentBackupLiveData){
            value = backupDialogShouldBeShown()
        }
    }

    private val _backupUri = MutableLiveData<Uri>()

    private val searchStringLiveData = MutableLiveData<String>()
    private val searchSpinnerSelection = MutableLiveData<Int>()

    private var searchFieldOpen: Boolean = false

    private var query: String
        get() = searchStringLiveData.value ?: ""
        set(it) {
            searchStringLiveData.value = it
        }
    private var searchType: Int
        get() = searchSpinnerSelection.value ?: ALL
        set(it) {
            searchSpinnerSelection.value = it
        }

    private val _displayFlightsList2 = MediatorLiveData<List<DisplayFlight>>().apply{
        addSource(flightRepository.liveFlights) {
            value = flightsList
        }
        addSource(airportRepository.icaoIataMap) {
            value = flightsList
        }
        addSource(airportRepository.useIataAirports) {
            value = flightsList
        }
        addSource(searchStringLiveData) {
            value = flightsList
        }
        addSource(searchSpinnerSelection) {
            value = flightsList
        }
    }

    /**
     * Server Error to show
     * Null if no errors present
     */
    private val _errorToShow = MutableLiveData(ScheduledErrors.currentErrors.firstOrNull())





    /**
     * Will search flights, return immediate results but if needed also update [_displayFlightsList2] async with more detailed data
     */
    private fun searchFlights(fff: List<Flight>?): List<Flight> {
        if (fff == null) return emptyList()
        if (!searchFieldOpen) return fff
        return when (searchType) {
            ALL -> searchAll(fff)
            AIRPORTS -> searchAirports(fff)
            AIRCRAFT -> searchAircraft(fff)
            NAMES -> searchNames(fff)
            FLIGHTNUMBER -> searchFlightnumber(fff)
            else -> fff
        }
    }

    private fun searchAll(fff: List<Flight>) = fff.filter {
        query in it.name.uppercase(Locale.ROOT)
                || query in it.name2.uppercase(Locale.ROOT)
                || query in it.flightNumber.uppercase(Locale.ROOT)
                || query in it.registration.uppercase(Locale.ROOT)
                || query in it.orig.uppercase(Locale.ROOT)
                || query in it.dest.uppercase(Locale.ROOT)
                || query in icaoIataMap[it.orig]?.uppercase(Locale.ROOT) ?: ""
                || query in icaoIataMap[it.dest]?.uppercase(Locale.ROOT) ?: ""
    }

    private fun searchAirports(fff: List<Flight>) = fff.filter {
        query in it.orig.uppercase(Locale.ROOT)
                || query in it.dest.uppercase(Locale.ROOT)
                || query in icaoIataMap[it.orig]?.uppercase(Locale.ROOT) ?: ""
                || query in icaoIataMap[it.dest]?.uppercase(Locale.ROOT) ?: ""
    }.also {
        viewModelScope.launch {
            // TODO make with async update from [airportRepository]
        }
    }


    private fun searchAircraft(fff: List<Flight>) = fff.filter {
        val ac = aircraftRepository.getAircraftTypeByShortName(it.aircraftType)
        query in it.registration.uppercase(Locale.ROOT)
                || ac?.shortName?.uppercase(Locale.ROOT)?.contains(query) ?: false
                || ac?.name?.uppercase(Locale.ROOT)?.contains(query) ?: false
    }

    private fun searchNames(fff: List<Flight>) = fff.filter {
        query in it.name.uppercase(Locale.ROOT)
                || query in it.name2.uppercase(Locale.ROOT)
    }

    private fun searchFlightnumber(fff: List<Flight>) = fff.filter {
        query in it.flightNumber.uppercase(Locale.ROOT)
    }

    private fun disableCalendarImportUntil(time: Long, silent: Boolean = false) {
        Preferences.calendarDisabledUntil = time
        if (!silent) feedback(MainActivityEvents.CALENDAR_SYNC_PAUSED)
    }

    private fun toggleSearchField() {
        if (searchFieldOpen)
            closeSearchField()
        else {
            feedback(MainActivityEvents.OPEN_SEARCH_FIELD)
            searchFieldOpen = true
        }
    }

    /*
    not using this anymore
    private fun setSearchFieldHint(it: Int) {
        _searchFieldHint.value = if (it == rawFlights.size) null else "$it flights found"
    }
    */

    /**
     * Returns whether the time between now and most recent backup is greater than [Preferences.backupInterval] days
     * Will count days starting from midnight LT, so if I just backed up and set it to 1 day, I will get a reminder at midnight.
     */
    private fun backupDialogShouldBeShown(): Boolean {
        if (Preferences.backupInterval == 0) return false
        val mostRecentBackup = Instant.ofEpochSecond(Preferences.mostRecentBackup).atStartOfDay(OffsetDateTime.now().offset)
        return (Instant.now() - mostRecentBackup > Duration.ofDays(Preferences.backupInterval.toLong())).also{
            if (it) feedback(MainActivityEvents.BACKUP_NEEDED)
        }
    }

    /**
     * Mark overlapping flights as markAsError
     */
    private fun flightsToDisplayFlightsWithErrorCheck(fff: List<Flight>): List<DisplayFlight> = fff.mapIndexed { index, f ->
        val previous = fff.getOrNull(index-1)
        val next = fff.getOrNull(index+1)
        DisplayFlight.of(f, icaoIataMap, Preferences.useIataAirports, error = f.overlaps(previous) || f.overlaps(next))
    }

    /*********************************************************************************************
     * Public parts
     *********************************************************************************************/

    /**
     * Observable data:
     */

    val displayFlightsList: LiveData<List<DisplayFlight>>
        get() = _displayFlightsList2

    val displayedFlightsAmount: LiveData<Int> = displayFlightsList.map { it.size }

    val picNameNeedsToBeSet: LiveData<Boolean>
        get() = distinctUntilChanged(Preferences.picNameNeedsToBeSetLiveData)

    val internetAvailable: LiveData<Boolean>
        get() = InternetStatus.internetAvailableLiveData

    val notLoggedIn: LiveData<Boolean>
        get() = flightRepository.notLoggedIn

    val airportSyncProgress: LiveData<Int>
        get() = airportRepository.airportSyncProgress

    val flightSyncProgress: LiveData<Int>
        get() = flightRepository.syncProgress

    val workingFlight: LiveData<WorkingFlight?>
        get() = flightRepository.workingFlight

    val showBackupNotice: LiveData<Boolean>
        get() = _showBackupNotice

    //server error to show
    val errorToShow: LiveData<Errors?>
        get() = _errorToShow

    val backupUri: LiveData<Uri>
        get() = _backupUri

    val undoAvailable
        get() = flightRepository.undoAvailable

    val redoAvailable
        get() = flightRepository.redoAvailable

    /**
     * Exposed vals
     */

    // SHows how many days you have not backed up. Days are counted at midnight local time.
    val daysSinceLastBackup: String = (minOf((Instant.now() - Instant.ofEpochSecond(Preferences.mostRecentBackup).atStartOfDay((OffsetDateTime.now().offset))).toDays(), 999L).toString())


    /*********************************************************************************************
     * Menu functions
     **********************************************************************************************/

    fun menuSelectedDoSomething() {
        /**
         * Current function: force update aircraft/airports on next update
         */
        JoozdlogWorkersHub.periodicSynchronizeAircraftTypes(false, true)

        /*
        viewModelScope.launch{
            flightRepository.getAllFlights().filter{it.timeIn < it.timeOut}.map{
                it.copy(timeIn = it.timeIn+86400, timeStamp = TimestampMaker.nowForSycPurposes)
            }.let{
                flightRepository.save(it)
            }
        }

         */
    }


    fun menuSelectedAddFlight() = addFlight()

    fun undo() = flightRepository.undo()


    fun redo() = flightRepository.redo()

    fun menuSelectedSearch() {
        toggleSearchField()
    }

    fun menuSelectedEditAircraft() {
        viewModelScope.launch {
            feedback(MainActivityEvents.NOT_IMPLEMENTED)
        }
    }

    fun menuSelectedAboutDialog(){
        viewModelScope.launch {
            feedback(MainActivityEvents.SHOW_ABOUT_DIALOG)
        }
    }


    /**
     * Handlers for clickety thingies
     */
    fun dismissBackup() {
        _showBackupNotice.value = false
        CoroutineTimerTask(Instant.now().atEndOfDay(OffsetDateTime.now().offset)).run(viewModelScope + Dispatchers.Main){
            _showBackupNotice.value = backupDialogShouldBeShown()
        }
    }

    fun backUpNow() = viewModelScope.launch {
        _showBackupNotice.value = false
        val dateString = LocalDate.now().toDateStringForFiles()
        //TODO make some kind of "working" animation on button
        _backupUri.value = JoozdlogExport.shareCsvExport("joozdlog_backup_$dateString")
        Preferences.mostRecentBackup = Instant.now().epochSecond
    }

    fun emailConfirmationErrorSeen(){
        ScheduledErrors.clearError(Errors.EMAIL_CONFIRMATION_FAILED)
    }

    fun badEmailErrorSeen(){
        ScheduledErrors.clearError(Errors.BAD_EMAIL_SAVED)
    }

    fun loginErrorSeen(){
        Preferences.useCloud = false
        Preferences.username = null
        Preferences.password = null
        ScheduledErrors.clearError(Errors.LOGIN_DATA_REJECTED_BY_SERVER)
    }

    fun serverErrorSeen(){
        ScheduledErrors.clearError(Errors.SERVER_ERROR)
    }



    private val openingFlightMutex = Mutex()
    fun showFlight(flightId: Int) = viewModelScope.launch {
        openingFlightMutex.withLock {
            WorkingFlight.fromFlightId(flightId)?.let {
                flightRepository.setWorkingFlight(it)
                // feedback(MainActivityEvents.SHOW_FLIGHT) this goes through livedata
            } ?: feedback(MainActivityEvents.FLIGHT_NOT_FOUND)
        }
    }

    fun addFlight() {
        viewModelScope.launch(Dispatchers.IO) {
            flightRepository.setWorkingFlight(WorkingFlight.createNew())
        }
    }

    /**
     * Does one of three things:
     * - if the flight with flightID [id] is:
     *  * nonexistent:      feedback(FLIGHT_NOT_FOUND)
     *  * planned:          Delete flight
     *  * Not planned:      feedback(TRYING_TO_DELETE_COMPLETED_FLIGHT) with its id in ExtraData as "ID"
     */
    fun deleteFlight(id: Int) {
        viewModelScope.launch {
            withContext(Dispatchers.IO) { flightRepository.fetchFlightByID(id) }.let {
                when (it?.isPlanned) {
                    null -> feedback(MainActivityEvents.FLIGHT_NOT_FOUND)
                    true -> {
                        if (Preferences.useCalendarSync && it.timeOut > Preferences.calendarDisabledUntil && it.timeOut > Instant.now().epochSecond)
                            feedback(MainActivityEvents.TRYING_TO_DELETE_CALENDAR_FLIGHT).apply {
                                extraData.putInt(MainActivityFeedbackExtraData.FLIGHT_ID, id)
                            }
                        else {
                            flightRepository.delete(it, addToUndo = true)
                            println("BANAAAAAN")
                            feedback(MainActivityEvents.DELETED_FLIGHT)
                        }
                    }
                    false -> feedback(MainActivityEvents.TRYING_TO_DELETE_COMPLETED_FLIGHT).apply {
                        extraData.putInt(MainActivityFeedbackExtraData.FLIGHT_ID, id)
                    }
                }
            }
        }
    }

    fun deleteNotPlannedFlight(id: Int) = flightRepository.delete(id, addToUndo = true)

    /*********************************************************************************************
     * Functions related to saving/loading working flight
     *********************************************************************************************/

    /**
     * Check if a saved flight will make a conflict with calendar sync
     * (ie. a planned flight gets times or airports changed)
     * @param f: Flight to check against FlightRepository.undoFlight
     * @return time (epochSecond) to disable calendarSync to if conflict, 0 if not
     */
    fun checkFlightConflictingWithCalendarSync(f: Flight): Long = if (f == flightRepository.undoSaveFlight) 0L
    else FlightConflictChecker.checkConflictingWithCalendarSync(flightRepository.undoSaveFlight, f)

    /*********************************************************************************************
     * Functions related to synchronization:
     *********************************************************************************************/

    /**
     * This will synch time with server and launch repository update functions (which can decide for themselves if it is necessary)
     */
    fun notifyActivityResumed() {
        GeneralRepository.synchTimeWithServer()
        flightRepository.syncIfNeeded()
        if (Preferences.emailVerified){
            Preferences.emailJobsWaiting.forEach {
                viewModelScope.launch { it() }
            }
        }
    }

    fun deleteAndDisableCalendarImportUntillAfterThisFlight(flightId: Int) {
        viewModelScope.launch {
            flightRepository.fetchFlightByID(flightId)?.let { flight ->
                disableCalendarImportUntil(flight.timeIn)
                flightRepository.delete(flight, addToUndo = true)
                feedback(MainActivityEvents.DELETED_FLIGHT)
            } ?: feedback(MainActivityEvents.FLIGHT_NOT_FOUND)
        }
    }

    fun disableCalendarImport() {
        Preferences.useCalendarSync = false
    }

    /*********************************************************************************************
     * Functions related to searching:
     *********************************************************************************************/

    fun closeSearchField() {
        feedback(MainActivityEvents.CLOSE_SEARCH_FIELD)
        query = ""
        searchFieldOpen = false
    }


    fun setSearchString(it: String) {
        query = it.uppercase(Locale.ROOT)
    }

    /**
     * Set selection from Search Spinner
     */
    fun setSpinnerSelection(it: Int) {
        searchType = it
    }


    /**
     * Intent handling:
     */

    fun handleIntent(intent: Intent?) {
        val action: String? = intent?.action
        val data: Uri? = intent?.data

        /**
         * Todo handle other links
         */
        if (action == ACTION_VIEW) {
            when(data?.pathSegments?.firstOrNull()){
                "inject-key"-> {
                    data.lastPathSegment?.let { lpString ->
                        Log.d("Uri", "lastPathSegment: $lpString")

                        //TODO needs sanity check
                        val loginPass = lpString.replace('-', '/').split(":").let { lp ->
                            lp.first() to lp.last()
                        }
                        viewModelScope.launch {
                            val result = UserManagement.loginFromLink(loginPass)
                            Log.d("LinkLogin", "result: $result")
                            if (result == null){
                                Preferences.loginLinkStringWaiting = lpString
                                JoozdlogWorkersHub.scheduleLoginAttempt()
                                feedback(MainActivityEvents.LOGIN_DELAYED_DUE_NO_SERVER)
                            }
                            else {
                                flightRepository.syncIfNeeded()
                                feedback(MainActivityEvents.LOGGED_IN)
                            }
                        }
                    }
                }
                "verify-email" -> {
                    data.lastPathSegment?.replace("-", "/")?.let {
                        viewModelScope.launch{
                            Log.d("mainViewModel", "Sending $it")
                            if (UserManagement.confirmEmail(it)) {
                                Cloud.requestLoginLinkMail()
                                feedback(MainActivityEvents.EMAIL_VERIFIED)
                            }
                            else _errorToShow.value = ScheduledErrors.currentErrors.firstOrNull()
                        }
                    }
                }
            }

        }
    }

    fun tryToFixLogin() = viewModelScope.launch {
        when (withContext(Dispatchers.IO) { UserManagement.tryToFixLogin() }) {
            true -> flightRepository.syncIfNeeded() // this will eventually check if login is correct and set flag accordingly, setting [notLoggedIn]
            false -> flightRepository.setNotLoggedInFlag(notLoggedIn = true) // This will set [notLoggedIn], triggering observer in MainActivity
            // null -> Don't do anything, server is not OK
        }
    }



        companion object {
        const val ALL = 0
        const val AIRPORTS = 1
        const val AIRCRAFT = 2
        const val NAMES = 3
        const val FLIGHTNUMBER = 4
    }
}

