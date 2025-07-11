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

import androidx.lifecycle.viewModelScope
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.launch
import nl.joozd.logbookapp.data.sharedPrefs.Prefs
import nl.joozd.logbookapp.model.viewmodels.JoozdlogDialogViewModel
import nl.joozd.logbookapp.utils.replaceFirstInstanceOf
import nl.joozd.textscanner.analyzer.CrewNamesCollector
import java.util.LinkedHashMap

class Name2DialogViewModel: JoozdlogDialogViewModel() {
    private val undoNames = flightEditor.name2 // this gets saved on first creation of the dialog. CANCEL will revert the names to this.

    // The name being edited.
    private var indexOfNameBeingEdited = 0

    // This provides a list that is at least an empty string so users don't have to add the first name but can just start typing.
    // Empty names are filtered out by flightEditor when saving.
    val currentNamesFlow: Flow<List<String>> = flightEditor.flightFlow.map { f -> f.name2.takeIf{ it.isNotEmpty()} ?: listOf("") }

    /** This takes two lists of strings which are matched by their indices.
     * Then, it builds a map of thise, and adds [CrewNamesCollector.functionOrder].first() as PIC name and the rest in that order to name2
     */
    fun handleScanActivityResult(namesList: List<String>, ranksList: List<String>) {
        // async because we need to get [ownName] and [replaceOwnNameWithSelf] from Prefs. Fast enough to not need loading screen.
        viewModelScope.launch {
            //replace own name with "SELF" if needed
            val namesWithSelf =
                if(Prefs.replaceOwnNameWithSelf())
                    namesList.replaceFirstInstanceOf(Prefs.ownName(), "SELF")
                else
                    namesList

            /**
             * will be something like "Captain" to 0, "Purser" to 3, "First Officer" to 1, etc.
             * Follows [CrewNamesCollector.validFunctions]
             */
            val namesToRanksMap: MutableMap<String, Int> = LinkedHashMap<String, Int>().apply {
                namesWithSelf.indices.forEach { i ->
                    // [this] refers to the LinkedHashMap we are building
                    this[namesWithSelf[i]] = CrewNamesCollector.functionOrder[ranksList[i]] ?: 999
                }
            }

            val sortedNames = namesWithSelf.sortedBy { namesToRanksMap[it] }

            val picName = sortedNames.firstOrNull().takeIf { namesToRanksMap[it] == 0 } // first name in sortedNames, but only if it has rank 0 (Captain)
            val nonPicNames = sortedNames.filter { it != picName }

            picName?.let { flightEditor.name = it } // there might not be anybody labeled Captain in results, in this case nothing will be done here.
            flightEditor.name2 = nonPicNames
        }
    }

    fun updateNameBeingEdited(name: String){
        val unchangedNamesBefore = flightEditor.name2.take(indexOfNameBeingEdited) // all names before name being edited
        val unchangedNamesAfter = flightEditor.name2.drop(indexOfNameBeingEdited + 1) // all names after name being edited
        flightEditor.name2 = unchangedNamesBefore + name + unchangedNamesAfter
    }

    fun addNewEmptyName(){
        val lastName = flightEditor.name2.lastOrNull()
        if((lastName ?: "EMPTY_LIST").isNotBlank()) // Add a new name if the list is empty or the last name in the list is not blank.
            flightEditor.name2 += ""
        indexOfNameBeingEdited = flightEditor.name2.lastIndex
    }

    // removes the first occurrence of found name
    fun removeName(nameToRemove: String){
        updateIndexOfNameBeingEditedIfNeeded(nameToRemove)
        val firstOccurrenceFound = flightEditor.name2.indexOf(nameToRemove).takeIf { it != -1 }
        firstOccurrenceFound?.let{ index ->
            flightEditor.name2 = flightEditor.name2.take(index) + flightEditor.name2.drop(index + 1)
        }
    }

    // Removing a name should change the index of the name being edited if it means that gets changed. This takes care of that
    private fun updateIndexOfNameBeingEditedIfNeeded(nameToRemove: String) {
        val indexOfNameToRemove = flightEditor.name2.indexOf(nameToRemove).takeIf { it != -1 } ?: Int.MAX_VALUE
        when {
            indexOfNameToRemove < indexOfNameBeingEdited -> indexOfNameBeingEdited--
            indexOfNameToRemove == indexOfNameBeingEdited -> indexOfNameBeingEdited = flightEditor.name2.lastIndex
        }
    }

    fun undo(){
        flightEditor.name2 = undoNames
    }

    fun notifyNowEditing(name: String){
        flightEditor.name2.indexOf(name).takeIf{ it != -1 }?.let{
            indexOfNameBeingEdited = it
        }
    }
}