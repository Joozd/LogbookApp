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

package nl.joozd.logbookapp.model.workingFlight

import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.map
import nl.joozd.joozdlogcommon.AircraftType
import nl.joozd.logbookapp.data.dataclasses.Aircraft
import nl.joozd.logbookapp.data.dataclasses.Airport
import nl.joozd.logbookapp.data.miscClasses.crew.AugmentedCrew
import nl.joozd.logbookapp.extensions.atDate
import nl.joozd.logbookapp.extensions.plusDays
import nl.joozd.logbookapp.extensions.toLocalDate
import nl.joozd.logbookapp.model.dataclasses.Flight
import nl.joozd.logbookapp.utils.CastFlowToMutableFlowShortcut
import nl.joozd.logbookapp.utils.TwilightCalculator
import java.time.Duration
import java.time.Instant
import java.time.LocalDate

/**
 * Initialize with a flight; dummy classes (Airport, Aircraft) will be made to keep data.
 * Data should be filled with actual classes for calculating things like night time from viewModel
 * after being instantiated.
 */
class WorkingFlightImpl(flight: Flight): WorkingFlight {
    override val isNewFlight: Boolean = flight.flightID == Flight.FLIGHT_ID_NOT_INITIALIZED

    override val flightNumberFlow = MutableStateFlow(flight.flightNumber)

    override val origFlow: Flow<Airport> = MutableStateFlow(makeDummyAirport(flight.orig))

    override val destFlow: Flow<Airport> = MutableStateFlow(makeDummyAirport(flight.dest))

    override val timeOutFlow: Flow<Instant> = MutableStateFlow(Instant.ofEpochSecond(flight.timeOut))

    override val timeInFlow: Flow<Instant> = MutableStateFlow(Instant.ofEpochSecond(flight.timeIn))

    // Not a StateFlow!
    override val dateFlow = timeOutFlow.map { it.toLocalDate() }

    override val aircraftFlow: Flow<Aircraft> = MutableStateFlow(makeDummyAircraft(flight))

    override val takeoffLandingsFlow: Flow<TakeoffLandings> = MutableStateFlow(TakeoffLandings.fromFlight(flight))

    override val nameFlow: Flow<String> = MutableStateFlow(flight.name)

    override val name2Flow: Flow<List<String>> = MutableStateFlow(flight.name2.split(";"))

    override val remarksFlow: Flow<String> = MutableStateFlow(flight.remarks)

    override val multiPilotTimeFlow: Flow<Int> = MutableStateFlow(flight.multiPilotTime)

    override val ifrTimeFlow: Flow<Int> = MutableStateFlow(flight.ifrTime)

    override val nightTimeFlow: Flow<Int> = MutableStateFlow(flight.nightTime)

    override val correctedTotalTimeFlow: Flow<Int> = MutableStateFlow(flight.duration())

    override val augmentedCrewFlow: Flow<AugmentedCrew> = MutableStateFlow(AugmentedCrew.of(flight.augmentedCrew))

    override val isSimFlow: Flow<Boolean> = MutableStateFlow(flight.isSim)

    override val signatureFlow: Flow<String> = MutableStateFlow(flight.signature)

    override val isDualFlow: Flow<Boolean> = MutableStateFlow(flight.isDual)

    override val isInstructorFlow: Flow<Boolean> = MutableStateFlow(flight.isInstructor)

    // Not a StateFlow!
    override val isMultiPilotFlow: Flow<Boolean> = aircraftFlow.map { it.type?.multiPilot ?: false }

    // Not a StateFlow!
    override val isIfrFlow: Flow<Boolean> = ifrTimeFlow.map{ it > 0 }

    override val isPicFlow: Flow<Boolean> = MutableStateFlow(flight.isPIC)

    override val isPicusFlow: Flow<Boolean> = MutableStateFlow(flight.isPICUS)

    override val isCopilotFlow: Flow<Boolean> = MutableStateFlow(flight.isCoPilot)

    override val isPfFlow: Flow<Boolean> = MutableStateFlow(flight.isPF)

    override val isAutoValuesFlow: Flow<Boolean> = MutableStateFlow(flight.autoFill)

