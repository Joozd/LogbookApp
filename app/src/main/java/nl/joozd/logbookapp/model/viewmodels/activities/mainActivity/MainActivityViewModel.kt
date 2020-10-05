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
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import kotlinx.coroutines.withContext
import nl.joozd.logbookapp.data.comm.InternetStatus
import nl.joozd.logbookapp.data.comm.UserManagement
import nl.joozd.logbookapp.data.repository.GeneralRepository
import nl.joozd.logbookapp.data.sharedPrefs.Preferences
import nl.joozd.logbookapp.extensions.nullIfZero
import nl.joozd.logbookapp.model.dataclasses.DisplayFlight
import nl.joozd.logbookapp.model.dataclasses.Flight
import nl.joozd.logbookapp.model.feedbackEvents.FeedbackEvents.MainActivityEvents
import nl.joozd.logbookapp.model.helpers.FlightConflichtChecker
import nl.joozd.logbookapp.model.viewmodels.JoozdlogActivityViewModel
import nl.joozd.logbookapp.model.workingFlight.WorkingFlight
import nl.joozd.logbookapp.utils.TimestampMaker
import java.time.Instant
import java.util.*

class MainActivityViewModel: JoozdlogActivityViewModel() {

    /*********************************************************************************************
     * Private parts
     *********************************************************************************************/

    private val icaoIataMap
        get() = airportRepository.icaoIataMap.value ?: emptyMap()

    private val rawFlights
        get() = flightRepository.liveFlights.value ?: emptyList()
    private val flightsList
        get() = searchFlights(rawFlights).map { DisplayFlight.of(it, icaoIataMap, Preferences.useIataAirports) }.also { setSearchFieldHint(it.size) }

    private val searchStringLiveData = MutableLiveData<String>()
    private val searchSpinnerSelection = MutableLiveData<Int>()
    private val _searchFieldHint = MutableLiveData<String?>()

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

    private val _displayFlightsList2 = MediatorLiveData<List<DisplayFlight>>()

