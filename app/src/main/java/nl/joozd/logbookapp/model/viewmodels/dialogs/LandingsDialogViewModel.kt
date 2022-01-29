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

import androidx.lifecycle.MutableLiveData
import nl.joozd.logbookapp.model.viewmodels.JoozdlogDialogViewModel

class LandingsDialogViewModel: JoozdlogDialogViewModel() {
    val tempLiveData = MutableLiveData(0)

    val toDay = tempLiveData
    val toNight = tempLiveData
    val ldgDay = tempLiveData
    val ldgNight = tempLiveData
    val autoland = tempLiveData


    /**
     * Add or subtract one takeoff or landing or autoland on click.
     * workingFlight will take care of not becoming negative.
     */
    fun toDayUpButtonClick() {
        TODO("TODO")
    }
    fun toNightUpButtonClick() {
        TODO("TODO")
    }
    fun ldgDayUpButtonClick() {
        TODO("TODO")
    }
    fun ldgNightUpButtonClick() {
        TODO("TODO")
    }
    fun autolandUpButtonClick() {
        TODO("TODO")
    }

    fun toDayDownButtonClick() {
        TODO("TODO")
    }
    fun toNightDownButtonClick() {
        TODO("TODO")
    }
    fun ldgDayDownButtonClick() {
        TODO("TODO")
    }
    fun ldgNightDownButtonClick() {
        TODO("TODO")
    }
    fun autolandDownButtonClick() {
        TODO("TODO")
    }
}