    /*
     * Shortcuts for functions
     */
    private var _flightNumber: String by CastFlowToMutableFlowShortcut(flightNumberFlow)
    private var _orig: Airport by CastFlowToMutableFlowShortcut(origFlow)
    private var _dest: Airport by CastFlowToMutableFlowShortcut(destFlow)
    private var _timeOut: Instant by CastFlowToMutableFlowShortcut(timeOutFlow)
    private var _timeIn: Instant by CastFlowToMutableFlowShortcut(timeInFlow)
    private var _aircraft: Aircraft by CastFlowToMutableFlowShortcut(aircraftFlow)
    private var _takeOffLandings: TakeoffLandings by CastFlowToMutableFlowShortcut(takeoffLandingsFlow)
    private var _name: String by CastFlowToMutableFlowShortcut(nameFlow)
    private var _name2: List<String> by CastFlowToMutableFlowShortcut(name2Flow)
    private var _remarks: String by CastFlowToMutableFlowShortcut(remarksFlow)
    private var _multiPilotTime: Int by CastFlowToMutableFlowShortcut(multiPilotTimeFlow)
    private var _ifrTime: Int by CastFlowToMutableFlowShortcut(ifrTimeFlow)
    private var _nightTime: Int by CastFlowToMutableFlowShortcut(nightTimeFlow)
    private var _correctedTotalTime: Int by CastFlowToMutableFlowShortcut(correctedTotalTimeFlow) // this is always 0 when autoValues
    private var _augmentedCrew: AugmentedCrew by CastFlowToMutableFlowShortcut(augmentedCrewFlow)
    private var _isSim: Boolean by CastFlowToMutableFlowShortcut(isSimFlow)
    private var _signature: String by CastFlowToMutableFlowShortcut(signatureFlow)
    private var _isDual: Boolean by CastFlowToMutableFlowShortcut(isDualFlow)
    private var _isInstructor: Boolean by CastFlowToMutableFlowShortcut(isInstructorFlow)
    private var _isPIC: Boolean by CastFlowToMutableFlowShortcut(isPicFlow)
    private var _isPICUS: Boolean by CastFlowToMutableFlowShortcut(isPicusFlow)
    private var _isCopilot: Boolean by CastFlowToMutableFlowShortcut(isCopilotFlow)
    private var _isPF: Boolean by CastFlowToMutableFlowShortcut(isPfFlow)
    private var _isAutoValues: Boolean by CastFlowToMutableFlowShortcut(isAutoValuesFlow)

    val crew get() = _augmentedCrew

    private val _isIFR: Boolean
        get() = _ifrTime > 0
    private val _isMultiPilot: Boolean
        get() = _aircraft.type?.multiPilot ?: false


    // Influences auto-values (Night time)
    override fun setDate(date: LocalDate) {
        _timeOut = _timeOut.atDate(date)
        _timeIn = _timeIn.atDate(date).let{
            if (it > _timeOut) it
            else it.plusDays(1)
        }
        autoUpdateValuesIfAutovaluesEnabled()
    }

    override fun setFlightNumber(flightNumber: String) {
        _flightNumber = flightNumber
    }

    // Influences auto-values (Night time)
    override fun setOrig(orig: Airport) {
        _orig = orig
        autoUpdateValuesIfAutovaluesEnabled()
    }

    // Influences auto-values (Night time)
    override fun setDest(dest: Airport) {
        _dest = dest
        autoUpdateValuesIfAutovaluesEnabled()
    }

    // Influences auto-values
    override fun setTimeOut(timeOut: Instant) {
        _timeOut = timeOut
        autoUpdateValuesIfAutovaluesEnabled()
    }

    // Influences auto-values
    override fun setTimeIn(timeIn: Instant) {
        _timeIn = timeIn
        autoUpdateValuesIfAutovaluesEnabled()
    }

    // Influences auto-values (MultiPilot time)
    override fun setAircraft(aircraft: Aircraft) {
        _aircraft = aircraft
    }

    override fun setTakeoffLandings(takeoffLandings: TakeoffLandings) {
        _takeOffLandings = takeoffLandings
    }

    override fun setName(name: String) {
        _name = name
    }

    override fun setName2(names: List<String>) {
        _name2 = names
    }

    override fun setRemarks(remarks: String) {
        _remarks = remarks
    }

    override fun setSignature(signature: String) {
        _signature = signature
    }

    override fun setIsDual(isDual: Boolean) {
        _isDual = isDual
    }

    override fun setIsInstructor(isInstructor: Boolean) {
        _isInstructor = isInstructor
    }

    //This value also calculated automatically
    override fun setMultiPilotTime(multiPilotTime: Int) {
        _multiPilotTime = multiPilotTime
        disableAutoValuesIfConflicting()
    }

    //This value also calculated automatically
    override fun setIfrTime(ifrTime: Int) {
        _ifrTime = ifrTime
        disableAutoValuesIfConflicting()
    }

    // Influences auto-values (PIC matters for Augmented crew logged time)
    override fun setIsPIC(isPIC: Boolean) {
        _isPIC = isPIC
        autoUpdateValuesIfAutovaluesEnabled()
    }

    override fun setIsPICUS(isPICUS: Boolean) {
        _isPICUS = isPICUS
    }

