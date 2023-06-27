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
import nl.joozd.textscanner.analyzer.CrewNamesCollector
import java.util.LinkedHashMap

class Name2DialogViewModel: JoozdlogDialogViewModel() {
    private val undoNames = flightEditor.name2 // this gets saved on first creation of the dialog. CANCEL will revert the names to this.
    private val repo = FlightRepository.instance
    private val pickedNewNameFlow = MutableStateFlow<String?>(null)
    private var pickedNewName: String? by CastFlowToMutableFlowShortcut(pickedNewNameFlow)

    private val pickedSelectedNameFlow = MutableStateFlow<String?>(null)
    private var pickedSelectedName: String? by CastFlowToMutableFlowShortcut(pickedSelectedNameFlow)

    private val queryFlow = MutableStateFlow("")
    private var query: String by CastFlowToMutableFlowShortcut(queryFlow)

    private val allAvailableNamesFlow: Flow<List<String>> = repo.allFlightsFlow().map{
        it.makeNamesList()
    }
    private val currentNamesFlow: Flow<List<String>> = flightEditor.flightFlow.map { it.name2 }

    // This gives a list of names mapped to whether that name is currently picked or not. Only one name is picked, and that will be highlighted in GUI.
    fun pickableNamesListFlow(): Flow<List<Pair<String, Boolean>>> = combine(allAvailableNamesFlow, currentNamesFlow, queryFlow, pickedNewNameFlow, flightEditor.flightFlow ) { all, current, query, picked, currentFlight->
        all.filter{
            it !in current                          // only pick names that aren't picked yet
                    && query in it                  // only pick names that match current search text
                    && it != currentFlight.name     // Only pick names that aren't currently set as PIC.
        }.map {
            it to (it == picked)
        }
    }
    fun currentNamesListFlow() = combine (currentNamesFlow, pickedSelectedNameFlow) { current, picked ->
        current.map { name -> name to (name == picked) }
    }

    /** This takes two lists of strings which are matched by their indices.
     * Then, it builds a map of thise, and adds [CrewNamesCollector.functionOrder].first() as PIC name and the rest in that order to name2
     */
    fun handleScanActivityResult(namesList: List<String>, ranksList: List<String>){
        val namesToRanksMap: MutableMap<String, Int> = LinkedHashMap<String, Int>()
        namesList.indices.forEach { i ->
            namesToRanksMap[namesList[i]] = CrewNamesCollector.functionOrder[ranksList[i]] ?: 999
        }
        val picName = namesList.first { name ->
            namesToRanksMap[name] == 0
        }

        val otherNames = namesList.sortedBy{ namesToRanksMap[it] }. filter { it != picName }
        flightEditor.name = picName
        flightEditor.name2 = otherNames
    }

    fun pickNewName(name: String){
        pickedNewName = name
    }

    fun pickSelectedName(name: String){
        pickedSelectedName = name
    }

    fun removeCurrentNameFromSelectedNames(){
        pickedSelectedName?.let{ n ->
            pickedSelectedName = null
            flightEditor.name2 = flightEditor.name2.filter { it != n}
        }
    }

    fun addName(name: String){
        flightEditor.name2 += name
    }

    fun updateQuery(q: String?){
        query = q ?: ""
    }

    fun undo(){
        flightEditor.name2 = undoNames
    }
}