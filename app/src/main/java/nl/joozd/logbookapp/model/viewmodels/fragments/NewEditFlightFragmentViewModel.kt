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

package nl.joozd.logbookapp.model.viewmodels.fragments

import android.text.Editable
import android.util.Log
import androidx.lifecycle.*
import kotlinx.coroutines.Job
import kotlinx.coroutines.launch
import nl.joozd.logbookapp.data.dataclasses.Aircraft
import nl.joozd.logbookapp.data.dataclasses.Airport
import nl.joozd.logbookapp.data.sharedPrefs.Preferences
import nl.joozd.logbookapp.extensions.*
import nl.joozd.logbookapp.model.feedbackEvents.FeedbackEvents.EditFlightFragmentEvents
import nl.joozd.logbookapp.model.helpers.FlightDataEntryFunctions.hoursAndMinutesStringToInt
import nl.joozd.logbookapp.model.viewmodels.JoozdlogViewModel
import nl.joozd.logbookapp.model.workingFlight.WorkingFlight
import nl.joozd.logbookapp.R
import nl.joozd.logbookapp.data.repository.helpers.isSameFlightAs
import java.time.Instant
import java.time.LocalDate
import java.time.LocalTime


class NewEditFlightFragmentViewModel: JoozdlogViewModel() {
    private val wf = flightRepository.getWorkingFlight()

    private val _title = MutableLiveData<String>(context.getString(if(wf.newFlight) R.string.add_flight else R.string.edit_flight))

    // If this is true, no more windows should be opened
    private var closing: Boolean = false


        /**
     * MediatorLiveData
     */


    /**
     * MediatorLiveData
     */

    private val _aircraft = MediatorLiveData<String>().apply{
        addSource(wf.aircraftLiveData) { ac -> value = (if (sim) ac?.type?.shortName else ac?.toString()) ?: NO_DATA_STRING}
        addSource (wf.isSimLiveData) { sim -> value = (if (sim) wf.aircraftLiveData.value?.type?.shortName else wf.aircraftLiveData.value?.toString()) ?: NO_DATA_STRING}
    }

    private val _dualInstructor = MediatorLiveData<Int>().apply{
        value = DUAL_INSTRUCTOR_FLAG_NONE
        addSource(wf.isDualLiveData) {
            isDual -> value = when {
                isDual -> DUAL_INSTRUCTOR_FLAG_DUAL
                wf.isInstructorLiveData.value == true -> DUAL_INSTRUCTOR_FLAG_INSTRUCTOR
                else -> DUAL_INSTRUCTOR_FLAG_NONE
            }
        }
        addSource(wf.isInstructorLiveData) {
            isInstructor -> value = when {
                isInstructor -> DUAL_INSTRUCTOR_FLAG_INSTRUCTOR
                wf.isDualLiveData.value == true -> DUAL_INSTRUCTOR_FLAG_DUAL
                else -> DUAL_INSTRUCTOR_FLAG_NONE
            }
        }
    }

    //Transformations.map(wf.aircraft)
    /**
     * Observables
     */
    // this will cause nullpointerexception if not set
    // However, fragment should only be
    val date = Transformations.map(wf.dateLiveData){ it.toDateString() ?: NO_DATA_STRING }
    val localDate
            get() = wf.mDate
    val flightNumber
            get() = wf.flightNumberLiveData
    val origin = Transformations.map(wf.originLiveData) { getAirportString(it)}
    val destination = Transformations.map(wf.destinationLiveData) { getAirportString(it)}
    val originIsValid = Transformations.map(wf.originLiveData){ it != null && it.latitude_deg != 0.0 && it.longitude_deg != 0.0}
    val destinationIsValid = Transformations.map(wf.destinationLiveData){ it != null && it.latitude_deg != 0.0 && it.longitude_deg != 0.0}
    val timeOut = Transformations.map(wf.timeOutLiveData) { it.toTimeString()}
    val timeIn = Transformations.map(wf.timeInLiveData) { it.toTimeString()}
    val landings = Transformations.map(wf.takeoffLandingsLiveData){it.toString()}
    val aircraft: LiveData<String>
        get() = _aircraft
    val name
        get() = wf.nameLiveData
    val name2
        get() = wf.name2LiveData
    val allNames
        get() = flightRepository.allNames
    val remarks
        get() = wf.remarksLiveData
    val ifrTime
        get() = wf.ifrTimeLiveData
    val simTime
        get() = wf.simTimeLiveData
    val nightTime
        get() = wf.nightTimeLiveData
    val multiPilotTime
        get() = wf.multiPilotTimeLiveData
    val isSim
        get() = wf.isSimLiveData
    val sim: Boolean    // val to check if flight is sim
        get()= isSim.value ?: false

