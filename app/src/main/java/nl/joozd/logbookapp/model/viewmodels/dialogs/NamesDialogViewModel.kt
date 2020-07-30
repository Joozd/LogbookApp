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

import androidx.lifecycle.LiveData
import androidx.lifecycle.MediatorLiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.Transformations
import nl.joozd.logbookapp.R
import nl.joozd.logbookapp.model.feedbackEvents.FeedbackEvents
import nl.joozd.logbookapp.model.helpers.FlightDataEntryFunctions.addName2
import nl.joozd.logbookapp.model.helpers.FlightDataEntryFunctions.removeLastName2
import nl.joozd.logbookapp.model.viewmodels.JoozdlogDialogViewModel

class NamesDialogViewModel: JoozdlogDialogViewModel() {
    /**
     * this MUST be set in onActivityCreated in Fragment so feedback event will be observed
     * also, feedbackEvent must be observed. If [won1] == null, things won't work.
     */
    var workingOnName1: Boolean? = null
    set(name1used){
        field = name1used
        when (name1used){
            true -> layoutForName1()
            false -> layoutForName2()
            null -> feedback(FeedbackEvents.NamesDialogEvents.NAME1_OR_NAME2_NOT_SELECTED)
        }
    }

    //Texts for UI elements depending on which name is being edited
    private val _addSearchFieldNameButtonTextResource = MutableLiveData<Int>() // getString(R.string.select)
    val addSearchFieldNameButtonTextResource: LiveData<Int>
        get() = _addSearchFieldNameButtonTextResource
    private val _addSelectedNameButtonTextResource = MutableLiveData<Int>() //getString(R.string.select)
    val addSelectedNameButtonTextResource: LiveData<Int>
        get() = _addSelectedNameButtonTextResource
    private val _removeLastButonTextResource = MutableLiveData<Int>() //getString(R.string.clear)
    val removeLastButonTextResource: LiveData<Int>
        get() = _removeLastButonTextResource



    private val _selectedName = MutableLiveData<String>()
    val selectedName: LiveData<String>
        get() = _selectedName
    fun selectName(name: String){
        _selectedName.value = name
    }
    private val _manualName = MutableLiveData<String>()
    private var manualName: String
        get() = _manualName.value?: ""
        set(n) { _manualName.value = n }

    val takenNames: List<String>
        get() = currentNames.value?.split("\n") ?: emptyList()

    val currentNames = Transformations.map(flight) {
        when(workingOnName1){
            true -> it.name
            false -> it.name2.split("|").joinToString("\n")
            null -> "ERROR"
        }
    }

    // all names
    private val _allNames = MediatorLiveData<List<String>>()
    val allNames: LiveData<List<String>>
        get() = _allNames
    init{
        _allNames.addSource(flightRepository.allNames){
            _allNames.value = filterNames(it)
        }
        _allNames.addSource(_manualName){
            _allNames.value = filterNames((flightRepository.allNames.value ?: emptyList()))
        }
        _allNames.addSource(currentNames){
            _allNames.value = filterNames((flightRepository.allNames.value ?: emptyList()))
        }
    }



    fun addSelectedName(){
        selectedName.value?.let{ selName ->
            workingFlight?.let { f ->
                workingFlight = when (workingOnName1) {
                    true -> f.copy(name = selName)
                    false -> f.addName2(selName)
                    null -> error ("ERROR woringOnName1 == null")
                }
            }
        }
    }

    fun searchNames(query: String){
        _manualName.value = query
    }

    fun addManualNameClicked(){
        //TODO build this
        feedback(FeedbackEvents.NamesDialogEvents.NOT_IMPLEMENTED)
    }

    fun removeLastName(){
        workingFlight?.let { f ->
            workingFlight = when(workingOnName1){
                true -> f.copy(name = "")
                false -> f.removeLastName2()
                null -> error ("ERROR woringOnName1 == null")
            }
        }
    }

    private fun layoutForName1(){
        _addSearchFieldNameButtonTextResource.value = R.string.select
        _addSelectedNameButtonTextResource.value = R.string.select
        _removeLastButonTextResource.value = R.string.clear
    }
    private fun layoutForName2(){
        _addSearchFieldNameButtonTextResource.value = R.string.addThis
        _addSelectedNameButtonTextResource.value = R.string.addThis
        _removeLastButonTextResource.value = R.string.remove
    }

    private fun filterNames(names: List<String>) = names.filter {manualName in it}.filter{it !in takenNames}
}