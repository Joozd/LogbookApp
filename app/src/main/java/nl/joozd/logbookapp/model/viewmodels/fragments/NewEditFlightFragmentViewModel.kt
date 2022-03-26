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

import android.util.Log
import androidx.lifecycle.viewModelScope
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.launch
import nl.joozd.logbookapp.data.dataclasses.Aircraft
import nl.joozd.logbookapp.data.repository.aircraftrepository.AircraftRepository
import nl.joozd.logbookapp.data.repository.flightRepository.FlightRepository
import nl.joozd.logbookapp.extensions.toLocalDate
import nl.joozd.logbookapp.model.dataclasses.Flight
import nl.joozd.logbookapp.model.enumclasses.DualInstructorFlag
import nl.joozd.logbookapp.model.enumclasses.PicPicusFlag
import nl.joozd.logbookapp.model.helpers.makeNamesList
import nl.joozd.logbookapp.model.viewmodels.JoozdlogViewModel
import nl.joozd.logbookapp.model.workingFlight.FlightEditor
import nl.joozd.logbookapp.model.workingFlight.FlightEditorDataParser
import java.time.LocalDate


class NewEditFlightFragmentViewModel: JoozdlogViewModel() {
    private val flightEditor = FlightEditor.instance!! // this Fragment should not have launched if flightEditor is null
    private val aircraftRepository = AircraftRepository.instance
    private val flightRepository = FlightRepository.instance

    //use this for any data that needs parsing
    private val flightEditorDataParser = FlightEditorDataParser(flightEditor)

    private val flightFlow get() = flightEditor.flightFlow

    val isNewFlight = flightEditor.isNewFlight

    val localDate: LocalDate get() = flightEditor.timeOut.toLocalDate()

    val isSim: Boolean get() = flightEditor.isSim

    fun dateFlow() = flightFlow.map { it.date() }
    fun flightNumberFlow() = flightFlow.map { it.flightNumber }
    fun timeOutFlow() = flightFlow.map { it.timeOut }
    fun timeInFlow() = flightFlow.map { it.timeIn }
    fun origFlow() = flightFlow.map { it.orig }
    fun destFlow() = flightFlow.map { it.dest }
    fun aircraftFlow() = flightFlow.map { it.aircraft }
    fun takeoffLandingsFlow() = flightFlow.map{ it.takeoffLandings }
    fun nameFlow() = flightFlow.map { it.name }
    fun name2Flow() = flightFlow.map { it.name2 }
    fun remarksFlow() = flightFlow.map { it.remarks }

    fun isSimFlow() = flightFlow.map { it.isSim }
    fun isSignedFlow() = flightFlow.map { it.signature.isNotBlank() }
    fun dualInstructorFlow() = flightFlow.map { when {
        it.isDual -> DualInstructorFlag.DUAL
        it.isInstructor -> DualInstructorFlag.INSTRUCTOR
        else -> DualInstructorFlag.NONE
    }   }
    fun isMultiPilotFlow() = flightFlow.map { it.multiPilotTime > 0 }
    fun isIfrFlow() = flightFlow.map { it.ifrTime >= 0 } // IFR time of -1 means VFR
    fun picPicusFlow() = flightFlow.map { when{
        it.isPIC -> PicPicusFlag.PIC
        it.isPICUS -> PicPicusFlag.PICUS
        else -> PicPicusFlag.NONE
    }    }
    fun isPfFlow() = flightFlow.map { it.isPF }

    fun simTimeFlow() = flightFlow.map { it.simTime }

    fun isAutoValuesFlow() = flightFlow.map { it.autoFill }


    fun sortedRegistrationsFlow() =
        aircraftRepository.aircraftMapFlow().map { regMap ->
            makeSortedRegistrationsList(regMap)
        }

    fun namesFlow() = flightRepository.allFlightsFlow().map{
        it.makeNamesList()
    }

    private fun makeSortedRegistrationsList(regMap: Map<String, Aircraft>) =
        regMap.keys.toList()

    fun toggleSim(){
        flightEditor.isSim = !flightEditor.isSim
    }

    fun toggleDualInstructorNone(){
        flightEditor.toggleDualInstructorNeither()
    }

    fun togglePicusPicNone(){
        flightEditor.togglePicusPicNeither()
    }

    fun toggleMultiPilot(){
        flightEditor.multiPilotTime =
        if (flightEditor.multiPilotTime == 0)
             flightEditor.totalFlightTime
        else 0
    }

    fun toggleIFR(){
        flightEditor.ifrTime =
            if(flightEditor.ifrTime >= 0) Flight.FLIGHT_IS_VFR
        else flightEditor.totalFlightTime
    }

    fun togglePF(){
        flightEditor.isPF = !flightEditor.isPF
    }

    fun toggleAutoValues(){
        flightEditor.autoFill = !flightEditor.autoFill
    }

    fun setFlightNumber(flightNumber: String?){
        println("setFlightNumber Received $flightNumber")
        flightNumber?.let{
            flightEditor.flightNumber = it
        }
    }

    fun setOrig(orig: String?){
        orig?.let {
            flightEditorDataParser.setOrig(it)
        } ?: Log.w(this::class.simpleName, "setOrig() received null param")
    }

    fun setDest(dest: String?){
        dest?.let {
            flightEditorDataParser.setDest(it)
        } ?: Log.w(this::class.simpleName, "setDest() received null param")
    }

    fun setTimeOut(timeOut: String?){
        timeOut?.let {
            flightEditorDataParser.setTimeOut(it)
        }
    }