    init {
        _displayFlightsList2.addSource(flightRepository.liveFlights) {
            _displayFlightsList2.value = flightsList
        }
        _displayFlightsList2.addSource(airportRepository.icaoIataMap) {
            _displayFlightsList2.value = flightsList
        }
        _displayFlightsList2.addSource(airportRepository.useIataAirports) {
            _displayFlightsList2.value = flightsList
        }
        _displayFlightsList2.addSource(searchStringLiveData) {
            _displayFlightsList2.value = flightsList
        }
        _displayFlightsList2.addSource(searchSpinnerSelection) {
            _displayFlightsList2.value = flightsList
        }
    }

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
        query in it.name.toUpperCase(Locale.ROOT)
                || query in it.name2.toUpperCase(Locale.ROOT)
                || query in it.registration.toUpperCase(Locale.ROOT)
                || query in it.orig.toUpperCase(Locale.ROOT)
                || query in it.dest.toUpperCase(Locale.ROOT)
                || query in icaoIataMap[it.orig]?.toUpperCase(Locale.ROOT) ?: ""
                || query in icaoIataMap[it.dest]?.toUpperCase(Locale.ROOT) ?: ""
    }

    private fun searchAirports(fff: List<Flight>) = fff.filter {
        query in it.orig.toUpperCase(Locale.ROOT)
                || query in it.dest.toUpperCase(Locale.ROOT)
                || query in icaoIataMap[it.orig]?.toUpperCase(Locale.ROOT) ?: ""
                || query in icaoIataMap[it.dest]?.toUpperCase(Locale.ROOT) ?: ""
    }.also {
        viewModelScope.launch {
            // TODO make with async update from [airportRepository]
        }
    }


    private fun searchAircraft(fff: List<Flight>) = fff.filter {
        query in it.registration.toUpperCase(Locale.ROOT)
    }.also {
        viewModelScope.launch {
            //TODO make with async update from [aircraftRepository]
        }
    }

    private fun searchNames(fff: List<Flight>) = fff.filter {
        query in it.name.toUpperCase(Locale.ROOT)
                || query in it.name2.toUpperCase(Locale.ROOT)
    }

    private fun searchFlightnumber(fff: List<Flight>) = fff.filter {
        query in it.flightNumber.toUpperCase(Locale.ROOT)
    }

    private fun disableCalendarImportUntil(time: Long) {
        Preferences.calendarDisabledUntil = time
        feedback(MainActivityEvents.CALENDAR_SYNC_PAUSED)
    }

    private fun toggleSearchField() {
        if (searchFieldOpen)
            closeSearchField()
        else {
            feedback(MainActivityEvents.OPEN_SEARCH_FIELD)
            searchFieldOpen = true
        }
    }

    private fun setSearchFieldHint(it: Int) {
        _searchFieldHint.value = if (it == rawFlights.size) null else "$it flights found"
    }

    /*********************************************************************************************
     * Public parts
     *********************************************************************************************/

    /**
     * Observable data:
     */

    val displayFlightsList: LiveData<List<DisplayFlight>>
        get() = _displayFlightsList2

    val searchFieldHint
        get() = _searchFieldHint

    /**
     * Livedata related to synchronization:
     */

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

    val savedflight
        get() = flightRepository.savedFlight


    /*********************************************************************************************
     * Menu functions
     **********************************************************************************************/

    fun menuSelectedDoSomething() {
        /**
         * Current function: Fix names lists
         */
        viewModelScope.launch {

            val timeStamp = TimestampMaker.nowForSycPurposes
            val allFlights: List<Flight> = flightRepository.getAllFlights().map { f ->
                f.copy(name2 = f.name2.replace('|', ';'))
            }.also {
                Log.d("menuSelectedDoSomething", "Done. Fixed ${it.filter { it.timeStamp == timeStamp }.size} / ${it.size} flights")
            }
            flightRepository.save(allFlights)
            feedback(MainActivityEvents.DONE)

        }
    }

    fun menuSelectedRebuild() {
        feedback(MainActivityEvents.NOT_IMPLEMENTED)
        /*
val ENABLE_REBUILD = true
if (ENABLE_REBUILD) {
    launch { MenuFunctions.rebuildFlightsFromServer(this@MainActivity) }
}
else{
    longToast("Disabled!")            }

 */
    }

    fun menuSelectedAddFlight() = addFlight()

    fun menuSelectedTotalTimes() {
        feedback(MainActivityEvents.NOT_IMPLEMENTED)
    }

    fun menuSelectedBalanceForward() {
        feedback(MainActivityEvents.NOT_IMPLEMENTED)
    }

    fun menuSelectedSearch() {
        toggleSearchField()
    }

    fun menuSelectedEditAircraft() {
        viewModelScope.launch {
            feedback(MainActivityEvents.NOT_IMPLEMENTED)
        }
    }


    /**
     * Handler for clickety thingies
     */


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
     * Undo saving of a flight bu reverting to its backed up state in FlightRepository
     * If a [savedflight] was given and there is no flightRepository.undoSaveFlight, it is a new flight wchich will be deleted
     */
    fun undoSaveWorkingFlight(savedFlightToUndo: Flight? = null) = flightRepository.undoSaveFlight?.let { undoFlight ->
        flightRepository.save(undoFlight)
    }
        ?: savedFlightToUndo?.let { savedFlight ->
            flightRepository.delete(savedFlight)
        } ?: feedback(MainActivityEvents.FLIGHT_NOT_FOUND)

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
                        if (Preferences.getFlightsFromCalendar && it.timeOut > Preferences.calendarDisabledUntil && it.timeOut > Instant.now().epochSecond)
                            feedback(MainActivityEvents.TRYING_TO_DELETE_CALENDAR_FLIGHT).apply {
                                extraData.putInt(MainActivityFeedbackExtraData.FLIGHT_ID, id)
                            }
                        else {
                            flightRepository.delete(it)
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

    fun deleteNotPlannedFlight(id: Int) = flightRepository.delete(id)

    fun undoDeleteFlight() {
        flightRepository.undeleteFlight()
    }


    /*********************************************************************************************
     * Functions related to saving/loading working flight
     *********************************************************************************************/

    /**
     * Check if a saved flight will make a conflict with calendar sync
     * (ie. a planned flight gets times or airports changed)
     * @param f: Flight to check against FlightRepository.undoFlight
     */
    fun checkFlightConflictingWithCalendarSync(f: Flight): Long = if (f == flightRepository.undoSaveFlight) 0L
    else FlightConflichtChecker.checkConflictingWithCalendarSync(flightRepository.undoSaveFlight, f)


    /**
     * Fixes a calendar sync conflict with edited flight by disabling calendar sync until after both flights.
     * @param f: Flight to check against FlightRepository.undoFlight. Will throw error if f == null.
     * Call this function from an observer of [FlightRepository.savedFlight]
     */
    fun fixCalendarSyncConflictIfNeeded(f: Flight?) {
        if (f == null) return // no flight, no conflict
        checkFlightConflictingWithCalendarSync(f).nullIfZero()?.let {
            disableCalendarImportUntil(it)
        }
    }


    /*********************************************************************************************
     * Functions related to synchronization:
     *********************************************************************************************/

    /**
     * This will synch time with server and launch repository update functions (which can decide for themselves if it is necessary)
     */
    fun notifyActivityResumed() {
        GeneralRepository.synchTimeWithServer()
        flightRepository.syncIfNeeded()
        airportRepository.getAirportsIfNeeded()
        aircraftRepository.checkIfAircraftTypesUpToDate()
    }

    fun deleteAndDisableCalendarImportUntillAfterThisFlight(flightId: Int) {
        viewModelScope.launch {
            flightRepository.fetchFlightByID(flightId)?.let { flight ->
                disableCalendarImportUntil(flight.timeIn)
                flightRepository.delete(flight)
                feedback(MainActivityEvents.DELETED_FLIGHT)
            } ?: feedback(MainActivityEvents.FLIGHT_NOT_FOUND)
        }
    }

    fun disableCalendarImport() {
        Preferences.getFlightsFromCalendar = false
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
        query = it.toUpperCase(Locale.ROOT)
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
            data?.lastPathSegment?.let {
                Log.d("Uri", "lastPathSegment: $it")

                //TODO needs sanity check
                val loginPass = it.split(":").let { lp ->
                    lp.first() to lp.last()
                }
                viewModelScope.launch {
                    val result = UserManagement.loginFromLink(loginPass)
                    Log.d("LinkLogin", "result: $result")
                    flightRepository.syncIfNeeded()
                }


            }

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

