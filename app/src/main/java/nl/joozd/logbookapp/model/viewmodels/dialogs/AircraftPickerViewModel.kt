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
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.launch
import nl.joozd.joozdlogcommon.AircraftType
import nl.joozd.logbookapp.data.dataclasses.Aircraft
import nl.joozd.logbookapp.data.repository.aircraftrepository.AircraftRepository
import nl.joozd.logbookapp.extensions.inIgnoreCase
import nl.joozd.logbookapp.model.viewmodels.JoozdlogDialogViewModel
import nl.joozd.logbookapp.utils.CastFlowToMutableFlowShortcut
import java.util.*


/**
 * ViewModel for AircraftPicker and SimTypePicker fragments
 */
class AircraftPickerViewModel: JoozdlogDialogViewModel(){
    private val aircraftRepository = AircraftRepository.instance
    private val _query = MutableStateFlow("")
    private var query: String by CastFlowToMutableFlowShortcut(_query)

    private val selectedType get() = flightEditor.aircraft.type

    fun aircraftTypesFlow(): Flow<List<Pair<AircraftType, Boolean>>> =
        combine(aircraftRepository.aircraftTypesFlow(), _query) { types, query ->
            types.filter { type -> query inIgnoreCase type.name || query inIgnoreCase type.shortName }
                .map { it to (it == selectedType) }
    }

    val knownRegistrationsLiveData =
        aircraftRepository.aircraftMapFlow().map{
            it.keys.toList()
        }

    /**
     * Update selected aircaft's registration, type or source
     */
    private fun updatedSelectedAircraft(registration: String? = null, type: AircraftType? = null, source: Int? = null) {
        flightEditor.aircraft = Aircraft(
            registration = registration ?:flightEditor.aircraft.registration,
            type = type,
            source = Aircraft.KNOWN
        )
    }

    fun selectAircraftType(type: AircraftType){
        viewModelScope.launch {
            updatedSelectedAircraft(type = type)
        }

        //TODO save AircraftRegistrationWithTypeData? Or will we just grab this from flights?
        //Why do I even have that ARWT database?
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
        TODO("Not implemented")
    }

    fun updateSearchString(searchString: String) {
        query = searchString.uppercase(Locale.ROOT)
    }
}