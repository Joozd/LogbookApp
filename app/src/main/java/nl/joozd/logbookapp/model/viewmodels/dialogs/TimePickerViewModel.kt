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
import nl.joozd.logbookapp.data.miscClasses.crew.AugmentedCrew
import nl.joozd.logbookapp.model.enumclasses.DualInstructorFlag
import nl.joozd.logbookapp.model.enumclasses.PicPicusFlag
import nl.joozd.logbookapp.model.helpers.hoursAndMinutesStringToInt
import nl.joozd.logbookapp.model.viewmodels.JoozdlogDialogViewModel

/**
 * Does not support changing orig or dest during this dialog open
 */
class TimePickerViewModel: JoozdlogDialogViewModel() {
    private val undoCorrectedTotalTime = flightEditor.correctedTotalTime
    private val undoNightTime = flightEditor.nightTime
    private val undoIfrTime = flightEditor.ifrTime
    private val undoAugmentedCrew = flightEditor.augmentedCrew
    private val undoAutoFill = flightEditor.autoFill

    private val f get() = flightEditor.flightFlow

    val debugF get() = f

    fun totalTimeFlow() = f.map { it.calculateTotalTime() } // this shows calculated time even when correctedTotalTime is zero.
    fun nightTimeFlow() = f.map { it.nightTime }
    fun ifrTimeFlow() = f.map { it.ifrTime }
    fun augmentedCrewFlow() = f.map { AugmentedCrew.of(it.augmentedCrew) }
    fun picPicusFlow() = f.map { when {
        it.isPIC -> PicPicusFlag.PIC
        it.isPICUS -> PicPicusFlag.PICUS
        else -> PicPicusFlag.NONE
    }    }
    fun copilotFlow() = f.map { it.isCoPilot }
    fun dualInstructorFlow() = f.map { when {
        it.isDual -> DualInstructorFlag.DUAL
        it.isInstructor -> DualInstructorFlag.INSTRUCTOR
        else -> DualInstructorFlag.NONE
    }   }

    fun undo(){
        flightEditor.apply {

            // set to avoid unnecessary autoValues calculations
            // It is reset to undoAutoFill at the end which will do all the autoValues if needed.
            autoFill = false

            correctedTotalTime = undoCorrectedTotalTime
            nightTime = undoNightTime
            ifrTime = undoIfrTime
            augmentedCrew = undoAugmentedCrew
            autoFill = undoAutoFill
        }
    }

    fun setIfrTime(enteredTime: String?) {
        enteredTime?.hoursAndMinutesStringToInt()?.let{
            flightEditor.ifrTime = it.maxedAtTotalTime().also{
                println("IFR TIME $it")
            }
        }
    }

    fun setNightTime(enteredTime: String?) {
        enteredTime?.hoursAndMinutesStringToInt()?.let{
            flightEditor.nightTime = it.maxedAtTotalTime()
        }
    }

    fun setTotalTimeOfFlight(enteredTime: String?) {
        enteredTime?.hoursAndMinutesStringToInt()?.let{
            flightEditor.totalFlightTime = it
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

    private fun Int.maxedAtTotalTime() = minOf(this, flightEditor.totalFlightTime)
}