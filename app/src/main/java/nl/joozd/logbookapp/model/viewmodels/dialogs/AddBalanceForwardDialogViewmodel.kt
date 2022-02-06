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

import android.text.Editable
import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.Transformations
import androidx.lifecycle.viewModelScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.NonCancellable
import kotlinx.coroutines.launch
import nl.joozd.logbookapp.data.dataclasses.BalanceForward
import nl.joozd.logbookapp.data.repository.BalanceForwardRepository
import nl.joozd.logbookapp.model.feedbackEvents.FeedbackEvents.BalanceForwardDialogEvents
import nl.joozd.logbookapp.model.helpers.FlightDataEntryFunctions.hoursAndMinutesStringToInt
import nl.joozd.logbookapp.model.helpers.minutesToHoursAndMinutesString
import nl.joozd.logbookapp.model.viewmodels.JoozdlogDialogViewModel
import java.lang.NumberFormatException


/**
 * Viewmodel for AddBalanceForwardDialog Fragment
 * As this is only for inputting data, and not for retrieving, only limited feedback is required
 * TODO toch wel met livedata :/
 */
class AddBalanceForwardDialogViewmodel: JoozdlogDialogViewModel() {

    /**********************************************************************************************
     * Private parts
     **********************************************************************************************/

    private val _balanceForward = MutableLiveData(emptyBalanceForward())
    private var balanceForwardSetter: BalanceForward
        get() = _balanceForward.value!! // cannot be null as it is initialized with a non-null value and not touched directly
        set(it) { _balanceForward.value = it}

    /**
     * Update _balanceForward with given functions
     */
    private fun emptyBalanceForward(): BalanceForward = BalanceForward(-1, "", 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0)

    /**********************************************************************************************
     * Public parts
     **********************************************************************************************/


    /**
     * Observable values
     */

    val name: LiveData<String>
        get() = Transformations.map(_balanceForward) { it.logbookName }

    val multiPilot: LiveData<String>
        get() = Transformations.map(_balanceForward) { it.multiPilotTime.minutesToHoursAndMinutesString()}

    val totalTimeOfFlight: LiveData<String>
        get() = Transformations.map(_balanceForward) { it.aircraftTime.minutesToHoursAndMinutesString()}

    val landingDay: LiveData<String>
        get() = Transformations.map(_balanceForward) { it.landingDay.toString()}
    val landingNight: LiveData<String>
        get() = Transformations.map(_balanceForward) { it.landingNight.toString()}

    val nightTime: LiveData<String>
        get() = Transformations.map(_balanceForward) { it.nightTime.minutesToHoursAndMinutesString()}

    val ifrTime: LiveData<String>
        get() = Transformations.map(_balanceForward) { it.ifrTime.minutesToHoursAndMinutesString()}

    val picTime: LiveData<String>
        get() = Transformations.map(_balanceForward) { it.picTime.minutesToHoursAndMinutesString()}

    val copilotTime: LiveData<String>
        get() = Transformations.map(_balanceForward) { it.copilotTime.minutesToHoursAndMinutesString()}

    val dualTime: LiveData<String>
        get() = Transformations.map(_balanceForward) { it.dualTime.minutesToHoursAndMinutesString()}

    val instructorTime: LiveData<String>
        get() = Transformations.map(_balanceForward) { it.instructortime.minutesToHoursAndMinutesString()}

    val simTime: LiveData<String>
        get() = Transformations.map(_balanceForward) { it.simTime.minutesToHoursAndMinutesString()}




    val workingBalanceForward: BalanceForward
        get() = balanceForwardSetter

    /**
     * Force all values to a specific BalanceForward, for instance when preloading values or editing
     * an existing BalanceForward.
     */
    fun setWorkingBalanceForward(it: BalanceForward){
        balanceForwardSetter = it
        feedback(BalanceForwardDialogEvents.UPDATE_FIELDS)
    }

    fun setName(name: Editable?){
        name?.toString()?.let {
            balanceForwardSetter = balanceForwardSetter.copy(logbookName = it)
        } ?: feedback(BalanceForwardDialogEvents.NUMBER_PARSE_ERROR)
    }

