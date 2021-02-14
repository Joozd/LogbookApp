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

package nl.joozd.logbookapp.model.viewmodels.dialogs.airportPicker

import androidx.lifecycle.*
import androidx.lifecycle.Transformations.distinctUntilChanged
import kotlinx.coroutines.*
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.collect
import kotlinx.coroutines.flow.conflate
import nl.joozd.logbookapp.data.dataclasses.Airport
import nl.joozd.logbookapp.extensions.nonNullLiveData
import nl.joozd.logbookapp.model.viewmodels.JoozdlogDialogViewModelWithWorkingFlight

//TODO make sure list gets filled straight away?
//TODO sort airportsList based on ICAO/IATA prefs?

@ExperimentalCoroutinesApi
abstract class AirportPickerViewModel: JoozdlogDialogViewModelWithWorkingFlight(){
    /**
     * MediatorLiveData for kepping track of which airport is picked.
     * Add sources in implementing classes
     */
    protected val pickedAirportMediator = MediatorLiveData<Airport>()

    /**
     * Pick an airport and set that as Airport
     */
    abstract fun pickAirport(airport: Airport)

    /**
     * Set a custom value as Icao Identifier for in logbook.
     */
    abstract fun setCustomAirport(airport: String)

    /**
     * The airport that is set in [workingFlight] when viewmodel is initialized
     */
    protected abstract val initialAirport: Airport?

    private var currentSearchJob: Job = Job()

    private val _airportsList = MutableLiveData<List<Airport>>()
    val airportsList: LiveData<List<Airport>> = nonNullLiveData(distinctUntilChanged(_airportsList))
    init{
        _airportsList.value = airportRepository.liveAirports.value
    }

    val pickedAirport: LiveData<Airport>
        get() = pickedAirportMediator


    /**
     * Do some search magic. Needs at least [MIN_CHARACTERS] characters for performance reasons
     * TODO this doesn't cancel correctly
     */
    fun updateSearch(query: String){
        if (query.length >= MIN_CHARACTERS) {
            currentSearchJob.cancel()
            currentSearchJob = viewModelScope.launch {
                collectAirports(airportRepository.getQueryFlow(query))
            }
        }
        //feedback(NOT_IMPLEMENTED)
    }

    private suspend fun collectAirports(flow: Flow<List<Airport>>){
        flow.conflate().collect {
            _airportsList.value = it
            delay(200)
        }
    }

    companion object{
        const val MIN_CHARACTERS = 3
    }
}