    val isSigned
        get() = wf.isSignedLiveData
    val signature: String
        get() = wf.signatureLiveData.value ?: ""
    /*val isDual
        get() = wf.isDualLiveData
    val isInstructor
        get() = wf.isInstructorLiveData
    */
    /**
     * Livedata keeps track of if [wf] is logged as Dual, Instructor or neither.
     */
    val dualInstructor: LiveData<Int>
        get() = _dualInstructor

    /**
     * emits true if wf.multipilotTime is not zero
     */
    val isMultiPilot = wf.multiPilotTimeLiveData.map { it > 0 }
    val isIfr
        get() = wf.isIfrLiveData
    val isPic
        get() = wf.isPicLiveData
    val isPF
        get() = wf.isPFLiveData
    val isAutoValues
        get() = wf.isAutoValuesLiveData
    val knownRegistrations
        get() = aircraftRepository.registrationsLiveData

    val title: LiveData<String>
        get() = _title


    /**
     * Data entry functions
     */

    /**
     * Set date
     * @param newDate: Date as [LocalDate]
     */
    fun setDate(newDate: LocalDate?){
        newDate?.let { wf.setDate(it) }
            ?: Log.w(this::class.simpleName, "setDate() trying to set date as null")
    }

    /**
     * Set FlightNumber
     */
    fun setFlightNumber(newFlightNumber: Editable?){
        newFlightNumber?.toString()?.let{
            if (it != (wf.flightNumberLiveData.value ?: "").removeTrailingDigits())
                wf.setFlightNumber(it)
            else wf.setFlightNumber(wf.flightNumberLiveData.value ?: "")
        }
    }

    /**
     * Set origin.
     * Checks if entered data is found in airportRepository.
     * If it is not found, it will enter it "as is" as identifier (ICAO code)
     */
    private fun setOrig(origString: String) = wf.setOrig(origString)


    fun setOrig(origEditable: Editable?) = origEditable?.let { setOrig(it.toString()) }

    /**
     * Set destination.
     * Checks if entered data is found in airportRepository.
     * If it is not found, it will enter it "as is" as identifier (ICAO code)
     */
    private fun setDest(destString: String) = wf.setDest(destString)

    fun setDest(destEditable: Editable?) = destEditable?.let {setDest (it.toString())}


    /**
     * Set departure time
     * If flight is sim, this is used for entering simTime because of reasons.
     * TODO I might want to change that and make a dedicated box for that in EditFlightFragmentLayout
     * Works for now.
     */
    fun setTimeOut(timeString: Editable?){
        wf.setTimeOut(makeTimeFromTimeString(timeString.toString()))
    }

    /**
     * Set arrival time
     */
    fun setTimeIn(timeString: Editable?){
        wf.setTimeIn(makeTimeFromTimeString(timeString.toString()))
    }

    fun setSimTime(simTimeString: Editable?){
        wf.setSimTime(hoursAndMinutesStringToInt(simTimeString.toString()) ?: return)
    }

