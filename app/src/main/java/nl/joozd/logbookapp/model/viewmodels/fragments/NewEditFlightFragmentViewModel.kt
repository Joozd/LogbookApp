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

package nl.joozd.logbookapp.model.viewmodels.fragments

import android.text.Editable
import androidx.lifecycle.*
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.launch
import nl.joozd.logbookapp.data.dataclasses.Airport
import nl.joozd.logbookapp.data.sharedPrefs.Preferences
import nl.joozd.logbookapp.extensions.*
import nl.joozd.logbookapp.model.feedbackEvents.FeedbackEvents.EditFlightFragmentEvents
import nl.joozd.logbookapp.model.helpers.FlightDataEntryFunctions.hoursAndMinutesStringToInt
import nl.joozd.logbookapp.model.viewmodels.JoozdlogViewModel
import nl.joozd.logbookapp.R
import nl.joozd.logbookapp.data.dataclasses.Aircraft
import nl.joozd.logbookapp.data.dataclasses.FlightData
import nl.joozd.logbookapp.data.repository.flightRepository.FlightRepository
import nl.joozd.logbookapp.data.repository.helpers.findBestHitForRegistration
import nl.joozd.logbookapp.model.workingFlight.TakeoffLandings
import nl.joozd.logbookapp.model.workingFlight.WorkingFlight
import java.time.Instant
import java.time.LocalTime


class NewEditFlightFragmentViewModel: JoozdlogViewModel() {
    private val wf = flightRepository.getWorkingFlight()
    private val aircraftDataCache = aircraftRepository.getSelfUpdatingAircraftDataCache(viewModelScope)

    private var cachedSortedRegistrationsList: List<String> = emptyList()

    val airportDbLiveData = airportRepository.liveAirports
    val knownRegistrationsFlow = makeSortedRegistrationsFlowAndCacheIt().also{
        println("made $it")
    }

    val title: LiveData<String> = MutableLiveData(context.getString(if(wf.newFlight) R.string.add_flight else R.string.edit_flight))

    val dualInstructor: LiveData<Int> = makeDualInstructorMediatorLivedata()
    val aircraft: LiveData<String> = makeAircraftDisplayNameMediatorLiveData()
    val isPic: LiveData<Boolean> = makePicOrPicusMediatorLiveData()
    val picPicusText: LiveData<String> = makePicPicusTextMediatorLiveData()

    val dateStringLiveData = wf.timeOutLiveData.map{ makeDateString(it) }
    val flightNumberLiveData = wf.flightNumberLiveData
    val origin = Transformations.map(wf.originLiveData) { getAirportString(it)}
    val destination = Transformations.map(wf.destinationLiveData) { getAirportString(it)}
    val originIsValid = Transformations.map(wf.originLiveData){ it?.checkIfValidCoordinates() == true }
    val destinationIsValid = Transformations.map(wf.destinationLiveData){ it?.checkIfValidCoordinates() == true }
    val timeOut = wf.timeOutLiveData.map { Instant.ofEpochSecond(it).toTimeString()}
    val timeIn = wf.timeInLiveData.map { Instant.ofEpochSecond(it).toTimeString()}
    val landingsLiveData: LiveData<String>  = wf.takeoffLandingsLiveData.map { it.toString() }
    val nameLiveData = wf.nameLiveData
    val name2LiveData = wf.name2LiveData
    val allNamesLiveData = flightRepository.allNamesLiveData
    val remarksLiveData = wf.remarksLiveData
    val simTimeLiveData = wf.simTimeLiveData
    val isSimLiveData = wf.isSimLiveData
    val isMultiPilotLiveData = wf.isMultipilotLiveData
    val isIfrLiveData = wf.isIfrLiveData
    val isPFLiveData = wf.isPFLiveData
    val isAutoValuesLiveData = wf.autoFillLiveData

    val isSignedLiveData = wf.signatureLiveData.map { it.isNotBlank() }

    val signature: String
        get() = wf.signatureLiveData.value ?: ""
    val isSim: Boolean    // val to check if flight is sim
        get() = isSimLiveData.value ?: false

    val localDate
        get() = wf.date

    var closing: Boolean = false
        private set

    //If this is true, if autoValues is off, the only reason for that is [checkAutovaluesForUnknownAirport] set it to off.
    //If checkAutovaluesForUnknownAirport decides it's ok again,  autoValues can be set to on again
    private var autoValuesOnlyOffBecauseOfUnknownAirport: Boolean = wf.autoFill

