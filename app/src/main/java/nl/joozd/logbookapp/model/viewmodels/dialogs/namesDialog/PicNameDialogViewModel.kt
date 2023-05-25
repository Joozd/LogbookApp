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

package nl.joozd.logbookapp.model.viewmodels.dialogs.namesDialog

import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.map
import nl.joozd.logbookapp.data.repository.flightRepository.FlightRepository
import nl.joozd.logbookapp.model.helpers.makeNamesList
import nl.joozd.logbookapp.model.viewmodels.JoozdlogDialogViewModel
import nl.joozd.logbookapp.utils.CastFlowToMutableFlowShortcut

class PicNameDialogViewModel: JoozdlogDialogViewModel() {
    private val undoName = flightEditor.name
    private val queryFlow = MutableStateFlow("")
    private var query: String by CastFlowToMutableFlowShortcut(queryFlow)
    private val namesFlow = FlightRepository.instance.allFlightsFlow().map{
        it.makeNamesList()
    }
    private val pickedNameFlow = flightEditor.flightFlow.map { it.name }

    // This gives a list of names mapped to whether that name is currently picked or not. Only one name is picked, and that will be highlighted in GUI.
    fun namesListFlow(): Flow<List<Pair<String, Boolean>>> = combine(namesFlow, queryFlow, pickedNameFlow, flightEditor.flightFlow ){ names, query, currentPickedName, currentFlight ->
        (if (currentPickedName in names) names else listOf(currentPickedName) + names)
            .filter {
                query in it
                        && it !in currentFlight.name2     // Only pick names that aren't currently set as "other names".
            }
            .map { it to (it == currentPickedName) }
    }

    fun setName(name: String?){
        name?.let { flightEditor.name = it }
    }

    fun updateQuery(q: String){
        query = q
    }

    fun undo(){
        flightEditor.name = undoName
    }

}