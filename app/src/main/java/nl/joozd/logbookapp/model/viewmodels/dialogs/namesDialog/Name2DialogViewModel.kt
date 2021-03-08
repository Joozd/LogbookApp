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

import android.util.Log
import androidx.lifecycle.map
import nl.joozd.logbookapp.R

class Name2DialogViewModel: NamesDialogViewModel() {
    init{
        Log.d("name2DialogViewModel", "workingFlight == $workingFlight")
    }
    /**
     * Set to true if working on PIC, or false if working on other names (name2)
     */
    override val workingOnName1 = false

    /**
     * One string with all names we are working on now, separated by '\n'
     */
    override val currentNames = workingFlight.name2ListLiveData.map{ it.joinToString("\n")}

    /**
     * Add a selected name to the list of names, or replace name if only one name allowed
     */
    override fun addName(name: String) {
        workingFlight.setNames2List(workingFlight.name2ListLiveData.value!! + name)
    }

    /**
     * Remove the last name from the list. If no names left, set names to [""]
     */
    override fun removeLastName() {
        workingFlight.setNames2List(workingFlight.name2ListLiveData.value!!.dropLast(1))
    }

    /**
     * Set correct labels for this dialog
     */
    init {
        mutableAddSearchFieldNameButtonTextResource.value = R.string.addThis
        mutableAddSelectedNameButtonTextResource.value = R.string.addThis
        mutableRemoveLastButonTextResource.value = R.string.remove
    }


}