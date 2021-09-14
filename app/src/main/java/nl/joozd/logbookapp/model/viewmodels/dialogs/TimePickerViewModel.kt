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

import androidx.lifecycle.LiveData
import androidx.lifecycle.MediatorLiveData
import androidx.lifecycle.map
import nl.joozd.logbookapp.R
import nl.joozd.logbookapp.model.feedbackEvents.FeedbackEvents.TimePickerEvents
import nl.joozd.logbookapp.model.helpers.FlightDataEntryFunctions.hoursAndMinutesStringToInt
import nl.joozd.logbookapp.model.helpers.FlightDataPresentationFunctions.minutesToHoursAndMinutesString
import nl.joozd.logbookapp.model.viewmodels.JoozdlogDialogViewModelWithWorkingFlight

/**
 * Does not support changing orig or dest during this dialog open
 */
class TimePickerViewModel: JoozdlogDialogViewModelWithWorkingFlight() {

    /**
     * Livedata objects:
     */
    val isAugmentedCrew // LiveData<Boolean>
        get() = workingFlight.isAugmented
    private val _isPicOrPicus = MediatorLiveData<Boolean>().apply{
        addSource(workingFlight.isPICLiveData){
            value = it || workingFlight.isPICUS
        }
        addSource(workingFlight.isPICUSLiveData){
            value = it || workingFlight.isPIC
        }
    }
    val isPic: LiveData<Boolean> get() = _isPicOrPicus

    private val _picPicusText = MediatorLiveData<String>().apply{
        addSource(workingFlight.isPICLiveData){
            value = getPicPicusString()
        }
        addSource(workingFlight.isPICUSLiveData){
            value = getPicPicusString()
        }
    }
    val picPicusText: LiveData<String> get() = _picPicusText

    val coPilot
        get() = workingFlight.isCoPilotLiveData

    val totalTime = workingFlight.durationLiveData.map { minutesToHoursAndMinutesString(it) }

    val ifrTime = workingFlight.ifrTimeLiveData.map { minutesToHoursAndMinutesString(it) }

    val nightTime = workingFlight.nightTimeLiveData.map {minutesToHoursAndMinutesString(it)}

    private val _dualInstructorText = MediatorLiveData<String>().apply{
        addSource(workingFlight.isDualLiveData){
            value = getDualInstructorString()
        }
        addSource(workingFlight.isInstructorLiveData){
            value = getDualInstructorString()
        }
    }
    val dualInstructorText: LiveData<String> get() = _dualInstructorText

    private val _dualInstructorActive = MediatorLiveData<Boolean>().apply{
        addSource(workingFlight.isDualLiveData){
            value = it || workingFlight.isInstructor
        }
        addSource(workingFlight.isInstructorLiveData){
            value = it || workingFlight.isDual
        }
    }
    val dualInstructorActive: LiveData<Boolean> get() = _dualInstructorActive


    /**
     * Set IFR time to time parsed from [enteredTime], only if
     * - parsing worked
     * - that time != more than total duration
     */
    fun setIfrTime(enteredTime: String) =
        hoursAndMinutesStringToInt(enteredTime).let { ifrMinutes ->
            when {
                workingFlight.durationLiveData.value == null -> feedback(TimePickerEvents.FLIGHT_IS_NULL)
                ifrMinutes == null -> feedback(TimePickerEvents.INVALID_IFR_TIME) // previous time can be found in ifrTime.value
                ifrMinutes > workingFlight.duration -> feedback(TimePickerEvents.IFR_TIME_GREATER_THAN_DURATION).putInt(ifrMinutes)
                else -> {
                    workingFlight.autoFill = workingFlight.autoFill && (ifrMinutes == workingFlight.duration) // set autofill off if it was on and ifrTime is not entire flight
                    workingFlight.ifrTime = ifrMinutes
                }
            }
        }

    /**
     * Set night time to time parsed from [enteredTime], only if
     * - parsing worked
     * - that time != more than total duration
     */
    fun setNightTime(enteredTime: String){
        hoursAndMinutesStringToInt(enteredTime).let{
            when{
                workingFlight.durationLiveData.value == null -> feedback(TimePickerEvents.FLIGHT_IS_NULL)
                it == null -> feedback(TimePickerEvents.INVALID_NIGHT_TIME) // previous time can be found in ifrTime.value
                it > workingFlight.duration -> feedback(TimePickerEvents.NIGHT_TIME_GREATER_THAN_DURATION)
                else -> workingFlight.nightTime = it
            }
        }
    }

    /**
     * set correctedTotalTime
     * if autoValues is true: workingFlight will Set IFR and Night as same ratio
     * set autoValues to false of not 0
     */
    fun setTotalTimeOfFlight(enteredTime: String){
        hoursAndMinutesStringToInt(enteredTime).let{
            when{
                workingFlight.durationLiveData.value == null -> feedback(TimePickerEvents.FLIGHT_IS_NULL)
                enteredTime.isBlank() -> workingFlight.correctedTotalTime = 0
                it == null -> feedback(TimePickerEvents.INVALID_TOTAL_TIME) // previous time can be found in ifrTime.value
                else -> workingFlight.correctedTotalTime = it
            }
        }
    }

    /**
     * Toggle PICUS -> PIC -> NONE -> PICUS -> etc
     */
    fun togglePic() = when {
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



    /**
     * Toggle isCopilot
     * If Aircraft or isPic is changed, this will automatically be set again
     */
    fun toggleCopilot(){
        workingFlight.isCoPilot = !workingFlight.isCoPilot
    }

    /**
     * Toggle isDual - instructor - none
     */
    fun toggleDualInstructor() = when{
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

    /**
     * Gets the correct string for when flight is marked as PIC, PICUS or neither
     */
    private fun getPicPicusString(): String = getString(
        if(workingFlight.isPICUS) R.string.picus else R.string.pic
        )
    /**
     * Gets the correct string for when flight is marked as dual, instructor or neither
     */
    private fun getDualInstructorString(): String = getString(when{
        workingFlight.isDual -> R.string.dualString
        workingFlight.isInstructor -> R.string.instructorString
        else -> R.string.dualInstructorString
    })

    /**
     * Undo all changes made in this dialog
     */
}