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

package nl.joozd.logbookapp.model.viewmodels.dialogs.airportPicker

import kotlinx.coroutines.*
import kotlinx.coroutines.flow.*
import nl.joozd.logbookapp.data.dataclasses.Airport
import nl.joozd.logbookapp.data.repository.airportrepository.AirportRepository
import nl.joozd.logbookapp.model.viewmodels.JoozdlogDialogViewModel
import nl.joozd.logbookapp.model.viewmodels.dialogs.airportPicker.AirportPickerConstants.MAX_RESULT_SIZE
import nl.joozd.logbookapp.model.workingFlight.FlightEditor

@ExperimentalCoroutinesApi
abstract class AirportPickerViewModel: JoozdlogDialogViewModel() {
    private val currentQueryFlow = MutableStateFlow("")

    //The FlightEditor we use. Can be assumed to be not null.
    protected val editor = FlightEditor.instance
    /**
     * Override this with getter and setter that changes orig or dest in target
     */
    abstract var airport: Airport

    /**
     * Flow that emits the currently active airport
     */
    abstract val airportFlow: Flow<Airport>

    /**
     * Pick an airport and set that as Airport
     */
    fun pickAirport(pickedAirport: Airport){
        airport = pickedAirport
    }

    /**
     * touch this in Fragment onCreateView() to initialize it
     */
    val undoAirport by lazy { airport }

    /**
     * Set a custom value as Icao Identifier for in logbook.
     */
    fun setCustomAirport(identifier: String){
        airport = Airport(ident = identifier)
    }

    val pickedAirportFlow: Flow<Airport> get() = airportFlow

    val airportsToIsPickedListFlow: Flow<List<Pair<Airport, Boolean>>> =
        combine(AirportRepository.instance.airportsFlow(), currentQueryFlow, pickedAirportFlow) { airports, query, pickedAirport ->
            AirportsQueryPicked(airports, query, pickedAirport)
        }.flatMapLatest {
            makeAirportSearcherFlow(it)
        }.conflate()

    fun updateSearch(query: String){
        currentQueryFlow.update { query }
    }

    fun undo(){
        airport = undoAirport
    }


    private fun makeAirportSearcherFlow(aqp: AirportsQueryPicked): Flow<List<Pair<Airport, Boolean>>> = flow {
        val query = aqp.query
        val airports = aqp.airports
        val picked = aqp.picked
        if (query.isBlank()) {
            emit(airportsToIsPicked(airports, picked))
        } else {
            var result = airports.filter { it.iata_code.contains(query, ignoreCase = true) }
            emit(airportsToIsPicked(result, picked))
            if (result.size > MAX_RESULT_SIZE) return@flow

            result = result + airports.filter { it !in result && it.ident.contains(query, ignoreCase = true) }
            emit(airportsToIsPicked(result, picked))
            if (result.size > MAX_RESULT_SIZE) return@flow

            result = result + airports.filter { it !in result && it.municipality.contains(query, ignoreCase = true) }
            emit(airportsToIsPicked(result, picked))
            if (result.size > MAX_RESULT_SIZE) return@flow

            result = result + airports.filter { it !in result && it.name.contains(query, ignoreCase = true) }
            emit(airportsToIsPicked(result, picked))
        }
    }

    private fun airportsToIsPicked(
        airports: List<Airport>,
        picked: Airport
    ) = airports.map { it to (it == picked) }

    private class AirportsQueryPicked(val airports: List<Airport>, val query: String, val picked: Airport)

    companion object{


    }
}
