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

package nl.joozd.logbookapp.model.viewmodels.activities.mainActivity

import androidx.lifecycle.viewModelScope
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.launch
import nl.joozd.logbookapp.data.repository.aircraftrepository.AircraftRepository
import nl.joozd.logbookapp.data.repository.airportrepository.AirportDataCache
import nl.joozd.logbookapp.data.repository.airportrepository.AirportRepository
import nl.joozd.logbookapp.data.repository.flightRepository.FlightRepositoryWithUndo
import nl.joozd.logbookapp.model.ModelFlight
import nl.joozd.logbookapp.model.dataclasses.Flight
import nl.joozd.logbookapp.model.helpers.ModelFlightsSearcher
import nl.joozd.logbookapp.model.viewmodels.JoozdlogViewModel
import nl.joozd.logbookapp.model.workingFlight.FlightEditor
import nl.joozd.logbookapp.utils.CastFlowToMutableFlowShortcut

//TODO this is still WIP
class MainActivityViewModelNew(): JoozdlogViewModel() {
    private val flightRepository = FlightRepositoryWithUndo.instance
    private val aircraftRepository = AircraftRepository.instance
    private val airportRepository = AirportRepository.instance
    private val modelFlightsSearcher = ModelFlightsSearcher()

    val flightsFlow: Flow<List<ModelFlight>> = makeFlightsFlowCombiner()

    private var airportDataCache: AirportDataCache = AirportDataCache.empty().apply{
        // This will keep airportDataCache up-to-date, even when app is not in the foreground.
        // Only time that happens thouhg is after and Airport DB update, which happens so rarely
        // that this doesn't warrant putting in back-and-forth lifecycle checking with Activity.
        viewModelScope.launch{
            AirportRepository.instance.airportDataCacheFlow().collect{
                airportDataCache = it
            }
        }
    }

    val flightEditorFlow = FlightEditor.instanceFlow

    val searchFieldOpenFlow: Flow<Boolean> = MutableStateFlow(false)
    private val searchQueryFlow: Flow<String> = MutableStateFlow("")
    private val searchTypeFlow: Flow<Int> = MutableStateFlow(SEARCH_ALL)

    private var searchFieldOpen: Boolean by CastFlowToMutableFlowShortcut(searchFieldOpenFlow)
    private var searchQuery: String by CastFlowToMutableFlowShortcut(searchQueryFlow)
    private var searchType: Int by CastFlowToMutableFlowShortcut(searchTypeFlow)



    fun deleteFlight(flight: Flight){
        viewModelScope.launch{
            flightRepository.delete(flight)
        }
    }

    /**
     * This will create a new FlightEditor.
     * [flightEditorFlow] will emit after a new FlightEditor is made.
     */
    fun showEditFlightDialog(flight: Flight){
        viewModelScope.launch {
            FlightEditor.setFromFlight(flight)
        }
    }

    fun menuSelectedAddFlight(){
        FlightEditor.setNewflight()
    }

    fun menuSelectedSearch(): Boolean {
        toggleSearchField()
        return true
    }

    private fun toggleSearchField() {
        if (searchFieldOpen) {
            searchQuery = ""
            searchFieldOpen = false
        }
        else {
            searchFieldOpen = true
        }
    }

    //returns true so we can use it straight from Menu in Activity
    fun undo(){
        viewModelScope.launch { flightRepository.undo() }
    }

    //returns true so we can use it straight from Menu in Activity
    fun redo(){
        viewModelScope.launch { flightRepository.redo() }
    }

    fun pickSearchType(pickedSearchType: Int){
        searchType = pickedSearchType
    }

    fun updateQuery(query: String){
        searchQuery = query
    }

    private fun makeFlightsFlowCombiner() = combine(
        flightRepository.getAllFlightsFlow(),
        aircraftRepository.aircraftDataCacheFlow(),
        airportRepository.airportDataCacheFlow(),
        searchQueryFlow,
        searchTypeFlow
    ){ flights, aircraftData, airportsData, query, searchType ->
        val modelFlights = flights.map{ ModelFlight.ofFlightAndDataCaches(it, airportsData, aircraftData) }
        modelFlightsSearcher.searchFlights(modelFlights, query, searchType)
    }



    private fun searchAirports(fff: List<ModelFlight>, query: String) = fff /*.filter {
        query in it.orig.uppercase(Locale.ROOT)
                || query in it.dest.uppercase(Locale.ROOT)
                || query in airportDataCache.icaoToIata(it.orig)?.uppercase(Locale.ROOT) ?: ""
                || query in airportDataCache.icaoToIata(it.dest)?.uppercase(Locale.ROOT) ?: ""
    }
    */




    companion object {
        const val SEARCH_ALL = 0
        const val SEARCH_AIRPORTS = 1
        const val SEARCH_AIRCRAFT = 2
        const val SEARCH_NAMES = 3
        const val SEARCH_FLIGHTNUMBER = 4
    }
}