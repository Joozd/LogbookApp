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

import android.util.Log
import androidx.lifecycle.*
import kotlinx.coroutines.NonCancellable
import kotlinx.coroutines.launch
import nl.joozd.joozdlogcommon.AircraftType
import nl.joozd.logbookapp.data.dataclasses.Aircraft
import nl.joozd.logbookapp.model.dataclasses.Flight
import nl.joozd.logbookapp.model.viewmodels.JoozdlogDialogViewModel


/**
 * Viewmodel for [AircraftPicker] and [SimTypePicker] fragments
 */
class AircraftPickerViewModel: JoozdlogDialogViewModel(){
    private val _typesSearchString = MutableLiveData<String>()
    private val typesSearchString
        get() = _typesSearchString.value ?: ""

    val aircraftTypes = MediatorLiveData<List<String>>()
    init{
        aircraftTypes.addSource(aircraftRepository.liveAircraftTypes){
            aircraftTypes.value = it.map{ac -> ac.name}.filter{typesSearchString in it}
        }
        aircraftTypes.addSource(_typesSearchString){
            aircraftTypes.value = (aircraftRepository.liveAircraftTypes.value ?: emptyList()).map{ac -> ac.name}.filter{typesSearchString in it}
        }
    }

    private val _selectedAircraft = MutableLiveData(
        Aircraft(
            "XX-XXX"
        )
    )
    val selectedAircraft
        get ()= _selectedAircraft
    private fun updatedSelectedAircraft(
        registration: String? = null,
        type: AircraftType? = null,
        source: Int? = null
    ) {
        Log.d(this::class.simpleName, "updatedSelectedAircraft($registration, $type, $source)")
        _selectedAircraft.value = _selectedAircraft.value?.let {
            it.copy(
                registration = registration ?: it.registration,
                type = type ?: it.type,
                source = source ?: it.source
            )
        } ?: Aircraft(
            registration ?: "XX-XXX",
            type,
            source ?: Aircraft.NONE
        )
    }
    private fun setSelectedAircraft(aircraft: Aircraft){
        Log.d(this::class.simpleName, "setSelectedAircraft($aircraft)")
        _selectedAircraft.value = aircraft
    }
    private fun setSelectedAircraftFromFlight(flight: Flight?){
        if (flight == null) return
        viewModelScope.launch {
            _selectedAircraft.value = aircraftRepository.getAircraftFromRegistration(flight.registration)
                ?: Aircraft(
                    flight.registration,
                    aircraftRepository.getAircraftTypeByShortName(flight.aircraft),
                    Aircraft.FLIGHT
                )
        }
    }

    val selectedAircraftString: LiveData<String> = Transformations.map(_selectedAircraft) { it.type?.name ?: "UNKNOWN"}

    fun selectAircraftTypeByString(typeString: String){
        viewModelScope.launch {
            val newType = aircraftRepository.getAircraftType(typeString)
            updatedSelectedAircraft(type = newType)
        }

        //TODO set AircraftRegistrationWithTypeData
    }

    val registration: LiveData<String> = Transformations.map(selectedAircraft) { it.registration }
    fun updateRegistration(reg: String) = viewModelScope.launch {
        val previousType = selectedAircraft.value?.type
        setSelectedAircraft(
            aircraftRepository.getAircraftFromRegistration(reg)
                ?: Aircraft(
                    reg,
                    previousType,
                    Aircraft.PREVIOUS
                )
        )
        // search for known type with that reg, if null, make new aircraft with this reg and previous known type (if any)
        // set found type as [_selectedAircraftString] and [_selectedAircraftType]
    }

    fun start(){
        setSelectedAircraftFromFlight(workingFlight)
    }

    fun saveAircraft() {
        selectedAircraft.value?.let{ac ->
            if (ac.type != null){
                viewModelScope.launch (NonCancellable) {
                    aircraftRepository.saveAircraft(ac)
                }
            }
        }
        workingFlight?.let {
            Log.d(this::class.simpleName, "selectedAircraft = ${selectedAircraft.value}")
            workingFlight = it.copy(
                registration = selectedAircraft.value?.registration ?: "".also{Log.d(this::class.simpleName, "selectedAircraft.value?.registration is null")},
                aircraft = selectedAircraft.value?.type?.shortName ?: ""
            ).also{Log.d(this@AircraftPickerViewModel::class.simpleName,"saved $it")}
            //TODO: Save reg+type to repo
        }
    }

    fun saveTypeOnly(){
        workingFlight?.let{
            workingFlight = it.copy(aircraft = selectedAircraft.value?.type?.shortName ?: "")
        }
    }

    fun updateSearchString(query: String){
        _typesSearchString.value = query
    }

}