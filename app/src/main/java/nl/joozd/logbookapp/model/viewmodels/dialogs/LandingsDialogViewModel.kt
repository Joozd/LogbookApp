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

import kotlinx.coroutines.flow.map
import nl.joozd.logbookapp.model.viewmodels.JoozdlogDialogViewModel
import nl.joozd.logbookapp.model.workingFlight.TakeoffLandings

class LandingsDialogViewModel: JoozdlogDialogViewModel() {
    private val undoLandings = flightEditor.takeoffLandings
    val takeoffLandingsFlow = flightEditor.flightFlow.map { it.takeoffLandings }
    var takeoffLandings: TakeoffLandings
        get() = flightEditor.takeoffLandings
        set(it){ flightEditor.takeoffLandings = it }


    /**
     * Add or subtract one takeoff or landing or autoland on click.
     * workingFlight will take care of not becoming negative.
     */
    fun toDayUpButtonClick() {
        takeoffLandings = takeoffLandings.copy(takeoffDay = takeoffLandings.takeoffDay + 1)
    }
    fun toNightUpButtonClick() {
        takeoffLandings = takeoffLandings.copy(takeoffNight = takeoffLandings.takeoffNight + 1)
    }
    fun ldgDayUpButtonClick() {
        takeoffLandings = takeoffLandings.copy(landingDay = takeoffLandings.landingDay + 1)
    }
    fun ldgNightUpButtonClick() {
        takeoffLandings = takeoffLandings.copy(landingNight = takeoffLandings.landingNight + 1)
    }
    fun autolandUpButtonClick() {
        takeoffLandings = takeoffLandings.copy(autoLand = takeoffLandings.autoLand + 1)
    }

    fun toDayDownButtonClick() {
        takeoffLandings = takeoffLandings.copy(takeoffDay = maxOf(0, takeoffLandings.takeoffDay - 1))
    }
    fun toNightDownButtonClick() {
        takeoffLandings = takeoffLandings.copy(takeoffNight = maxOf(0, takeoffLandings.takeoffNight - 1))
    }
    fun ldgDayDownButtonClick() {
        takeoffLandings = takeoffLandings.copy(landingDay = maxOf(0, takeoffLandings.landingDay - 1))
    }
    fun ldgNightDownButtonClick() {
        takeoffLandings = takeoffLandings.copy(landingNight = maxOf(0, takeoffLandings.landingNight - 1))
    }
    fun autolandDownButtonClick() {
        takeoffLandings = takeoffLandings.copy(autoLand = maxOf(0, takeoffLandings.autoLand - 1))
    }

    fun undo(){
        flightEditor.takeoffLandings = undoLandings
    }
}