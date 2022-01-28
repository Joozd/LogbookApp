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







    // Not a StateFlow!
    override val isMultiPilotFlow: Flow<Boolean> = aircraftFlow.map { it.type?.multiPilot ?: false }

    // Not a StateFlow!
    override val isIfrFlow: Flow<Boolean> = ifrTimeFlow.map{ it > 0 }

    override val isPicFlow: Flow<Boolean> = MutableStateFlow()

    override val isPicusFlow: Flow<Boolean> = MutableStateFlow()

    override val isCopilotFlow: Flow<Boolean> = MutableStateFlow()

    override val isPfFlow: Flow<Boolean> = MutableStateFlow()

    override val isAutoValuesFlow: Flow<Boolean> = MutableStateFlow()

    /*
     * Shortcuts for functions
     */
    override var flightNumber: String = flight.flightNumber

    override var orig: Airport = makeDummyAirport(flight.orig)
        set(orig) {
            field = orig
            autoUpdateValuesIfAutovaluesEnabled()
        }

    override var dest: Airport = makeDummyAirport(flight.dest)
        set(dest) {
            field = dest
            autoUpdateValuesIfAutovaluesEnabled()
        }

    override var timeOut: Instant = Instant.ofEpochSecond(flight.timeOut)
        set(timeOut) {
            field = timeOut
            autoUpdateValuesIfAutovaluesEnabled()
        }

    override var timeIn: Instant = Instant.ofEpochSecond(flight.timeIn)
        set(timeIn) {
            field = timeIn
            autoUpdateValuesIfAutovaluesEnabled()
        }
    override var date: LocalDate
        get() = timeOut.toLocalDate()
        set(date) {
            timeOut = timeOut.atDate(date)
            timeIn = timeIn.atDate(date).let{
                if (it > timeOut) it
                else it.plusDays(1)
            }
            autoUpdateValuesIfAutovaluesEnabled()
        }

    override var aircraft: Aircraft = makeDummyAircraft(flight)
    override var takeoffLandings = TakeoffLandings.fromFlight(flight)
    override var name: String = flight.name
    override var name2: List<String> = flight.name2.split(";")
    override var remarks: String = flight.remarks
    override var multiPilotTime: Int = flight.multiPilotTime
    override var ifrTime: Int = flight.ifrTime
    override var nightTime: Int = flight.nightTime
    override var correctedTotalTime: Int = flight.correctedTotalTime            // this is always 0 when autoValues
    override var augmentedCrew: Int = flight.augmentedCrew                      // parse this in ViewModel
    override var isSim: Boolean = flight.isSim
    override var signature: String = flight.signature
    override var isDual: Boolean = flight.isDual
    override var isInstructor: Boolean = flight.isInstructor
    override var isPIC: Boolean = flight.isPIC
    override var isPICUS: Boolean = flight.isPICUS
    override var isCopilot: Boolean = flight.isCoPilot
    override var isPF: Boolean = flight.isPF
    override var isAutoValues: Boolean = flight.autoFill

    val crew get() = _augmentedCrew

    private val _isIFR: Boolean
        get() = _ifrTime > 0
    private val _isMultiPilot: Boolean
        get() = _aircraft.type?.multiPilot ?: false


    // Influences auto-values (Night time)
    override fun




    // Influences auto-values
    override

    // Influences auto-values
    override fun

    // Influences auto-values (MultiPilot time)
    override fun setAircraft(aircraft: Aircraft) {
        _aircraft = aircraft
        autoUpdateValuesIfAutovaluesEnabled()
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


