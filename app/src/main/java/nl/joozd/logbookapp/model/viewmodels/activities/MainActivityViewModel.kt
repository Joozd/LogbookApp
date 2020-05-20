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

package nl.joozd.logbookapp.model.viewmodels.activities



import android.util.Log
import androidx.lifecycle.*
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import nl.joozd.logbookapp.data.repository.GeneralRepository
import nl.joozd.logbookapp.data.sharedPrefs.Preferences
import nl.joozd.logbookapp.model.dataclasses.DisplayFlight
import nl.joozd.logbookapp.model.dataclasses.Flight
import nl.joozd.logbookapp.model.helpers.FeedbackEvents.MainActivityEvents
import nl.joozd.logbookapp.model.viewmodels.JoozdlogActivityViewModel
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
        get() = searchFlights(rawFlights).map{DisplayFlight.of(it,icaoIataMap, Preferences.useIataAirports) }.also{setSearchFieldHint(it.size)}

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


    /**
     * Menu functions
     */
    fun menuSelectedDoSomething(){
        Log.d("useIataAirports", "before: ${Preferences.useIataAirports}")
        Preferences.useIataAirports = !Preferences.useIataAirports
        Log.d("useIataAirports", "after: ${Preferences.useIataAirports}")
        Log.d("displayFlightsList.value", "after: ${displayFlightsList.value}")
    }

    fun menuSelectedRebuild(){
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

    fun menuSelectedTotalTimes(){
        feedback(MainActivityEvents.NOT_IMPLEMENTED)
    }

    fun menuSelectedBalanceForward(){
        feedback(MainActivityEvents.NOT_IMPLEMENTED)
    }

    fun menuSelectedSearch(){
        toggleSearchField()
    }

    fun menuSelectedEditAircraft() {
        viewModelScope.launch {
            if (flightRepository.updateNamesDivider())
                feedback(MainActivityEvents.DONE)
            else feedback(MainActivityEvents.ERROR)
        }
    }

            /*
            launch {
                progressBarField?.let { pbf ->
                    val progBar = JoozdlogProgressBar(
                        pbf
                    ).apply {
                        backgroundColor = getColorFromAttr(android.R.attr.colorPrimary)
                        text = getString(R.string.loadingAirports)
                    }.show()
                    Cloud.getAircraftTypes{progBar.progress = it}?.let { result ->
                        launch(NonCancellable) {
                            aircraftRepository.saveAircraftTypes(result)
                            Log.d(this::class.simpleName,"repo now has ${aircraftRepository.liveAircraftTypes.value?.size} types")
                        }
                    }
                    progBar.remove()
                }
            }
             */

    fun menuSelectedExportPDF(){
        feedback(MainActivityEvents.NOT_IMPLEMENTED)
    }

    /**
     * Observable data:
     */

    private val _displayFlightsList2= MediatorLiveData<List<DisplayFlight>>()
    init{
        _displayFlightsList2.addSource(flightRepository.liveFlights) {
            _displayFlightsList2.value = flightsList ?: _displayFlightsList2.value
        }
        _displayFlightsList2.addSource(airportRepository.icaoIataMap) {
            _displayFlightsList2.value = flightsList
        }
        _displayFlightsList2.addSource(airportRepository.useIataAirports){
            _displayFlightsList2.value = flightsList
        }
        _displayFlightsList2.addSource(searchStringLiveData){
            _displayFlightsList2.value = flightsList
        }
        _displayFlightsList2.addSource(searchSpinnerSelection){
            _displayFlightsList2.value = flightsList
        }
    }
    val displayFlightsList: LiveData<List<DisplayFlight>>
        get() = _displayFlightsList2

    val searchFieldHint
        get() = _searchFieldHint
    private fun setSearchFieldHint(it: Int){
        _searchFieldHint.value = if (it == rawFlights.size) null else "$it flights found"
    }

    /**
     * Handler for clickety thingies
     */
    fun showFlight(flightID: Int){
        viewModelScope.launch(Dispatchers.IO) {
            workingFlightRepository.fetchFlightByIdToWorkingFlight(flightID)
            feedback(MainActivityEvents.SHOW_FLIGHT)
        }

    }

    fun addFlight(){
        viewModelScope.launch(Dispatchers.IO) {
            workingFlightRepository.createNewWorkingFlight()
            feedback(MainActivityEvents.SHOW_FLIGHT)
        }
    }
    fun saveWorkingFlight() {
        workingFlightRepository.saveWorkingFlight()
        feedback(MainActivityEvents.FLIGHT_SAVED)
    }

    fun undoSaveWorkingFlight() = workingFlightRepository.undoSaveWorkingFlight()

    /**
     * Does one of three things:
     * - if the flight with flightID [id] is:
     *  * nonexistent:      feedback(FLIGHT_NOT_FOUND)
     *  * planned:          Delete flight
     *  * Not planned:      feedback(TRYING_TO_DELETE_COMPLETED_FLIGHT) with its id in ExtraData as "ID"
     */
    fun deleteFlight(id: Int){
        viewModelScope.launch {
            withContext (Dispatchers.IO) { flightRepository.fetchFlightByID(id)}.let{
                when(it?.isPlanned){
                    null -> feedback(MainActivityEvents.FLIGHT_NOT_FOUND)
                    true -> {
                        flightRepository.delete(it)
                        feedback(MainActivityEvents.DELETED_FLIGHT)
                    }
                    false -> feedback(MainActivityEvents.TRYING_TO_DELETE_COMPLETED_FLIGHT).apply{
                        extraData.putInt("ID", id)
                    }
                }
            }
        }
    }

    fun deleteNotPlannedFlight(id: Int) = flightRepository.delete(id)


    /*********************************************************************************************
     * Functions related to synchronization:
     *********************************************************************************************/

    /**
     * This will synch time with server and launch repository update functions (which can decide for themselves if it is necessary)
     */
    fun notifyActivityResumed(){
        GeneralRepository.synchTimeWithServer()
        flightRepository.syncIfNeeded()
        airportRepository.getAirportsIfNeeded()
        aircraftRepository.checkIfAircraftTypesUpToDate()
    }

    /*********************************************************************************************
     * Functions related to searching:
     *********************************************************************************************/

    fun closeSearchField(){
        feedback(MainActivityEvents.CLOSE_SEARCH_FIELD)
        query = ""
        searchFieldOpen = false
    }

    private fun toggleSearchField(){
        if (searchFieldOpen)
            closeSearchField()
        else {
            feedback(MainActivityEvents.OPEN_SEARCH_FIELD)
            searchFieldOpen = true
        }
    }

    /**
     * Will search flights, return immediate results but if needed also update [_displayFlightsList2] async with more detailed data
     */
    private fun searchFlights(fff: List<Flight>?): List<Flight> {
        if (fff == null) return emptyList()
        if (!searchFieldOpen) return fff
        return when(searchType){
            ALL -> searchAll(fff)
            AIRPORTS -> searchAirports(fff)
            AIRCRAFT -> searchAircraft(fff)
            NAMES -> searchNames(fff)
            else -> fff
        }
    }

    private fun searchAll(fff: List<Flight>) = fff.filter{
        query in it.name.toUpperCase(Locale.ROOT)
                || query in it.name2.toUpperCase(Locale.ROOT)
                || query in it.registration.toUpperCase(Locale.ROOT)
                || query in it.orig.toUpperCase(Locale.ROOT)
                || query in it.dest.toUpperCase(Locale.ROOT)
                || query in icaoIataMap[it.orig]?.toUpperCase(Locale.ROOT) ?: ""
                || query in icaoIataMap[it.dest]?.toUpperCase(Locale.ROOT) ?: ""}

    private fun searchAirports(fff: List<Flight>) = fff.filter{
        query in it.orig.toUpperCase(Locale.ROOT)
                || query in it.dest.toUpperCase(Locale.ROOT)
                || query in icaoIataMap[it.orig]?.toUpperCase(Locale.ROOT) ?: ""
                || query in icaoIataMap[it.dest]?.toUpperCase(Locale.ROOT) ?: ""}.also{
        viewModelScope.launch {
            // TODO make with async update from [airportRepository]
        }
    }


    private fun searchAircraft(fff: List<Flight>) = fff.filter{
        query in it.registration.toUpperCase(Locale.ROOT) }.also{
        viewModelScope.launch {
            //TODO make with async update from [aircraftRepository]
        }
    }

    private fun searchNames(fff: List<Flight>) = fff.filter{
        query in it.name.toUpperCase(Locale.ROOT)
                || query in it.name2.toUpperCase(Locale.ROOT)
    }

    fun setSearchString(it: String){
        query = it.toUpperCase(Locale.ROOT)
    }

    fun setSpinnerSelection(it: Int)
    {
        searchType = it
    }



    /*********************************************************************************************
     * Livedata related to synchronization:
     *********************************************************************************************/

    val airportSyncProgress: LiveData<Int>
        get() = airportRepository.airportSyncProgress

    val flightSyncProgress: LiveData<Int>
        get() = flightRepository.syncProgress

    val flightOpenEvents = workingFlightRepository.flightOpenEvents

    /**
     * Internal functions:
     */
    companion object{
        const val ALL = 0
        const val AIRPORTS = 1
        const val AIRCRAFT = 2
        const val NAMES = 3
    }

}