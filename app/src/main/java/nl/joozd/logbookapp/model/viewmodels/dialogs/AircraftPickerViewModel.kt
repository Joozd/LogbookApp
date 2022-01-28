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

package nl.joozd.logbookapp.model.viewmodels.dialogs

import androidx.lifecycle.*
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.launch
import nl.joozd.joozdlogcommon.AircraftType
import nl.joozd.logbookapp.data.dataclasses.Aircraft
import nl.joozd.logbookapp.extensions.inIgnoreCase
import nl.joozd.logbookapp.model.viewmodels.JoozdlogDialogViewModelWithWorkingFlight
import java.util.*


/**
 * ViewModel for AircraftPicker and SimTypePicker fragments
 */
class AircraftPickerViewModel: JoozdlogDialogViewModelWithWorkingFlight(){
    private val _typesSearchStringFlow = MutableStateFlow("")

    private val aircraftTypesFlow: Flow<List<AircraftType>> =
        println("AAAAA").let{
        combine(aircraftRepository.aircraftTypesFlow(), _typesSearchStringFlow) { types, query ->
            types.filter { type -> query inIgnoreCase type.name }
        }
    }

    // Active aircraft in [workingFligght] or a placeholder [Aircraft] while workingFlight is loading data
    val selectedAircraft = workingFlight.aircraftFlow.asLiveData().map { it ?: Aircraft("...")}

    //TODO make this be collected as flow instead of as liveData
    val aircraftTypes: LiveData<List<AircraftType>>
        get() = aircraftTypesFlow.asLiveData()

    val knownRegistrationsLiveData =
        aircraftRepository.aircraftFlow().asLiveData()
        .map{ it.map{ ac -> ac.registration} }

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

    fun selectAircraftType(type: AircraftType){
        viewModelScope.launch {
            updatedSelectedAircraft(type = type)
        }

        //TODO set AircraftRegistrationWithTypeData
    }

    /**
     * Called when user is typing in registrationField in UI.
     * look for aircraft to match registration or will create one with null type
     */
    fun updateRegistration(reg: String){
        TODO("not implemented" )
    }

    /**
     * Save aircraft to repository
     */
    fun saveAircraftToRepository() {
        aircraftRepository.saveAircraft(mAircraft)
    }

    fun updateSearchString(query: String) {
        _typesSearchStringFlow.value = query.uppercase(Locale.ROOT)
    }
}