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
import nl.joozd.logbookapp.model.ModelFlight
import nl.joozd.logbookapp.model.helpers.hoursAndMinutesStringToInt
import nl.joozd.logbookapp.model.viewmodels.JoozdlogDialogViewModel
import java.time.Duration

class AugmentedCrewDialogViewModel: JoozdlogDialogViewModel() {
    private val undoCrew = flightEditor.augmentedCrew

    private var crew get() = AugmentedCrew.fromInt(flightEditor.augmentedCrew)
    set(it) { flightEditor.augmentedCrew = it.toInt() }

    private val crewFlow = flightEditor.flightFlow.map { AugmentedCrew.fromInt(it.augmentedCrew) }

    val crewSizeFlow = crewFlow.map { it.size }
    val didTakeoffFlow = crewFlow.map { it.takeoff }
    val didLandingFlow = crewFlow.map { it.landing }
    val takeoffLandingTimeFlow = crewFlow.map { it.times }

    val restTimeFlow = flightEditor.flightFlow.map{
        calculateRestTimeFromFlight(it)
    }

    /**
     * This will emit true if [crew] is Fixed Time, or if undefined and [Prefs.defaultMultiCrewModeIsFixedRest]
     */
    val isFixedTimeFlow = combine(crewFlow, Prefs.defaultMultiCrewModeIsFixedRest.flow){
        crew, defaultModeIsFixed ->
        // defaultModeIsFixed == true means FIXED_REST, false means CALCULATE_REST.
        // Same goes for crew.isFixedTime.
        if (crew.isUndefined) defaultModeIsFixed else crew.isFixedTime
    }

    fun crewDown() {
        crew--
        updateFixedRestTime()
    }

    fun crewUp() {
        crew++
        updateFixedRestTime()
    }

    fun toggleTakeoff() {
        crew = crew.withTakeoff(!crew.takeoff)
        updateFixedRestTime()
    }

    fun toggleLanding() {
        crew = crew.withLanding(!crew.landing)
        updateFixedRestTime()
    }

    /**
     * If time is blank, will set takeoffLandingTimes to preset in Preferences
     * Else, will set it to whatever is entered.
     * Needs to be able to do Editable.toInt() or will throw exception.
     */
    fun setTime(time: String?, updateFixedRestTime: Boolean = true) {
        if (time == null){
            return
        }
        time.hoursAndMinutesStringToInt()?.let { t ->
            setTime(t)
            if(updateFixedRestTime) updateFixedRestTime()
        }
    }

    fun undo(){
        flightEditor.augmentedCrew = undoCrew
    }

    fun setTime(time: Int) {
        crew = crew.withTimes(time)
    }

    private var takeoffLandingTimeNotInUse: Int = 0 // setting this to 0 (undefined) will cause the fragment to load it from Prefs

    private var fixedRestTimeNotInUse: Int = calculateRestTimeFromFlight(flightEditor.snapshot())

    fun toggleUseFixedTime(){
        if(crew.isFixedTime) {
            fixedRestTimeNotInUse = crew.times
            crew = crew.copy(times = takeoffLandingTimeNotInUse, isFixedTime = false)
        }
        else {
            takeoffLandingTimeNotInUse = crew.times
            crew = crew.copy(times = fixedRestTimeNotInUse, isFixedTime = true)
        }
    }

    private fun calculateRestTimeFromFlight(it: ModelFlight): Int {
        val totalFlightTime = Duration.between(it.timeOut, it.timeIn).toMinutes()
        val loggableFlightTime = it.calculateTotalTime()
        return (totalFlightTime - loggableFlightTime).toInt()
    }

    /**
     * Call this whenever a value gets changed from calculated times, so fixed time gets recalculated in the background but only whenever any input changed.
     */
    private fun updateFixedRestTime() {
        fixedRestTimeNotInUse = calculateRestTimeFromFlight(flightEditor.snapshot())
    }
}