    private fun makeDateString(epochSecond: Long) =
        Instant.ofEpochSecond(epochSecond).toLocalDate().toDateString()

    private fun makeAircraftDisplayNameMediatorLiveData() =
        MediatorLiveData<String>().apply {
            addSource(wf.aircraftLiveData) { value = getAircraftDisplayName() }
            addSource(wf.isSimLiveData) { value = getAircraftDisplayName() }
        }

    private fun makePicOrPicusMediatorLiveData() =
        MediatorLiveData<Boolean>().apply {
            addSource(wf.isPICLiveData) { value = wf.isPicOrPicus() }
            addSource(wf.isPICUSLiveData) { value = wf.isPicOrPicus() }
        }

    private fun makePicPicusTextMediatorLiveData() =
        MediatorLiveData<String>().apply {
            addSource(wf.isPICLiveData) { value = getPicOrPicusString() }
            addSource(wf.isPICUSLiveData) { value = getPicOrPicusString() }
        }


    private fun getAircraftDisplayName() =
        (if (isSim) wf.aircraftLiveData.value?.type?.shortName else wf.aircraftLiveData.value?.toString())
            ?: NO_DATA_STRING

    private fun makeDualInstructorMediatorLivedata() =
        MediatorLiveData<Int>().apply {
            value = DUAL_INSTRUCTOR_FLAG_NONE
            addSource(wf.isDualLiveData) {
                value = makeDualOrInstructorFlag()
            }
            addSource(wf.isInstructorLiveData) {
                value = makeDualOrInstructorFlag()
            }
        }

    private fun makeDualOrInstructorFlag() = when {
        wf.isDualLiveData.value == true -> DUAL_INSTRUCTOR_FLAG_DUAL
        wf.isInstructorLiveData.value == true -> DUAL_INSTRUCTOR_FLAG_INSTRUCTOR
        else -> DUAL_INSTRUCTOR_FLAG_NONE
    }

    private fun WorkingFlight.isPicOrPicus(): Boolean =
        isPIC || isPICUS


    fun setFlightNumber(newFlightNumber: Editable?){
        newFlightNumber?.toString()?.let{
            // If new flightnumber is not the old one minus all the digits (eg KL1234 becomes KL)
            if (it.isNotOldValueWithDigitsRemoved())
                wf.flightNumber = it
        }
    }

    // true if all digits are removed from wf.flightNumber and nothing else changed, eg. KL1234 became KL
    private fun String.isNotOldValueWithDigitsRemoved() =
        this != (wf.flightNumber).removeTrailingDigits()


    fun setOrig(origEditable: Editable?) = origEditable?.let { setOrig(it.toString()) }

    fun setDest(destEditable: Editable?) = destEditable?.let {setDest (it.toString())}

    private fun setOrig(origString: String) { wf.orig = origString }

    private fun setDest(destString: String) { wf.dest = destString }




    /**
     * Set departure time
     * If flight is sim, this is used for entering simTime because of reasons.
     * TODO I might want to change that and make a dedicated box for that in EditFlightFragmentLayout
     * Works for now.
     */
    fun setTimeOut(timeString: Editable?){
        wf.setTimeOut(makeTimeFromTimeString(timeString.toString()))
    }


    fun setTimeIn(timeString: Editable?){
        wf.setTimeIn(makeTimeFromTimeString(timeString.toString()))
    }

    fun setSimTime(simTimeString: Editable?){
        hoursAndMinutesStringToInt(simTimeString.toString())?.let { wf.simTime = it }
    }

    private fun setRegAndType(regAndTypeString: String){
        when {
            isSim -> wf.aircraftType = regAndTypeString                                     // simulator
            regAndTypeString.isBlank() -> wf.registration = ""                              // no reg and type if field is empty
            "(" !in regAndTypeString -> searchRegistrationAndSaveInWorkingFlight(regAndTypeString)   // only registration entered
            else -> {                                                                       // If a ( or ) in [regAndTypeString] it will save exactly [reg] and [type] in [reg]([type]). Closing bracket is ignored. If no opening bracket, it is all reg.
                saveRegAndTypeInWorkingFlight(regAndTypeString)
            }
        }
    }

