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
import nl.joozd.logbookapp.extensions.minusOneWithFloor
import nl.joozd.logbookapp.model.viewmodels.JoozdlogDialogViewModel

class LandingsDialogViewModel: JoozdlogDialogViewModel() {
    val toDay = Transformations.map(flight){ it.takeOffDay }
    val toNight = Transformations.map(flight){ it.takeOffNight }
    val ldgDay = Transformations.map(flight){ it.landingDay }
    val ldgNight = Transformations.map(flight){ it.landingNight }
    val autoland = Transformations.map(flight){ it.autoLand }



    fun toDayUpButtonClick() { workingFlight?.let { workingFlight = it.copy(takeOffDay = it.takeOffDay + 1) }}
    fun toNightUpButtonClick() { workingFlight?.let { workingFlight = it.copy(takeOffNight = it.takeOffNight + 1) }}
    fun ldgDayUpButtonClick() { workingFlight?.let { workingFlight = it.copy(landingDay = it.landingDay + 1) }}
    fun ldgNightUpButtonClick() { workingFlight?.let { workingFlight = it.copy(landingNight = it.landingNight + 1) }}
    fun autolandUpButtonClick() { workingFlight?.let { workingFlight = it.copy(autoLand = it.autoLand + 1) }}

    fun toDayDownButtonClick() { workingFlight?.let { workingFlight = it.copy(takeOffDay = it.takeOffDay.minusOneWithFloor(0)) }}
    fun toNightDownButtonClick() { workingFlight?.let { workingFlight = it.copy(takeOffNight = it.takeOffNight.minusOneWithFloor(0)) }}
    fun ldgDayDownButtonClick() { workingFlight?.let { workingFlight = it.copy(landingDay = it.landingDay.minusOneWithFloor(0)) }}
    fun ldgNightDownButtonClick() { workingFlight?.let { workingFlight = it.copy(landingNight = it.landingNight.minusOneWithFloor(0)) }}
    fun autolandDownButtonClick() { workingFlight?.let { workingFlight = it.copy(autoLand = it.autoLand.minusOneWithFloor(0)) }}
}