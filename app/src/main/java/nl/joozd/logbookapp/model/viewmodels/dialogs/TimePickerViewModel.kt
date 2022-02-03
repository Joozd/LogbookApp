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

import androidx.lifecycle.LiveData
import androidx.lifecycle.MediatorLiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.map
import nl.joozd.logbookapp.R
import nl.joozd.logbookapp.model.feedbackEvents.FeedbackEvents
import nl.joozd.logbookapp.model.feedbackEvents.FeedbackEvents.TimePickerEvents
import nl.joozd.logbookapp.model.helpers.FlightDataEntryFunctions.hoursAndMinutesStringToInt
import nl.joozd.logbookapp.model.helpers.FlightDataPresentationFunctions.minutesToHoursAndMinutesString
import nl.joozd.logbookapp.model.viewmodels.JoozdlogDialogViewModel
import nl.joozd.logbookapp.model.workingFlight.FlightEditor

/**
 * Does not support changing orig or dest during this dialog open
 */
class TimePickerViewModel: JoozdlogDialogViewModel() {
    private val tempLivedataBoolean = MutableLiveData(false)
    private val tempLivedataString = MutableLiveData("REMOVE THIS")
    private val tempLiveDataInt = MutableLiveData(Int.MIN_VALUE)

    /**
     * Livedata objects:
     */
    val isAugmentedCrew // LiveData<Boolean>
        get() = false // WIP

    private val _isPicOrPicus = tempLivedataBoolean
    val isPic: LiveData<Boolean> get() = _isPicOrPicus

    private val _picPicusText = tempLivedataString
    val picPicusText: LiveData<String> get() = _picPicusText

    val coPilot
        get() = tempLivedataBoolean

    val totalTime = tempLiveDataInt

    val ifrTime = tempLiveDataInt

    val nightTime = tempLiveDataInt

    private val _dualInstructorText = tempLivedataString
    val dualInstructorText: LiveData<String> get() = _dualInstructorText

    private val _dualInstructorActive = tempLivedataBoolean
    val dualInstructorActive: LiveData<Boolean> get() = _dualInstructorActive

    fun setIfrTime(enteredTime: String) {
        TODO("WIP")
    }

    fun setNightTime(enteredTime: String) {
        TODO("WIP")
    }

    /**
     * set correctedTotalTime
     * if autoValues is true: workingFlight will Set IFR and Night as same ratio
     * set autoValues to false of not 0
     */
    fun setTotalTimeOfFlight(enteredTime: String) {
        TODO("WIP")
    }

    /**
     * Toggle PICUS -> PIC -> NONE -> PICUS -> etc
     */
    fun togglePic() {
        TODO("WIP")
    }

    /*
     when {

        workingFlight.isPIC -> {
            workingFlight.isPIC = false
            workingFlight.isPICUS = false
        }
        workingFlight.isPICUS -> {
            workingFlight.isPIC = true
            workingFlight.isPICUS = false
        }
        else -> {
            workingFlight.isPICUS = true
            workingFlight.isPIC = false
        }
    }

     */



    /**
     * Toggle isCopilot
     * If Aircraft or isPic is changed, this will automatically be set again
     */
    fun toggleCopilot() {
        TODO("WIP")
    }

    /**
     * Toggle isDual - instructor - none
     */
    fun toggleDualInstructor() {
        TODO("WIP")
    }
    /*= when{
        workingFlight.isDual -> {
            workingFlight.isInstructor = true
            workingFlight.isDual = false
        }
        workingFlight.isInstructor -> {
            workingFlight.isInstructor = false
            workingFlight.isDual = false
        }
        else  -> {
            workingFlight.isInstructor = false
            workingFlight.isDual = true
        }
    }

     */

    /**
     * Gets the correct string for when flight is marked as PIC, PICUS or neither
     */
    private fun getPicPicusString(): String = getString(
        if(flightEditor.isPICUS) R.string.picus else R.string.pic
        ) ?: "ERROR: NO CONTEXT"

    /**
     * Gets the correct string for when flight is marked as dual, instructor or neither
     */
    private fun getDualInstructorString(): String = getString(when{
        flightEditor.isDual -> R.string.dualString
        flightEditor.isInstructor -> R.string.instructorString
        else -> R.string.dualInstructorString
    }) ?: "ERROR: NO CONTEXT"

    /**
     * Undo all changes made in this dialog
     */
}