    private fun saveRegAndTypeInWorkingFlight(regAndTypeString: String) {
        require ('(' in regAndTypeString) { "Don't call saveRegAndTypeInWorkingFlight on a String without an opening parenthesis"}
        val reg: String?
        val type: String?
        regAndTypeString.filter { it != ')' }
            .split('(').let {
                reg = it.firstOrNull()
                type = it.lastOrNull()
            }
        wf.setAircraft(reg, type)
    }

    //TODO this has work that should be done in WorkingFlight
    private fun searchRegistrationAndSaveInWorkingFlight(regAndTypeString: String) {
        viewModelScope.launch {
            val bestRegistrationHit =
                getBestHitForPartialRegistration(regAndTypeString)
            if (bestRegistrationHit == null) {
                feedback(EditFlightFragmentEvents.AIRCRAFT_NOT_FOUND).putString(regAndTypeString)
                wf.registration = regAndTypeString
            } else
                wf.setAircraftHard(bestRegistrationHit)
        }
    }

    fun setRegAndType(regAndTypeEditable: Editable) = setRegAndType(regAndTypeEditable.toString())

    //tlString is expected to have only numbers or slashes
    fun setTakeoffLandings(tlString: String){
        if ('/' in tlString) {
            val parts = tlString.split('/')
            wf.takeoffLandings = TakeoffLandings(parts[0].toInt(), parts[1].toInt())
        } else {
            wf.takeoffLandings = TakeoffLandings(tlString.toInt())
        }
    }

    fun setName(name: String){
        wf.name = autoCompleteNameIfNotEndingWithSemicolon(name)
    }

    fun setName2(name2: String){
        wf.name2 = autoCompleteOrCleanNames(name2)
    }

    private fun autoCompleteOrCleanNames(name2: String) = when {
        ';' in name2 -> removeSpacesAroundSemicolons(name2)
        name2.isEmpty() -> ""
        else -> autoCompleteName(name2) ?: name2
    }

    private fun autoCompleteName(name: String) =
        allNamesLiveData.value?.firstOrNull { it.anyWordStartsWith(name, ignoreCase = true) }

    private fun autoCompleteNameIfNotEndingWithSemicolon(name: String) = when {
        ';' in name -> name.dropLast(1)
        name.isEmpty() -> ""
        else -> autoCompleteName(name) ?: name
    }

    private fun removeSpacesAroundSemicolons(name2: String) =
        (name2.split(';').joinToString(";") { it.trim() })


    fun setRemarks(remarks: String) {
        wf.remarks = remarks
    }

    fun toggleSim(force: Boolean? = null) {
        wf.isSim = force ?: !isSim
    }

    fun toggleDualInstructorNone(){
        when (dualInstructor.value){
            DUAL_INSTRUCTOR_FLAG_NONE -> wf.isDual = true
            DUAL_INSTRUCTOR_FLAG_DUAL -> {
                wf.isDual = false
                wf.isInstructor = true
            }
            else -> {
                wf.isDual = false
                wf.isInstructor =false
            }
        }
    }

    fun toggleMultiPilot(){
        wf.isMultipilot = !wf.isMultipilot
    }

    fun toggleIfr(force: Boolean? = null){
        wf.isIfr = force ?: !wf.isIfr
    }

    fun togglePicusPicNone() = when {
        wf.isPIC -> {
            wf.isPIC = false
            wf.isPICUS = false
        }
        wf.isPICUS -> {
            wf.isPIC = true
            wf.isPICUS = false
        }
        else -> {
            wf.isPICUS = true
            wf.isPIC = false
        }
    }

    fun togglePF(force: Boolean? = null){
        val newValue = force ?: wf.isPFLiveData.value == false
        wf.isPF = newValue
        //TODO this must be done in WorkingFlight not here
        if (wf.autoFill) {
            wf.takeoffLandings = TakeoffLandings(newValue.toInt())
        }
    }

    fun toggleAutovaluesSoftOffIfUnknownAirport(){
        val unknownAirportFound = origOrDestInvalid()
        if (!unknownAirportFound && autoValuesOnlyOffBecauseOfUnknownAirport){              // autovalues off because unknown airports, but all airports known
            toggleAutoValues(true)                                                // force autoValues on
            autoValuesOnlyOffBecauseOfUnknownAirport = false                            // it is not off so this should be false
        }
        if (unknownAirportFound && wf.autoFill){                        // autovalues wants to be on, but unknown airports prevet that
            autoValuesOnlyOffBecauseOfUnknownAirport = true                             // We switch off Autovalues for this reason
            toggleAutoValues(false)                                               // force autoValues off. If airports are all known again, calling this function again will reset it to on
        }
    }
    private fun origOrDestInvalid() =
        originIsValid.value == false || destinationIsValid.value == false

