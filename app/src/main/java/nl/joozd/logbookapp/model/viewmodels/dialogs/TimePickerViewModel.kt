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
        get() = workingFlightRepository.orig.value
    private val dest: Airport?
        get() = workingFlightRepository.dest.value


    val sim = Transformations.map(flight) { it.isSim }
    private val showSimValues: Boolean
        get() = sim.value ?: false.also{
            feedback(TimePickerEvents.FLIGHT_IS_NULL)
        } // won't work if null anyway

    val hourOut = Transformations.map(flight){if (showSimValues) it.simTime / 60 else it.tOut().hour}
    val minuteOut = Transformations.map(flight){if (showSimValues) it.simTime % 60 else it.tOut().minute}

    val hourIn = Transformations.map(flight){it.tIn().hour}
    val minuteIn = Transformations.map(flight){it.tIn().minute}

    val augmentedCrew = Transformations.map(flight){ Crew.of(it.augmentedCrew).crewSize > 2}
    val autoValues = Transformations.map(flight){ it.autoFill }

    val tOutTextResource = Transformations.map(flight){if (showSimValues) R.string.simtTime else R.string.timeOut}

    val ifrTime = Transformations.map(flight){minutesToHoursAndMinutesString(it.ifrTime)}
    fun setIfrTime(enteredTime: String){
        hoursAndMinutesStringToInt(enteredTime).let{
            when{
                it == null -> feedback(TimePickerEvents.INVALID_IFR_TIME) // previous time can be found in ifrTime.value
                it > workingFlight?.duration() ?: 0 -> feedback(TimePickerEvents.IFR_TIME_GREATER_THAN_DURATION)
                else -> workingFlight?.let { f -> workingFlight = f.copy(ifrTime = it) }
            }
        }

    }

    val nightTime = Transformations.map(flight){minutesToHoursAndMinutesString(it.nightTime)}
    fun setNightTime(enteredTime: String){
        hoursAndMinutesStringToInt(enteredTime).let{
            when{
                it == null -> feedback(TimePickerEvents.INVALID_NIGHT_TIME) // previous time can be found in ifrTime.value
                it > workingFlight?.duration() ?: 0 -> feedback(TimePickerEvents.NIGHT_TIME_GREATER_THAN_DURATION)
                else -> workingFlight?.let { f -> workingFlight = f.copy(nightTime = it) }
            }
        }
    }


    fun timeOutPicked(hours: Int = hourOut.value ?: 0, minutes: Int = minuteOut.value ?: 0){
        feedback(TimePickerEvents.NOT_IMPLEMENTED)
        /* val timeOut = LocalDateTime.of(
            flight.tOut.toLocalDate(),
            LocalTime.of(newVal, flight.tOut.minute)
        )
        val timeIn =
            when {
                flight.tIn.minusDays(1) > timeOut -> flight.tIn.minusDays(1)
                flight.tIn > timeOut -> flight.tIn
                else -> flight.tIn.plusDays(1)
            }
        flight = flight.copy(
            timeOut = timeOut.toInstant(ZoneOffset.UTC).epochSecond,
            timeIn = timeIn.toInstant(ZoneOffset.UTC).epochSecond
        )*/
    }

    fun timeInPicked(hours: Int = hourIn.value ?: 0, minutes: Int = minuteIn.value ?: 0){
        feedback(TimePickerEvents.NOT_IMPLEMENTED)
        /*
                        val timeToCheck = LocalDateTime.of(
                    flight.tOut.toLocalDate(),
                    LocalTime.of(newVal, flight.tIn.minute)
                )
                val timeIn = when {
                    timeToCheck.minusDays(1) > flight.tOut -> timeToCheck.minusDays(1)
                    timeToCheck > flight.tOut -> timeToCheck
                    else -> timeToCheck.plusDays(1)
                }
                flight = flight.copy(timeIn = timeIn.toInstant(ZoneOffset.UTC).epochSecond)
         */
    }

    fun setAutoValues(autovalues: Boolean){
        workingFlight?.let{
            workingFlight = it.copy(autoFill = autovalues)
        }
    }



}