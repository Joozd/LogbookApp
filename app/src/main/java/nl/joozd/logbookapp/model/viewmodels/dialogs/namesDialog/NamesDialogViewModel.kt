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

package nl.joozd.logbookapp.model.viewmodels.dialogs.namesDialog

import androidx.lifecycle.LiveData
import androidx.lifecycle.MediatorLiveData
import androidx.lifecycle.MutableLiveData
import nl.joozd.logbookapp.model.viewmodels.JoozdlogDialogViewModelWithWorkingFlight

abstract class NamesDialogViewModel: JoozdlogDialogViewModelWithWorkingFlight() {
    /**
     * Set to true if working on PIC, or false if working on other names (name2)
     */
    abstract val workingOnName1: Boolean

    //Texts for UI elements depending on which name is being edited
    protected val mutableAddSearchFieldNameButtonTextResource = MutableLiveData<Int>() // getString(R.string.select)
    val addSearchFieldNameButtonTextResource: LiveData<Int>
        get() = mutableAddSearchFieldNameButtonTextResource
    protected val mutableAddSelectedNameButtonTextResource = MutableLiveData<Int>() //getString(R.string.select)
    val addSelectedNameButtonTextResource: LiveData<Int>
        get() = mutableAddSelectedNameButtonTextResource
    protected val mutableRemoveLastButonTextResource = MutableLiveData<Int>() //getString(R.string.clear)
    val removeLastButonTextResource: LiveData<Int>
        get() = mutableRemoveLastButonTextResource



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

    private val takenNames: List<String>
        get() = workingFlight.allNamesList.value ?: emptyList()

    /**
     * One string with all names we are working on now, separated by '\n'
     */
    abstract val currentNames: LiveData<String>

    /**
     * All names to be placed in picker list.
     * [takenNames] are to be subtracted from list of all known names ([filterNames] does that)
     */
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
        //workingFlight.allNames is a List<String> of all names used in currentFlight
        _allNames.addSource(workingFlight.allNamesList){
            _allNames.value = filterNames((flightRepository.allNames.value ?: emptyList()))
        }
    }

    /**
     * Add a name to the list of names, or replace it if only one name allowed
     */
    abstract fun addName(name:String)

    fun addSelectedName(){
            selectedName.value?.let{ selName ->
                addName(selName)
            }
    }

    fun searchNames(query: String){
        _manualName.value = query
    }

    fun addManualNameClicked() {
        if (manualName.isBlank()) return // don't do anything if nothing typed
        addName(manualName)
    }


    /**
     * Remove the last name from the list. If no names left, set names to [""]
     */
    abstract fun removeLastName()

    private fun filterNames(names: List<String>) = names.filter {manualName in it}.filter{it !in takenNames}
}