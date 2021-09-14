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

import androidx.lifecycle.Transformations
import nl.joozd.logbookapp.model.viewmodels.JoozdlogDialogViewModelWithWorkingFlight

class LandingsDialogViewModel: JoozdlogDialogViewModelWithWorkingFlight() {
    private val undoLandings = workingFlight.takeoffLandings
    private val undoAutoValues = workingFlight.autoFill

    val toDay = workingFlight.takeOffDayLiveData
    val toNight = workingFlight.takeOffNightLiveData
    val ldgDay = workingFlight.landingDayLiveData
    val ldgNight = workingFlight.landingNightLiveData
    val autoland = workingFlight.autoLandLiveData


    /**
     * Add or subtract one takeoff or landing or autoland on click.
     * workingFlight will take care of not becoming negative.
     */
    fun toDayUpButtonClick() {
        workingFlight.takeOffDay++
        workingFlight.autoFill = false
    }
    fun toNightUpButtonClick() {
        workingFlight.takeOffNight++
        workingFlight.autoFill = false
    }
    fun ldgDayUpButtonClick() {
        workingFlight.landingDay++
        workingFlight.autoFill = false
    }
    fun ldgNightUpButtonClick(){
        workingFlight.landingNight++
        workingFlight.autoFill = false
    }
    fun autolandUpButtonClick() {
        workingFlight.autoLand++
    }

    fun toDayDownButtonClick() {
        workingFlight.takeOffDay--
        workingFlight.autoFill = false
    }
    fun toNightDownButtonClick() {
        workingFlight.takeOffNight--
        workingFlight.autoFill = false
    }
    fun ldgDayDownButtonClick() {
        workingFlight.landingDay--
        workingFlight.autoFill = false
    }
    fun ldgNightDownButtonClick() {
        workingFlight.landingNight--
        workingFlight.autoFill = false
    }
    fun autolandDownButtonClick() {
        workingFlight.autoLand--
    }

    override fun undo(){
        workingFlight.takeoffLandings = undoLandings
        workingFlight.autoFill = undoAutoValues
    }
}