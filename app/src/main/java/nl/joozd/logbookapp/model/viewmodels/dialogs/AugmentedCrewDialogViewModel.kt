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

import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.map
import nl.joozd.logbookapp.data.miscClasses.crew.AugmentedCrew
import nl.joozd.logbookapp.data.sharedPrefs.Prefs
import nl.joozd.logbookapp.model.helpers.hoursAndMinutesStringToInt
import nl.joozd.logbookapp.model.viewmodels.JoozdlogDialogViewModel

class AugmentedCrewDialogViewModel: JoozdlogDialogViewModel() {
    private val undoCrew = flightEditor.augmentedCrew

    private var crew get() = AugmentedCrew.fromInt(flightEditor.augmentedCrew)
    set(it) { flightEditor.augmentedCrew = it.toInt() }

    private val crewFlow = flightEditor.flightFlow.map { AugmentedCrew.fromInt(it.augmentedCrew) }

    fun crewSizeFlow() = crewFlow.map { it.size }
    fun didTakeoffFlow() = crewFlow.map { it.takeoff }
    fun didLandingFlow() = crewFlow.map { it.landing }
    fun takeoffLandingTimeFlow() = crewFlow.map { it.times }

    /**
     * This will emit[FIXED_REST] or [CALCULATE_REST]
     */
    fun multiCrewModeFlow() = combine(crewFlow, Prefs.defaultMultiCrewModeIsFixedRest.flow){
        crew, defaultModeIsFixed ->
        // defaultModeIsFixed == true means FIXED_REST, false means CALCULATE_REST.
        // Same goes for crew.isFixedTime.
        if (crew.isUndefined) defaultModeIsFixed else crew.isFixedTime
    }

    fun crewDown() {
        crew--
    }

    fun crewUp() {
        crew++
    }

    fun toggleTakeoff() {
        crew = crew.withTakeoff(!crew.takeoff)
    }

    fun toggleLanding() {
        crew = crew.withLanding(!crew.landing)
    }

    /**
     * If time is blank, will set takeoffLandingTimes to preset in Preferences
     * Else, will set it to whatever is entered.
     * Needs to be able to do Editable.toInt() or will throw exception.
     */
    fun setTakeoffLandingTime(time: String?) {
        if (time == null){
            return
        }
        time.hoursAndMinutesStringToInt()?.let { t ->
            setTakeoffLandingTime(t)
        }
    }

    fun undo(){
        flightEditor.augmentedCrew = undoCrew
    }

    fun setTakeoffLandingTime(time: Int) {
        crew = crew.withTimes(time)
    }

    private var crewSizeWhenFixedTimesUsed: Int = crew.size
    fun toggleUseFixedTime(){
        if(crew.isFixedTime)
             crew = crew.copy(size = crewSizeWhenFixedTimesUsed, isFixedTime = false)
        else {
            crewSizeWhenFixedTimesUsed = crew.size
        }
    }

    companion object{
        const val FIXED_REST = true
        const val CALCULATE_REST = false
    }
}