    fun setMultipilotTime(it: Editable?){
        hoursAndMinutesStringToInt(it?.toString())?.let{
            balanceForwardSetter = balanceForwardSetter.copy(multiPilotTime = it)
        } ?: feedback(BalanceForwardDialogEvents.NUMBER_PARSE_ERROR)
    }
    fun setTotalTime(it: Editable?){
        hoursAndMinutesStringToInt(it?.toString())?.let{
            balanceForwardSetter = balanceForwardSetter.copy(aircraftTime = it)
        } ?: feedback(BalanceForwardDialogEvents.NUMBER_PARSE_ERROR)
    }

    fun setLandingsDay(it: Editable?){
        try{
            val landings = it?.toString()!!.toInt()
            balanceForwardSetter = balanceForwardSetter.copy(landingDay = landings)
        }
        catch (nfe: NumberFormatException) {
            feedback(BalanceForwardDialogEvents.NUMBER_PARSE_ERROR)
        }
        catch (npe: NullPointerException){
            feedback(BalanceForwardDialogEvents.NUMBER_PARSE_ERROR)
        }
    }
    fun setLandingsNight(it: Editable?){
        try{
            val landings = it?.toString()!!.toInt()
            balanceForwardSetter = balanceForwardSetter.copy(landingNight = landings)
        }
        catch (nfe: NumberFormatException) {
            feedback(BalanceForwardDialogEvents.NUMBER_PARSE_ERROR)
        }
        catch (npe: NullPointerException){
            feedback(BalanceForwardDialogEvents.NUMBER_PARSE_ERROR)
        }
    }

    fun setNightTime(it: Editable?){
        hoursAndMinutesStringToInt(it?.toString())?.let{
            balanceForwardSetter = balanceForwardSetter.copy(nightTime = it)
        } ?: feedback(BalanceForwardDialogEvents.NUMBER_PARSE_ERROR)
    }

    fun setIfrTime(it: Editable?){
        hoursAndMinutesStringToInt(it?.toString())?.let{
            balanceForwardSetter = balanceForwardSetter.copy(ifrTime = it)
        } ?: feedback(BalanceForwardDialogEvents.NUMBER_PARSE_ERROR)
    }

    fun setPicTime(it: Editable?){
        hoursAndMinutesStringToInt(it?.toString())?.let{
            balanceForwardSetter = balanceForwardSetter.copy(picTime = it)
        } ?: feedback(BalanceForwardDialogEvents.NUMBER_PARSE_ERROR)
    }

    fun setCopilotTime(it: Editable?){
        hoursAndMinutesStringToInt(it?.toString())?.let{
            balanceForwardSetter = balanceForwardSetter.copy(copilotTime = it)
        } ?: feedback(BalanceForwardDialogEvents.NUMBER_PARSE_ERROR)
    }

    fun setDualTime(it: Editable?){
        hoursAndMinutesStringToInt(it?.toString())?.let{
            balanceForwardSetter = balanceForwardSetter.copy(dualTime = it)
        } ?: feedback(BalanceForwardDialogEvents.NUMBER_PARSE_ERROR)
    }

    fun setInstructorTime(it: Editable?){
        hoursAndMinutesStringToInt(it?.toString())?.let{
            balanceForwardSetter = balanceForwardSetter.copy(instructortime = it)
        } ?: feedback(BalanceForwardDialogEvents.NUMBER_PARSE_ERROR)
    }

    fun setSimTime(it: Editable?){
        hoursAndMinutesStringToInt(it?.toString())?.let{
            balanceForwardSetter = balanceForwardSetter.copy(simTime = it)
        } ?: feedback(BalanceForwardDialogEvents.NUMBER_PARSE_ERROR)
    }

    fun saveBalanceForward(){
        viewModelScope.launch(Dispatchers.IO + NonCancellable) {
            BalanceForwardRepository.instance.save(balanceForwardSetter)
            feedback(BalanceForwardDialogEvents.CLOSE_DIALOG)
        }
    }



}