    fun toggleAutoValues(force: Boolean? = null){
        viewModelScope.launch {
            wf.autoFill = force ?: !wf.autoFill
            if (force == null) {
                toggleAutovaluesSoftOffIfUnknownAirport()
            }
        }
    }

    // TODO do this in [WorkingFlight]
    init {
        viewModelScope.launch {
            wf.notifyAircraftDbUpdated()
        }
    }

    // TODO do this in [WorkingFlight]
    fun notifyAirportDbChanged(){
        wf.notifyAirportDbUpdated()
    }


    /*
     * If this edits a flight that doesn't end up as completed, it will either:
     *      - push back calendar sync if offBlocks is less than 30 minutes in the future
     *      - Feed back to main activity that this gives a possible sync problem
     *          -> This feedback is given instead of CLOSE_EDIT_FLIGHT_FRAGMENT so user can decide to continue editing.
     */
    fun saveWorkingFlightAndCloseFragment() {
        if (wf.canCauseCalendarConflict) {
            val now = Instant.now().epochSecond
            if (wf.timeIn > now) {
                if (wf.timeOut <= now + THIRTY_MINUTES_IN_SECONDS) {
                    postponeCalendarSync()
                }
                //Below happens if flight starts > 30 minutes in the future and calendar sync is active during this flight
                else if (Preferences.calendarDisabledUntil < wf.timeIn) {
                    feedback(EditFlightFragmentEvents.EDIT_FLIGHT_CALENDAR_CONFLICT)
                    return
                }
            }
            //else { /*no problem, no action*/ }
        }
        closing = true
        wf.saveAndClose()
        feedback(EditFlightFragmentEvents.CLOSE_EDIT_FLIGHT_FRAGMENT)
    }

    fun closeWithoutSaving(){                                                 // this makes sure any pending dialogs don't get opened anymore
        flightRepository.closeWorkingFlight()                               // This makes sure fligth doesn't reopen again on recreate activity (ie. rotate)
        feedback(EditFlightFragmentEvents.CLOSE_EDIT_FLIGHT_FRAGMENT)       // This tells fragment to close itself
    }

    fun disableCalendarSync(){
        Preferences.useCalendarSync = false
    }

    /**
     * Postpones Calendar Sync until max of:
     *      - 1 second after [WorkingFlight.mTimeIn]
     *      - current [Preferences.calendarDisabledUntil]
     */
    fun postponeCalendarSync(){
        (wf.timeIn + 1).let {
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

    /*
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

    private fun getPicOrPicusString(): String = getString(
        if(wf.isPICUS) R.string.picus else R.string.pic
    )

    private fun makeSortedRegistrationsFlowAndCacheIt() =
        combine(aircraftRepository.aircraftMapFlow,FlightRepository.getInstance().allFlightsFlow) {
        regMap, allFlights ->
        makeSortedRegistrationsList(allFlights, regMap).also {
            cachedSortedRegistrationsList = it
        }
    }


    private fun makeSortedRegistrationsList(
        allFlights: List<FlightData>,
        regMap: Map<String, Aircraft>
    ) = (allFlights.map { it.registration } + regMap.values.map { it.registration })
        .distinct()

    //I could make this suspended and use requireMap() and getAircraftFromRegistration()
    //Why is this done here anyway and not in WorkingFlight? TODO
    private fun getBestHitForPartialRegistration(r: String): Aircraft? =
        aircraftDataCache.getAircraftFromRegistration(r)
        ?: aircraftDataCache.getAircraftFromRegistration(findBestHitForRegistration(r,cachedSortedRegistrationsList))


    companion object{
        const val NO_DATA_STRING = "…"

        const val DUAL_INSTRUCTOR_FLAG_NONE = 0
        const val DUAL_INSTRUCTOR_FLAG_DUAL = 1
        const val DUAL_INSTRUCTOR_FLAG_INSTRUCTOR = 2

        private const val THIRTY_MINUTES_IN_SECONDS = 30*60
    }

}