    /**
     * Set registration and type from regAndTypeString.
     * - If [sim] it saves the whole thing as type
     * - if no '(' in [regAndTypeString] it assumes all is registration.
     * Else, it will save exactly `reg` and `type` in `reg(type)`.
     * Closing bracket is ignored. If no opening bracket, it is all reg.
     */
    @Suppress("MemberVisibilityCanBePrivate")
    fun setRegAndType(regAndTypeString: String){
        when {
            sim -> wf.setAircraft(type = regAndTypeString)                                 // simulator
            regAndTypeString.isBlank() -> wf.setAircraft(Aircraft(""))          // no reg and type if field is empty
            "(" !in regAndTypeString -> viewModelScope.launch {                            // only registration entered
                aircraftRepository.getBestHitForPartialRegistration(regAndTypeString)?.let {
                    wf.setAircraft(it)
                } ?: feedback(EditFlightFragmentEvents.AIRCRAFT_NOT_FOUND).apply {
                    putString(regAndTypeString)
                }.also {
                    wf.setAircraft(regAndTypeString)
                }
            }

            else -> { // If a ( or ) in [regAndTypeString] it will save exactly [reg] and [type] in [reg]([type]). Closing bracket is ignored. If no opening bracket, it is all reg.
                val reg: String?
                val type: String?
                regAndTypeString.filter { it != ')' }.split('(').let {
                    reg = it.firstOrNull()
                    type = it.getOrNull(1)
                }
                wf.setAircraft(reg, type)
            }
        }

    }

    fun setRegAndType(regAndTypeEditable: Editable) = setRegAndType(regAndTypeEditable.toString())

    /**
     * Set takeoff/landings from a string.
     * If '/' in string, it takes [WorkingFlight.takeoff]/[WorkingFlight.landing]
     * else it sets both takeoff and landing to that value.
     * [WorkingFlight] takes care of day/night
     * @param tlString: Takeoff/landing string. Can only consist of digits or '/'
     */
    fun setTakeoffLandings(tlString: String){
        require (tlString.all{ it in "1234567890/"})
        if ('/' in tlString) tlString.split('/').let{
            wf.takeoff = it[0].toInt()
            wf.landing = it[1].toInt()
        } else {
            wf.takeoff = tlString.toInt()
            wf.landing = tlString.toInt()
        }
    }

    /**
     * Set name. Will auto-complete names if it doesn't end with ';'
     * @see [String.anyWordStartsWith]
     */
    fun setName(name: String){
        if (';' in name) wf.setName(name.dropLast(1))
        else wf.setName(if (name.isEmpty()) "" else allNames.value?.firstOrNull{it.anyWordStartsWith(name, ignoreCase = true)} ?: name)
    }

    /**
     * Set name2. Will auto-complete names.
     * Names can be separated by ';', in which case they will be trimmed and not autocompleted
     * (you can also use this to enter an exact name if a longer one exists ie. "jan"jans" might becom "jan janssen" where "jan jans;" will be "jan jans")
     * @see [String.anyWordStartsWith]
     */
    fun setName2(name2: String){
        if (';' in name2) wf.setName2(name2.split(';').joinToString(";") { it.trim() })
        else wf.setName2(if (name2.isEmpty()) "" else allNames.value?.firstOrNull{it.anyWordStartsWith(name2, ignoreCase = true)} ?: name2)

    }


    /**
     * Set remarks
     */
    fun setRemarks(remarks: String) = wf.setRemarks(remarks)

    /**
     * Set sim
     * @param force: Can be used to force sim on or off. If not given or null, it sets it to what it currently is not
     */
    fun toggleSim(force: Boolean? = null) = wf.setIsSim ( force ?: !sim) // [sim] comes straight from [wf] so it is a true toggle

    /**
     * Set signature
     */
    fun setSignature(signature: String) = wf.setSignature(signature)

    /*
    /**
     * Set dual
     * @param force: Can be used to force sim on or off. If not given or null, it sets it to what it currently is not (or to true if it is null for some reason)
     */
    fun toggleDual(force: Boolean? = null) = wf.setIsDual ( force ?: wf.isDualLiveData.value == false)

    /**
     * Set instructor
     * @param force: Can be used to force sim on or off. If not given or null, it sets it to what it currently is not (or to true if it is null for some reason)
     */
    fun toggleInstructor(force: Boolean? = null) = wf.setIsInstructor ( force ?: wf.isInstructorLiveData.value == false)
    */

