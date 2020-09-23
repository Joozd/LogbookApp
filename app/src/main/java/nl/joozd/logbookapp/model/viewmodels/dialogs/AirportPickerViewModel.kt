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

package nl.joozd.logbookapp.model.viewmodels.dialogs

import androidx.lifecycle.*
import androidx.lifecycle.Transformations.distinctUntilChanged
import kotlinx.coroutines.*
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.collect
import kotlinx.coroutines.flow.conflate
import nl.joozd.logbookapp.data.dataclasses.Airport
import nl.joozd.logbookapp.model.viewmodels.JoozdlogDialogViewModel
import nl.joozd.logbookapp.R
import nl.joozd.logbookapp.model.feedbackEvents.FeedbackEvents.AirportPickerEvents

//TODO make sure list gets filled straight away?
//TODO sort airportsList based on ICAO/IATA prefs?

@ExperimentalCoroutinesApi
class AirportPickerViewModel: JoozdlogDialogViewModel(){
    private var currentSearchJob: Job = Job()

    /**
     * this MUST be set BEFORE onCreateView in Fragment so feedback event will be observed
     * also, feedbackEvent must be observed. If [_workingOnOrig] == null, things won't work.
     */
    private var _workingOnOrig: Boolean? = null
    val workingOnOrig: Boolean?
        get() = _workingOnOrig
    fun setWorkingOnOrig(orig: Boolean?){
        _workingOnOrig = orig
        if (orig == null) feedback(AirportPickerEvents.ORIG_OR_DEST_NOT_SELECTED)
        else {
            workingFlight?.let {f ->
                updateSearch((if (orig) f.orig else f.dest) ?: "")
                viewModelScope.launch {
                    _pickedAirport.value =
                        airportRepository.getAirportByIcaoIdentOrNull(if (_workingOnOrig == true) f.orig else f.dest)
                            ?: Airport(    -1,
                                ident = if (_workingOnOrig == true) f.orig else f.dest,
                                municipality = context.getString(R.string.unknown),
                                name = context.getString(R.string.unknown_airport)
                            ).also{
                                feedback(AirportPickerEvents.CUSTOM_AIRPORT_NOT_EDITED)
                            }
                }
            }
        }
    }

    fun checkWorkingOnOrigSet(){
        if (_workingOnOrig == null) feedback(AirportPickerEvents.ORIG_OR_DEST_NOT_SELECTED)
    }

    private val _airportsList = MutableLiveData<List<Airport>>()
    val airportsList: LiveData<List<Airport>> = distinctUntilChanged(_airportsList)
    init{
        _airportsList.value = airportRepository.liveAirports.value
    }

    private val _pickedAirport = MediatorLiveData<Airport>()
    init{
        _pickedAirport.addSource(flight) {f ->
            viewModelScope.launch {
                _pickedAirport.value =
                    airportRepository.getAirportByIcaoIdentOrNull(if (_workingOnOrig == true) f.orig else f.dest)
                        ?: Airport(    -1,
                            ident = if (_workingOnOrig == true) f.orig else f.dest,
                            municipality = context.getString(R.string.unknown),
                            name = context.getString(R.string.unknown_airport)
                            ).also{
                            feedback(AirportPickerEvents.CUSTOM_AIRPORT_NOT_EDITED)
                        }
            }
        }
    }

    val pickedAirport: LiveData<Airport>
        get() = _pickedAirport

    fun pickAirport(airport: Airport) {
        workingFlight?.let {
            workingFlight = when (_workingOnOrig) {
                null -> {
                    feedback(AirportPickerEvents.ORIG_OR_DEST_NOT_SELECTED)
                    return
                }
                true -> it.copy(orig = airport.ident)
                false -> it.copy(dest = airport.ident)
            }
        }
    }


    fun setCustomAirport(airport: String){
        workingFlight?.let {
            workingFlight = when (_workingOnOrig) {
                null -> {
                    feedback(AirportPickerEvents.ORIG_OR_DEST_NOT_SELECTED)
                    return
                }
                true -> it.copy(orig = airport)
                false -> it.copy(dest = airport)
            }
        }
    }

    fun updateSearch(query: String){
        currentSearchJob.cancel()
        currentSearchJob = viewModelScope.launch{
            collectAirports(airportRepository.getQueryFlow(query))
        }
        //feedback(NOT_IMPLEMENTED)
    }

    private suspend fun collectAirports(flow: Flow<List<Airport>>){
        flow.conflate().collect {
            _airportsList.value = it
            delay(200)
        }
    }
}
