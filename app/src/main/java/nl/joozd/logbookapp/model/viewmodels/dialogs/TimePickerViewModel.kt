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

import android.util.Log
import androidx.lifecycle.Transformations
import nl.joozd.logbookapp.R
import nl.joozd.logbookapp.data.dataclasses.Airport
import nl.joozd.logbookapp.data.miscClasses.Crew
import nl.joozd.logbookapp.model.helpers.FeedbackEvents.TimePickerEvents
import nl.joozd.logbookapp.model.helpers.FlightDataEntryFunctions.hoursAndMinutesStringToInt
import nl.joozd.logbookapp.model.helpers.FlightDataPresentationFunctions.minutesToHoursAndMinutesString
import nl.joozd.logbookapp.model.viewmodels.JoozdlogDialogViewModel
import nl.joozd.logbookapp.utils.TwilightCalculator
import java.time.format.DateTimeFormatter

/**
 * Does not support changing orig or dest during this dialog open
 */
class TimePickerViewModel: JoozdlogDialogViewModel(){


    private var twilightCalculator: TwilightCalculator? = null
    private val timeFormatter = DateTimeFormatter.ofPattern("HH:mm")
    private val orig: Airport?
        get() = workingFlightRepository.origin.value
    private val dest: Airport?
        get() = workingFlightRepository.destination.value


    val augmentedCrew = Transformations.map(flight){ Crew.of(it.augmentedCrew).crewSize > 2}
    val pic = Transformations.map(flight) { it.isPIC || it.isPICUS}
    val coPilot = Transformations.map(flight) { it.isCoPilot }
    val dual = Transformations.map(flight) { it.isDual }
    val instructor = Transformations.map(flight) { it.isInstructor }

    val totalTime = Transformations.map(flight) {
        minutesToHoursAndMinutesString(
            if (it.correctedTotalTime != 0) it.correctedTotalTime
            else it.duration()
        )
    }


    val ifrTime = Transformations.map(flight){minutesToHoursAndMinutesString(it.ifrTime)}
    fun setIfrTime(enteredTime: String){
        Log.d("ifrTime", "setting to $enteredTime")
        hoursAndMinutesStringToInt(enteredTime).let{
            Log.d("ifrTime", "int = $it")
            when{
                it == null -> feedback(TimePickerEvents.INVALID_IFR_TIME) // previous time can be found in ifrTime.value
                it > workingFlight?.duration() ?: 0 -> feedback(TimePickerEvents.IFR_TIME_GREATER_THAN_DURATION)
                else -> workingFlight?.let { f ->
                    if (f.ifrTime != it) workingFlight = f.copy(ifrTime = it, autoFill = false)
                    //TODO this might need some tweaking. Tired now, cannot think.
                }
            }
        }

    }

    val nightTime = Transformations.map(flight){minutesToHoursAndMinutesString(it.nightTime)}
    fun setNightTime(enteredTime: String){
        hoursAndMinutesStringToInt(enteredTime).let{
            when{
                workingFlight == null -> feedback(TimePickerEvents.FLIGHT_IS_NULL)
                it == null -> feedback(TimePickerEvents.INVALID_NIGHT_TIME) // previous time can be found in ifrTime.value
                it > workingFlight!!.duration() -> feedback(TimePickerEvents.NIGHT_TIME_GREATER_THAN_DURATION)
                else -> workingFlight?.let { f -> if (f.nightTime != it) workingFlight = f.copy(nightTime = it, autoFill = false) }
            }
        }
    }

    /**
     * set correctedTotalTime
     * if autoValues is true: Set IFR and Night as same ratio
     * set autoValues to false
     *
     */
    fun setTotalTimeOfFlight(enteredTime: String){
        hoursAndMinutesStringToInt(enteredTime).let{
            when{
                workingFlight == null -> feedback(TimePickerEvents.FLIGHT_IS_NULL)
                it == null -> feedback(TimePickerEvents.INVALID_TOTAL_TIME) // previous time can be found in ifrTime.value
                it > workingFlight!!.duration() -> feedback(TimePickerEvents.TOTAL_TIME_GREATER_THAN_DURATION)
                else -> workingFlight?.let { f -> workingFlight = f.copy(correctedTotalTime = it) }
            }
        }
    }


    fun togglePic(){
        feedback(TimePickerEvents.NOT_IMPLEMENTED)
    }

    fun toggleCopilot(){
        feedback(TimePickerEvents.NOT_IMPLEMENTED)
    }

    fun toggleDual(){
        feedback(TimePickerEvents.NOT_IMPLEMENTED)
    }

    fun toggleInstructor(){
        feedback(TimePickerEvents.NOT_IMPLEMENTED)
    }




}