    /**
     * Toggle between Dual, Instructor and None.
     */
    fun toggleDualInstructor(){
        when (dualInstructor.value){
            DUAL_INSTRUCTOR_FLAG_NONE -> wf.setIsDual(true)
            DUAL_INSTRUCTOR_FLAG_DUAL -> {
                wf.setIsDual(false)
                wf.setIsInstructor(true)
            }
            else -> {
                wf.setIsDual(false)
                wf.setIsInstructor(false)
            }
        }
    }

    /**
     * Set multiPilot. If multipilot time was 0, [wf] sets multiPilotTime to mDuration.toMinutes().toInt()
     */
    fun toggleMultiPilot(force: Boolean? = null){
        wf.setAutoValues(false)
        wf.setIsMultipilot(force ?: wf.mMultiPilotTime == 0)
    }

    /**
     * Set IFR
     * workingFlight will take care of also adjusting ifrTime (if autovalues)
     * @param force: Can be used to force sim on or off. If not given or null, it sets it to what it currently is not (or to true if it is null for some reason)
     */
    fun toggleIfr(force: Boolean? = null){
        wf.setIsIfr(force ?: wf.isIfrLiveData.value == false)
    }

    /**
     * Toggle PIC
     * @param force: Can be used to force sim on or off. If not given or null, it sets it to what it currently is not (or to true if it is null for some reason)
     */
    fun togglePic(force: Boolean? = null){
        wf.setIsPic(force ?: wf.isPicLiveData.value == false)
    }

    /**
     * Toggle PF.
     * If augmentedCrew.crewSize > 2, WorkingFlight will recalculate times (if autovalues)
     * @param force: Can be used to force sim on or off. If not given or null, it sets it to what it currently is not (or to true if it is null for some reason)
     */
    fun togglePF(force: Boolean? = null){
        val newValue = force ?: wf.isPFLiveData.value == false
        wf.setIsPF(newValue)
        if (wf.isAutoValuesLiveData.value == true) {
            wf.takeoff = newValue.toInt() // Boolean.toInt() is 1 if true or 0 if false
            wf.landing = newValue.toInt()
        }
    }

    //If this is true, if autoValues is off, the only reason for that is [checkAutovaluesForUnknownAirport] set it to off.
    //If checkAutovaluesForUnknownAirport decides it's ok again,  autoValues can be set to on again
    private var autoValuesOnlyOffBecauseOfUnknownAirport: Boolean = wf.isAutoValuesLiveData.value == true
    /**
     * Will set autovalues to "soft-off" if not both airports known
     */
    fun checkAutovaluesForUnknownAirport(){
        val unknownAirportFound = originIsValid.value == false || destinationIsValid.value == false
        println("BOTERHAM ${originIsValid.value} / ${destinationIsValid.value} - av is ${wf.isAutoValuesLiveData.value}")
        if (!unknownAirportFound && autoValuesOnlyOffBecauseOfUnknownAirport){              // autovalues off because unknown airports, but all airports known
            toggleAutoValues(true)                                                // force autoValues on
            autoValuesOnlyOffBecauseOfUnknownAirport = false                            // it is not off so this should be false
        }
        if (unknownAirportFound && wf.mIsAutovalues){                        // autovalues wants to be on, but unknown airports prevet that
            autoValuesOnlyOffBecauseOfUnknownAirport = true                             // We switch off Autovalues for this reason
            toggleAutoValues(false)                                               // force autoValues off. If airports are all known again, calling this function again will reset it to on
            println("BANAAAAAAAN")
        }
    }

    /**
     * Toggle auto values.
     * Used by "autoValues" checkbox without parameter
     * Used by [checkAutovaluesForUnknownAirport] with parameter
     */
    fun toggleAutoValues(force: Boolean? = null){
        val newValue = force ?: (!wf.mIsAutovalues)
        viewModelScope.launch {
            val j: Job = wf.setAutoValues(newValue)
            j.join()
            if (force == null) { // set fr0m checkbox,
                checkAutovaluesForUnknownAirport() // check airports known to see if we can set it to on
            }
        }
    }

