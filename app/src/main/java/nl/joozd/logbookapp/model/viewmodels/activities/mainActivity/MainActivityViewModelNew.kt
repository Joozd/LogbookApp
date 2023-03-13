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
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import nl.joozd.logbookapp.data.calendar.CalendarControl
import nl.joozd.logbookapp.data.repository.aircraftrepository.AircraftDataCache
import nl.joozd.logbookapp.data.repository.aircraftrepository.AircraftRepository
import nl.joozd.logbookapp.data.repository.airportrepository.AirportDataCache
import nl.joozd.logbookapp.data.repository.airportrepository.AirportRepository
import nl.joozd.logbookapp.data.repository.flightRepository.FlightRepositoryWithSpecializedFunctions
import nl.joozd.logbookapp.data.repository.flightRepository.FlightRepositoryWithUndo
import nl.joozd.logbookapp.data.repository.helpers.makeReturnFlight
import nl.joozd.logbookapp.model.ModelFlight
import nl.joozd.logbookapp.model.dataclasses.Flight
import nl.joozd.logbookapp.model.helpers.filterByQuery
import nl.joozd.logbookapp.model.viewmodels.JoozdlogViewModel
import nl.joozd.logbookapp.model.workingFlight.FlightEditor
import nl.joozd.logbookapp.utils.CastFlowToMutableFlowShortcut
import nl.joozd.logbookapp.utils.DispatcherProvider

class MainActivityViewModelNew: JoozdlogViewModel() {
    private val flightRepository = FlightRepositoryWithUndo.instance
    private val aircraftRepository = AircraftRepository.instance
    private val airportRepository = AirportRepository.instance

    private val searchQueryFlow: Flow<String> = MutableStateFlow("")
    private var searchQuery: String by CastFlowToMutableFlowShortcut(searchQueryFlow)

    private val searchTypeFlow: Flow<Int> = MutableStateFlow(SEARCH_ALL)
    private var searchType: Int by CastFlowToMutableFlowShortcut(searchTypeFlow)

    val searchFieldOpenFlow: Flow<Boolean> = MutableStateFlow(false)
    private var searchFieldOpen: Boolean by CastFlowToMutableFlowShortcut(searchFieldOpenFlow)

    val flightEditorFlow = FlightEditor.instanceFlow

    private val allFlightsFlow = flightRepository.allFlightsFlow()
    val foundFlightsFlow: Flow<List<ModelFlight>> = makeFlightsFlowCombiner()
    val amountOfFlightsFlow: Flow<Int> = allFlightsFlow.map { it.count{ f-> !f.isPlanned } }

    val undoRedoStatusChangedFlow = makeUndoRedoStatusChangedFlow()

    val undoAvailable get() = flightRepository.undoAvailable
    val redoAvailable get() = flightRepository.redoAvailable


    fun deleteFlight(flight: ModelFlight){
        viewModelScope.launch{
            val f = flight.toFlight()
            flightRepository.delete(f)
            CalendarControl.handleManualDelete(f)   //send the deleted flight to CalendarControl to check if Calendar sync needs to be postponed.
        }
    }

    /**
     * This will create a new FlightEditor.
     * [flightEditorFlow] will emit after a new FlightEditor is made.
     */
    fun showEditFlightDialog(flight: ModelFlight){
        FlightEditor.setFromFlight(flight)
    }

    fun newFlight(){
        viewModelScope.launch {
            FlightRepositoryWithSpecializedFunctions.instance.getMostRecentCompletedFlight()
                ?.makeReturnFlight()?.let{ f ->
                    FlightEditor.setFromFlight(f)
                } ?: FlightEditor.setNewflight()
        }
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

    fun undo(){
        viewModelScope.launch { flightRepository.undo() }
    }

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
        allFlightsFlow,
        aircraftRepository.aircraftDataCacheFlow(),
        airportRepository.airportDataCacheFlow(),
        searchQueryFlow,
        searchTypeFlow
    ){ flights, aircraftData, airportsData, query, searchType ->
        flights.toModelFlights(airportsData, aircraftData)
            .filterByQuery(query, searchType)
    }

    /**
     * Emites a pair of (undoAvail, redoAvail) so it will emit always when either changes.
     */
    private fun makeUndoRedoStatusChangedFlow() =
        combine(flightRepository.undoAvailableFlow, flightRepository.redoAvailableFlow){
            u, r -> Pair(u,r)
        }

    private suspend fun List<Flight>.toModelFlights(
        airportsData: AirportDataCache,
        aircraftData: AircraftDataCache
    ) = withContext (DispatcherProvider.default()) {
        this@toModelFlights.map { ModelFlight.ofFlightAndDataCaches(it, airportsData, aircraftData) } }


    companion object {
        const val SEARCH_ALL = 0
        const val SEARCH_AIRPORTS = 1
        const val SEARCH_AIRCRAFT = 2
        const val SEARCH_NAMES = 3
        const val SEARCH_FLIGHTNUMBER = 4
    }
}