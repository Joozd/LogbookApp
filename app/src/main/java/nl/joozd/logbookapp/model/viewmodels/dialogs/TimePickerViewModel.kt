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

import kotlinx.coroutines.flow.filterNotNull
import kotlinx.coroutines.flow.map
import nl.joozd.logbookapp.data.miscClasses.crew.AugmentedCrew
import nl.joozd.logbookapp.model.enumclasses.DualInstructorFlag
import nl.joozd.logbookapp.model.enumclasses.PicPicusFlag
import nl.joozd.logbookapp.model.helpers.hoursAndMinutesStringToInt
import nl.joozd.logbookapp.model.viewmodels.JoozdlogDialogViewModel

/**
 * Does not support changing orig or dest during this dialog open
 */
class TimePickerViewModel: JoozdlogDialogViewModel() {
    private val undoFlight = flightEditor.snapshot()

    private val flightFlow get() = flightEditor.flightFlow

    fun totalTimeFlow() = flightFlow.map { it.calculateTotalTime() } // this shows calculated time even when correctedTotalTime is zero.
    fun nightTimeFlow() = flightFlow.map { it.nightTime }
    fun ifrTimeFlow() = flightFlow.map { it.ifrTime }

    fun restTimeIfNotPIC() = flightFlow.map { f -> f.restTime().takeIf { !f.isPIC }}.filterNotNull()

    fun augmentedCrewFlow() = flightFlow.map { AugmentedCrew.fromInt(it.augmentedCrew) }
    fun picPicusFlow() = flightFlow.map { when {
        it.isPIC -> PicPicusFlag.PIC
        it.isPICUS -> PicPicusFlag.PICUS
        else -> PicPicusFlag.NONE
    }    }

    fun copilotFlow() = flightFlow.map { it.isCoPilot }
    fun dualInstructorFlow() = flightFlow.map { when {
        it.isDual -> DualInstructorFlag.DUAL
        it.isInstructor -> DualInstructorFlag.INSTRUCTOR
        else -> DualInstructorFlag.NONE
    }   }

    fun undo(){
        flightEditor.apply {
            setToFLight(undoFlight)
        }
    }

    fun setIfrTime(enteredTime: String) {
        enteredTime.hoursAndMinutesStringToInt()?.let{
            flightEditor.ifrTime = it.maxedAtTotalTime()
        }
    }

    fun setNightTime(enteredTime: String) {
        enteredTime.hoursAndMinutesStringToInt()?.let{
            flightEditor.nightTime = it.maxedAtTotalTime()
        }
    }

    fun setTotalTimeOfFlight(enteredTime: String) {
        enteredTime.hoursAndMinutesStringToInt()?.let{
            flightEditor.totalFlightTime = it
        }
    }

    fun setRestTime(restTime: String){
        restTime.hoursAndMinutesStringToInt()?.let {
            flightEditor.augmentedCrew = AugmentedCrew.fixedRest(it).toInt()
        }
    }

    fun togglePicusPicNone(){
        flightEditor.togglePicusPicNeither()
    }

    fun toggleCopilot() {
        flightEditor.isCoPilot = !flightEditor.isCoPilot
    }

    fun toggleDualInstructorNone() {
        flightEditor.toggleDualInstructorNeither()
    }

    private var oldAugmented: Int = 0 // not augmented
    /**
     * Returns true if toggled, false if not.
     */
    fun toggleAugmented(): Boolean{
        val currentCrewInt = flightEditor.snapshot().augmentedCrew
        val currentCrew = AugmentedCrew.fromInt(currentCrewInt)
        val savedCrew = AugmentedCrew.fromInt(oldAugmented)
        if(currentCrew.isAugmented()){
            //if current is augmented, we set it to 0 and save current crew
            oldAugmented = currentCrewInt
            flightEditor.augmentedCrew = 0
            return true
        }
        if(savedCrew.isAugmented()){
            //If we get here, currentCrew is NOT augmented, but savedCrew IS
            flightEditor.augmentedCrew = savedCrew.toInt()
            oldAugmented = 0
            return true
        }
        // if we get here, bot current and old crew are not augmented.
            return false
    }

    private fun Int.maxedAtTotalTime() = minOf(this, flightEditor.totalFlightTime)
}