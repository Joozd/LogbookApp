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
import kotlinx.coroutines.flow.map
import nl.joozd.logbookapp.model.viewmodels.JoozdlogDialogViewModel
import nl.joozd.textscanner.analyzer.CrewNamesCollector
import java.util.LinkedHashMap

class Name2DialogViewModel: JoozdlogDialogViewModel() {
    private val undoNames = flightEditor.name2 // this gets saved on first creation of the dialog. CANCEL will revert the names to this.

    // This provides a list that is at least an empty string so users don't have to add the first name but can just start typing.
    // Empty names are filtered out by flightEditor when saving.
    val currentNamesFlow: Flow<List<String>> = flightEditor.flightFlow.map { f -> f.name2.takeIf{ it.isNotEmpty()} ?: listOf("") }

    /** This takes two lists of strings which are matched by their indices.
     * Then, it builds a map of thise, and adds [CrewNamesCollector.functionOrder].first() as PIC name and the rest in that order to name2
     */
    fun handleScanActivityResult(namesList: List<String>, ranksList: List<String>){
        val namesToRanksMap: MutableMap<String, Int> = LinkedHashMap<String, Int>()
        namesList.indices.forEach { i ->
            namesToRanksMap[namesList[i]] = CrewNamesCollector.functionOrder[ranksList[i]] ?: 999
        }
        val picName = namesList.firstOrNull { name ->
            namesToRanksMap[name] == 0
        }

        val otherNames = namesList.sortedBy{ namesToRanksMap[it] }. filter { it != picName }
        picName?.let { flightEditor.name = it }
        flightEditor.name2 = otherNames
    }

    fun updateLastName(name: String){
        val unchangedNames = flightEditor.name2.dropLast(1) // all names except the last one. Empty list is still an empty list.
        flightEditor.name2 = unchangedNames + name
    }

    fun addNewEmptyName(){
        if(!flightEditor.name2.lastOrNull().isNullOrBlank())
            flightEditor.name2 += ""
    }

    fun removeName(nameToRemove: String){
        flightEditor.name2 = flightEditor.name2.filter { it != nameToRemove }
    }

    fun undo(){
        flightEditor.name2 = undoNames
    }
}