    /**
     * Save WorkingFlight and send Close message to fragment.
     * If this edits a flight that doesn't end up as completed, it will either:
     *      - push back calendar sync if offBlocks is less than 30 minutes in the future
     *      - Feed back to main activity that this gives a possible sync problem
     *          -> This feedback is given instead of CLOSE_EDIT_FLIGHT_FRAGMENT so user can decide to continue editing.
     */

    fun saveAndClose() {
        viewModelScope.launch {
            wf.waitForAsyncWork()
            if (wf.isPlanned && wf.canCauseCalendarConflict) {
                val now = Instant.now()
                // Push back calendar sync if timeIn in future
                if (wf.mTimeIn > now) {
                    if (wf.mTimeOut <= now.plusMinutes(30)) {
                        postponeCalendarSync()
                    }
                    //This happens if flight starts > 30 minutes in the future and calendar sync is active during this flight
                    else if (Preferences.calendarDisabledUntil < wf.mTimeIn.epochSecond) {
                        feedback(EditFlightFragmentEvents.EDIT_FLIGHT_CALENDAR_CONFLICT)
                        return@launch
                    }

                }
                //else { /*no problem, no action*/ }
            }

            wf.saveAndClose()
            feedback(EditFlightFragmentEvents.CLOSE_EDIT_FLIGHT_FRAGMENT)
        }
    }

    /**
     * Close workingFlight without saving
     */
    fun close(){
        notifyClosing()                                                     // this makes sure any pending dialogs don't get opened anymore
        flightRepository.closeWorkingFlight()                               // This makes sure fligth doesn't reopen again on recreate activity (ie. rotate)
        feedback(EditFlightFragmentEvents.CLOSE_EDIT_FLIGHT_FRAGMENT)       // This tells fragment to close itself
    }

    /**
     * Let the viewModel know the Fragment is about to close itself
     */
    fun notifyClosing(){
        closing = true
    }


    /**
     * Check if
     */
    fun checkIfStillOpen(): Boolean = !closing

    /**
     * Disables Calendar Sync alltogether
     */
    fun disableCalendarSync(){
        Preferences.useCalendarSync = false
    }

    /**
     * Postpones Calendar Sync until max of:
     *      - 1 second after [WorkingFlight.mTimeIn]
     *      - current [Preferences.calendarDisabledUntil]
     *
     */
    fun postponeCalendarSync(){
        (wf.mTimeIn.epochSecond + 1).let {
            if (it > Preferences.calendarDisabledUntil) Preferences.calendarDisabledUntil = it
        }
    }

    /**
     * Helper functions
     */

    /**
     * Displays the airport as ICAO or IATA, depending on settings in Preferences.
     * If no IATA found, defaults to ICAO
     */
    private fun getAirportString(a: Airport?): String = when{
        a == null -> NO_DATA_STRING
        !Preferences.useIataAirports -> a.ident
        else -> a.iata_code.nullIfBlank() ?: a.ident
    }

    /**
     * Make a Local Time from a string with format "1234"
     * Any non-number characters are ignored (so you can use 12+34, 12:34, etc)
     * "34" will be 00:34
     * 12345 will be 12:34, same for 12:34:56
     * 17:60 will be 18:00, 17:70 18:10
     * @param s: String to parse
     * @return Local Time parsed from [s]
     */
    private fun makeTimeFromTimeString(s: String): LocalTime =
        s.filter{it.isDigit()}.padStart(4, '0').take(4).toInt().let{
            val hours = it/100 + (it%100)/60    // hours is first two digits plus one if last two digits >= 60
            val mins = (it%100)%60              // mins is last two digits % 60
            LocalTime.of(hours, mins)
        }



    companion object{
        const val NO_DATA_STRING = "…"

        const val DUAL_INSTRUCTOR_FLAG_NONE = 0
        const val DUAL_INSTRUCTOR_FLAG_DUAL = 1
        const val DUAL_INSTRUCTOR_FLAG_INSTRUCTOR = 2
    }

}