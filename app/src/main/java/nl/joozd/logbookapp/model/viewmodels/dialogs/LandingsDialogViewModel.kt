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
    private val undoLandings = workingFlight.takeoffLandings.value!!
    private val undoAutoValues = workingFlight.isAutoValues.value!!

    val toDay = Transformations.map(workingFlight.takeoffLandings){ it.takeoffDay }
    val toNight = Transformations.map(workingFlight.takeoffLandings){ it.takeoffNight }
    val ldgDay = Transformations.map(workingFlight.takeoffLandings){ it.landingDay }
    val ldgNight = Transformations.map(workingFlight.takeoffLandings){ it.landingNight }
    val autoland = Transformations.map(workingFlight.takeoffLandings){ it.autoLand }


    /**
     * Add or subtract one takeoff or landing or autoland on click.
     * workingFlight will take care of not becoming negative.
     */
    fun toDayUpButtonClick() {
        workingFlight.takeoffDay++
        workingFlight.setAutoValues(false)
    }
    fun toNightUpButtonClick() {
        workingFlight.takeoffNight++
        workingFlight.setAutoValues(false)
    }
    fun ldgDayUpButtonClick() {
        workingFlight.landingDay++
        workingFlight.setAutoValues(false)
    }
    fun ldgNightUpButtonClick(){
        workingFlight.landingNight++
        workingFlight.setAutoValues(false)
    }
    fun autolandUpButtonClick() {
        workingFlight.autoLand++
        workingFlight.setAutoValues(false)
    }

    fun toDayDownButtonClick() {
        workingFlight.takeoffDay--
        workingFlight.setAutoValues(false)
    }
    fun toNightDownButtonClick() {
        workingFlight.takeoffNight--
        workingFlight.setAutoValues(false)
    }
    fun ldgDayDownButtonClick() {
        workingFlight.landingDay--
        workingFlight.setAutoValues(false)
    }
    fun ldgNightDownButtonClick() {
        workingFlight.landingNight--
        workingFlight.setAutoValues(false)
    }
    fun autolandDownButtonClick() {
        workingFlight.autoLand--
        workingFlight.setAutoValues(false)
    }

    override fun undo(){
        workingFlight.setTakeoffLandings(undoLandings)
        workingFlight.setAutoValues(undoAutoValues)
    }
}