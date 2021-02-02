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
import kotlinx.coroutines.launch
import nl.joozd.joozdlogcommon.AircraftType
import nl.joozd.logbookapp.data.dataclasses.Aircraft
import nl.joozd.logbookapp.extensions.in_ignoreCase
import nl.joozd.logbookapp.model.viewmodels.JoozdlogDialogViewModelWithWorkingFlight
import java.util.*


/**
 * Viewmodel for [AircraftPicker] and [SimTypePicker] fragments
 */
class AircraftPickerViewModel: JoozdlogDialogViewModelWithWorkingFlight(){
    private val undoAircraft = workingFlight.aircraft.value
    private val _typesSearchString = MutableLiveData("")
    private val typesSearchString
        get() = _typesSearchString.value ?: ""

    private val _aircraftTypes = MediatorLiveData<List<AircraftType>>().apply {
        addSource(aircraftRepository.aircraftTypesLiveData){
            // Create a list of names of all known aircraft matching [typesSearchString]
            value = it.filter{type -> typesSearchString in_ignoreCase type.name}
        }
        addSource(_typesSearchString){
            value = (aircraftRepository.aircraftTypes ?: emptyList()).filter{typesSearchString in_ignoreCase it.name}
        }
    }


    // Active aircraft in [workingFligght] or a placeholder [Aircraft] while workingFlight is loading data
    val selectedAircraft = workingFlight.aircraft.map { it ?: Aircraft("...")}

    val aircraftTypes: LiveData<List<AircraftType>>
        get() = _aircraftTypes

    val knownRegistrations = aircraftRepository.aircraftListLiveData.map{ it.map{ac -> ac.registration}}

    private var mAircraft: Aircraft
        get() = selectedAircraft.value!!
        set(newAircraft){
            workingFlight.setAircraft(newAircraft)
        }


    /**
     * Update selected aircaft's registration, type or source
     */
    private fun updatedSelectedAircraft(registration: String? = null, type: AircraftType? = null, source: Int? = null) {
        mAircraft = mAircraft.copy(
                registration = registration ?: mAircraft.registration,
                type = type ?: mAircraft.type,
                source = source ?: mAircraft.source)
    }

    val selectedAircraftType: LiveData<AircraftType?> = Transformations.map(selectedAircraft) { it.type }

    fun selectAircraftTypeByString(typeString: String, shortString: Boolean = false){
        viewModelScope.launch {
            val newType = if (shortString) aircraftRepository.getAircraftTypeByShortName(typeString) else aircraftRepository.getAircraftType(typeString)
            updatedSelectedAircraft(type = newType)
        }

        //TODO set AircraftRegistrationWithTypeData
    }

    fun selectAircraftType(type: AircraftType){
        viewModelScope.launch {
            updatedSelectedAircraft(type = type)
        }

        //TODO set AircraftRegistrationWithTypeData
    }

    /**
     * Called when user is typing in registrationField in UI.
     * workingFlight will look for aircraft to match registration or will create one with null type
     */
    fun updateRegistration(reg: String){
        workingFlight.setAircraft(registration = reg)
    }

    /**
     * Save aircraft to repository
     */
    fun saveAircraftToRepository() {
        aircraftRepository.saveAircraft(mAircraft)
    }

    fun updateSearchString(query: String){
        _typesSearchString.value = query.toUpperCase(Locale.ROOT)
    }

    /**
     * Revert aircraft to what it was when this viewModel was created
     */
    override fun undo() {
        workingFlight.setAircraft(undoAircraft)
    }

}