    override fun setIsPF(isPF: Boolean) {
        _isPF = isPF
        updateTakeOffLandingsIfRequired()
    }

    override fun setAugmentedCrew(augmentedCrew: AugmentedCrew) {
        _augmentedCrew = augmentedCrew
        autoUpdateValuesIfAutovaluesEnabled()
    }

    override fun setIsAutoValues(isAutoValues: Boolean) {
        _isAutoValues = isAutoValues
        autoUpdateValuesIfAutovaluesEnabled()
    }

    private fun autoUpdateValuesIfAutovaluesEnabled(){
        if (_isAutoValues){
            autoValues()
        }
    }

    /**
     * If [_isPF] and [_takeOffLandings] do not match, updates _takeOffLandings if it was 1/1 or 0/0
     */
    private fun updateTakeOffLandingsIfRequired(){
        when{
            oneTakeoffOneLanding() && !_isPF -> _takeOffLandings = TakeoffLandings(0,0)
            noTakeoffNoLanding() && _isPF -> {
                _takeOffLandings = TakeoffLandings(1,1)
                updateTakeoffLandingsForNightTime()
            }
            // else -> do nothing
        }
    }





    /*
     * Autovalues makes sure all related values change if a certain field is changed.
     * Values that need to be calculated:
     * - IFR time (depends on isSim, isIFR, timeOut, timeIn, augmentedCrew)
     * - Night time (depends on isSim, orig, dest, timeOut, timeIn, augmentedCrew)
     * - isCopilot (depends on aircraft, isPic)
     */
    private fun autoValues(){
        // NOTE TO SELF:
        // Run all autoValues calculations every time. Fast enough and makes program much more simple.
        // If it is not fast enough after all certain parts (i.e. night time) can be done async.
        _correctedTotalTime = 0
        _nightTime = calculateNightTime()
        _takeOffLandings = updateTakeoffLandingsForNightTime()
        _ifrTime = calculateIfrTime()
        _multiPilotTime = calculateMultiPilotTime()
    }

    /*
     * Disables autovalues if it was enabled and IFR, MultiPilot or Night time don't match with
     * calculated values.
     * Call this when changing a value that is also auto-calculated
     */
    private fun disableAutoValuesIfConflicting(){
        _isAutoValues =
            _isAutoValues &&
            calculateIfrTime() == _ifrTime &&
            calculateMultiPilotTime() == _multiPilotTime &&
            calculateNightTime() == _nightTime
    }

    private fun calculateNightTime(): Int =
        TwilightCalculator(_timeOut).minutesOfNight(_orig, _dest, _timeOut, _timeIn)

    /**
     * Will only work for 1/1 takeoff/landing, otheriwse will do nothing.
     */
    private fun updateTakeoffLandingsForNightTime(): TakeoffLandings =
        if (oneTakeoffOneLanding()){
            val takeoffDuringDay = TwilightCalculator(_timeOut).itIsDayAt(_orig, _timeOut)
            val landingDuringDay = TwilightCalculator(_timeIn).itIsDayAt(_dest, _timeIn)
            val toDay = if (takeoffDuringDay) 1 else 0
            val toNight = 1 - toDay
            val ldgDay = if (landingDuringDay) 1 else 0
            val ldgNight = 1 - ldgDay
            TakeoffLandings(toDay, toNight, ldgDay, ldgNight, _takeOffLandings.autoLand)
        }
        else _takeOffLandings

    private fun oneTakeoffOneLanding() =
        _takeOffLandings.takeOffs == 1 && _takeOffLandings.landings == 1

    private fun noTakeoffNoLanding() =
        _takeOffLandings.takeOffs == 0 && _takeOffLandings.landings == 0


    private fun calculateIfrTime() =
        if (_isIFR) calculateTotalTime()
        else 0

    private fun calculateMultiPilotTime() =
        if (_isMultiPilot) calculateTotalTime()
        else 0

    private fun calculateTotalTime(): Int =
        if (_isSim) 0
        else _augmentedCrew.getLogTime(getDurationOfFlight(), _isPIC)


    private fun getDurationOfFlight(): Duration =
        Duration.between(_timeOut, _timeIn)


    private fun makeDummyAirport(ident: String): Airport =
        Airport(ident = ident)

    private fun makeDummyAircraft(flight: Flight): Aircraft =
        Aircraft(registration = flight.registration, type = makeDummyAircraftType(flight))

    private fun makeDummyAircraftType(flight: Flight): AircraftType =
        AircraftType(
            name = "Dummy Aircraft",
            shortName = flight.aircraftType,
            multiPilot = flight.multiPilotTime > 0,
            multiEngine = false
        )
}