    fun setTimeIn(timeIn: String?){
        timeIn?.let {
            flightEditorDataParser.setTimeIn(it)
        }
    }

    fun setRegAndType(regAndType: String?){
        regAndType?.let{ flightEditorDataParser.setAircraft(it) }
    }

    fun setTakeoffLandings(toLandingData: String?){
        toLandingData?.let{
            flightEditorDataParser.setTakeoffLandings(it)
        }
    }


    fun setName(name: String?){
        name?.let{
            flightEditor.name = it
        }
    }

    fun setName2(names: String?){
        names?.let{
            flightEditor.name2 = splitAndTrimNames(it)
        }
    }

    fun setRemarks(remarks: String?){
        remarks?.let{
            flightEditor.remarks = it
        }
    }

    fun setSimTime(simTime: String?){
        flightEditorDataParser.setSimTimeFromString(simTime)
    }

    fun setSimAircraft(aircraft: String?){
        aircraft?.let {
            flightEditorDataParser.setSimAircraftType(it)
        }
    }


    private fun splitAndTrimNames(namesString: String) =
        namesString.split(";").map { it.trim() }

    fun saveAndClose(){
        viewModelScope.launch {
            flightEditorDataParser.saveAndClose()
        }
    }

    fun closeWithoutSaving(){
        flightEditorDataParser.close()
    }

    /*

    val dualInstructorFlow: Flow<Int> = makeDualInstructorFlow()
    val aircraftStringFlow: Flow<String> = makeAircraftDisplayNameFlow()
    val isPic: Flow<Boolean> = makePicOrPicusFlow()
    val picPicusText: LiveData<String> = makePicPicusTextMediatorLiveData()


    val dateStringLiveData = flightEditor.timeOutLiveData.map{ makeDateString(it) }
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

    private fun makeAircraftDataCacheUpToDate() {
        initializeAircraftDataCache()
        collectNewAircraftDataCaches()
    }

    private fun makeAirportDataCacheUpToDate() {
        initializeAirportDataCache()
        collectNewAirportDataCaches()
    }


    private fun initializeAirportDataCache() {
        viewModelScope.launch {
            airportDataCache = airportRepository.getAirportDataCache()
        }
    }



    private fun makeDateString(epochSecond: Long) =
        Instant.ofEpochSecond(epochSecond).toLocalDate().toDateString()

    private fun makeAircraftDisplayNameFlow() =
        combine(wf.aircraftFlow, wf.isSimFlow){ aircraft, isSim ->
            (if (isSim) aircraft.type?.shortName else aircraft.toString())
                ?: NO_DATA_STRING
        }

    private fun makePicOrPicusFlow() =
        flightFlow.map{
            it.isPIC || it.isPICUS
        }

    private fun makePicPicusTextMediatorLiveData() =
        MediatorLiveData<String>().apply {
            addSource(wf.isPICLiveData) { value = getPicOrPicusString() }
            addSource(wf.isPICUSLiveData) { value = getPicOrPicusString() }
        }


    private fun getAircraftDisplayName() =
        (if (isSim) flightEditor.aircraft.type?.shortName else flightEditor.aircraft.toString())
            ?: NO_DATA_STRING

    private fun makeDualInstructorFlow() = flightFlow.map{
            when {
                it.isDual -> DUAL_INSTRUCTOR_FLAG_DUAL
                it.isInstructor -> DUAL_INSTRUCTOR_FLAG_INSTRUCTOR
                else -> DUAL_INSTRUCTOR_FLAG_NONE
            }
        }

    private fun WorkingFlightOld.isPicOrPicus(): Boolean =
        isPIC || isPICUS


    fun setFlightNumber(newFlightNumber: Editable?){
        newFlightNumber?.toString()?.let{
            // If new flightnumber is not the old one minus all the digits (eg KL1234 becomes KL)
            if (it.isNotOldValueWithDigitsRemoved())
                flightEditor.flightNumber = it
        }
    }

    // true if all digits are removed from wf.flightNumber and nothing else changed, eg. KL1234 became KL
    private fun String.isNotOldValueWithDigitsRemoved() =
        this != (flightEditor.flightNumber).removeTrailingDigits()


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

    //TODO think about if this work should be done here or in workingFLight
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
        if(flightEditor.isPICUS) R.string.picus else R.string.pic
    )

    private fun makeSortedRegistrationsFlowAnd() =
        combine(aircraftRepository.aircraftMapFlow(), FlightRepository.instance.getAllFlightsFlow()) {
        regMap, allFlights ->
        makeSortedRegistrationsList(allFlights, regMap).
    }




    private fun getBestHitForPartialRegistration(r: String): Aircraft? =
        dataCache.aircraftDataCache.getAircraftFromRegistration(r)
        ?: aircraftDataCache?.getAircraftFromRegistration(findBestHitForRegistration(r,cachedSortedRegistrationsList))

    //TODO Is there a way to have this done in WorkingFlight itself without leaking the Flow?
    init {
        viewModelScope.launch {
            aircraftRepository.aircraftDataCacheFlow().collect {
                wf.notifyAircraftDbUpdated()
            }
        }
        viewModelScope.launch {
            airportRepository.airportsFlow().collect {
                wf.notifyAirportDbUpdated()
            }
        }
    }

    companion object{
        const val NO_DATA_STRING = "…"

        const val DUAL_INSTRUCTOR_FLAG_NONE = 0
        const val DUAL_INSTRUCTOR_FLAG_DUAL = 1
        const val DUAL_INSTRUCTOR_FLAG_INSTRUCTOR = 2

        private const val THIRTY_MINUTES_IN_SECONDS = 30*60
    }

     */

}