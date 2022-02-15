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

import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.map
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
    private val queryFlow = MutableStateFlow("")
    private var query: String by CastFlowToMutableFlowShortcut(queryFlow)

    private val undoAircraft = flightEditor.aircraft

    fun selectedAircraftFlow() = flightEditor.flightFlow.map { it.aircraft }

    fun aircraftTypesFlow(): Flow<List<Pair<AircraftType, Boolean>>> =
        combine(aircraftRepository.aircraftTypesFlow(), queryFlow, selectedAircraftFlow()) { types, query, currentAircraft ->
            types.filter { type -> query inIgnoreCase type.name || query inIgnoreCase type.shortName }
                .map { it to (it == currentAircraft.type) }
    }

    val knownRegistrationsLiveData =
        aircraftRepository.aircraftMapFlow().map{
            it.keys.toList()
        }

    /**
     * Update selected aircaft's registration, type or source
     */
    private fun updatedSelectedAircraft(registration: String? = null, type: AircraftType? = null) {
        flightEditor.aircraft = Aircraft(
            registration = registration ?: flightEditor.aircraft.registration,
            type = type ?: flightEditor.aircraft.type,
            source = Aircraft.KNOWN
        )
    }

    fun selectAircraftType(type: AircraftType){
        updatedSelectedAircraft(type = type)
    }

    fun updateSearchString(searchString: String) {
        query = searchString.uppercase(Locale.ROOT)
    }

    fun updateRegistration(reg: String){
        updatedSelectedAircraft(registration = reg)
    }

    fun undo(){
        flightEditor.aircraft = undoAircraft
    }
}