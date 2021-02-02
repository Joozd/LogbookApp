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
import nl.joozd.logbookapp.model.feedbackEvents.FeedbackEvents.TimePickerEvents
import nl.joozd.logbookapp.model.helpers.FlightDataEntryFunctions.hoursAndMinutesStringToInt
import nl.joozd.logbookapp.model.helpers.FlightDataPresentationFunctions.minutesToHoursAndMinutesString
import nl.joozd.logbookapp.model.viewmodels.JoozdlogDialogViewModelWithWorkingFlight

/**
 * Does not support changing orig or dest during this dialog open
 */
class TimePickerViewModel: JoozdlogDialogViewModelWithWorkingFlight() {

    val augmentedCrew
        get() = workingFlight.crew
    val pic
        get() = workingFlight.isPic
    val coPilot
        get() = workingFlight.isCopilot
    val dual
        get() = workingFlight.isDual
    val instructor
        get() = workingFlight.isInstructor

    val totalTime = Transformations.map(workingFlight.duration) {
        minutesToHoursAndMinutesString(it.toMinutes())
    }


    val ifrTime = Transformations.map(workingFlight.ifrTime) { minutesToHoursAndMinutesString(it) }

    val nightTime = Transformations.map(workingFlight.nightTime){minutesToHoursAndMinutesString(it)}

    /**
     * Set IFR time to time parsed from [enteredTime], only if
     * - parsing worked
     * - that time != more than total duration
     */
    fun setIfrTime(enteredTime: String) =
        hoursAndMinutesStringToInt(enteredTime).let { ifrMinutes ->
            when {
                workingFlight.duration.value == null -> feedback(TimePickerEvents.FLIGHT_IS_NULL)
                ifrMinutes == null -> feedback(TimePickerEvents.INVALID_IFR_TIME) // previous time can be found in ifrTime.value
                ifrMinutes > workingFlight.duration.value!!.toMinutes() -> feedback(TimePickerEvents.IFR_TIME_GREATER_THAN_DURATION).putInt(ifrMinutes)
                else -> workingFlight.setIfrTime(ifrMinutes)
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
                workingFlight.duration.value == null -> feedback(TimePickerEvents.FLIGHT_IS_NULL)
                it == null -> feedback(TimePickerEvents.INVALID_NIGHT_TIME) // previous time can be found in ifrTime.value
                it > workingFlight.duration.value!!.toMinutes() -> feedback(TimePickerEvents.NIGHT_TIME_GREATER_THAN_DURATION)
                else -> workingFlight.setNightTime(it)
            }
        }
    }

    /**
     * set correctedTotalTime
     * if autoValues is true: Set IFR and Night as same ratio
     * set autoValues to false of not 0
     */
    fun setTotalTimeOfFlight(enteredTime: String){
        hoursAndMinutesStringToInt(enteredTime).let{
            when{
                workingFlight.duration.value == null -> feedback(TimePickerEvents.FLIGHT_IS_NULL)
                enteredTime.isBlank() -> workingFlight.setCorrectedTotalTime(0)
                it == null -> feedback(TimePickerEvents.INVALID_TOTAL_TIME) // previous time can be found in ifrTime.value
                it > workingFlight.duration.value!!.toMinutes() -> feedback(TimePickerEvents.TOTAL_TIME_GREATER_THAN_DURATION)
                else -> workingFlight.setCorrectedTotalTime(it)
            }
        }
    }

    /**
     * Toggle PIC
     * @param force: Can be used to force sim on or off. If not given or null, it sets it to what it currently is not (or to true if it is null for some reason)
     */
    fun togglePic(force: Boolean? = null){
        workingFlight.setIsPic(force ?: workingFlight.isPic.value == false)
    }

    /**
     * Toggle isCopilot
     * @param force: Can be used to force sim on or off. If not given or null, it sets it to what it currently is not (or to true if it is null for some reason)
     * If Aircraft or isPic is changed, this will automatically be set again
     */
    fun toggleCopilot(force: Boolean? = null){
        workingFlight.setIsCopilot(force ?: workingFlight.isCopilot.value == false)
    }

    /**
     * Toggle isDual
     * @param force: Can be used to force sim on or off. If not given or null, it sets it to what it currently is not (or to true if it is null for some reason)
     */
    fun toggleDual(force: Boolean? = null){
        workingFlight.setIsDual(force ?: workingFlight.isDual.value == false)
    }

    /**
     * Set instructor
     * @param force: Can be used to force sim on or off. If not given or null, it sets it to what it currently is not (or to true if it is null for some reason)
     */
    fun toggleInstructor(force: Boolean? = null) = workingFlight.setIsInstructor ( force ?: workingFlight.isInstructor.value == false)

    /**
     * Undo all changes made in this dialog
     */
}