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
import nl.joozd.logbookapp.data.dataclasses.Aircraft
import nl.joozd.logbookapp.data.dataclasses.Airport
import nl.joozd.logbookapp.data.miscClasses.crew.AugmentedCrew
import nl.joozd.logbookapp.data.repository.flightRepository.FlightRepositoryWithUndo
import nl.joozd.logbookapp.extensions.atDate
import nl.joozd.logbookapp.extensions.plusDays
import nl.joozd.logbookapp.extensions.plusMinutes
import nl.joozd.logbookapp.extensions.toLocalDate
import nl.joozd.logbookapp.model.ModelFlight
import nl.joozd.logbookapp.model.dataclasses.Flight
import nl.joozd.logbookapp.utils.CastFlowToMutableFlowShortcut
import java.time.Instant
import java.time.LocalDate

/**
 * This holds a ModelFlight with entries to edit it.
 */
class FlightEditorImpl(flight: ModelFlight): FlightEditor {
    override val isNewFlight: Boolean = flight.flightID == Flight.FLIGHT_ID_NOT_INITIALIZED

    override val flightFlow: Flow<ModelFlight> = MutableStateFlow(flight)

    private var flight: ModelFlight by CastFlowToMutableFlowShortcut(flightFlow)

    override var flightNumber: String
        get() = flight.flightNumber
        set(flightNumber){
            flight = flight.copy(flightNumber = flightNumber)
        }

    override var orig: Airport
        get() = flight.orig
        set(orig) {
            flight = flight.copy (orig = orig).autoValues()
        }

    override var dest: Airport
        get() = flight.dest
        set(dest) {
            flight = flight.copy (dest = dest).autoValues()
        }

    override var timeOut: Instant
        get() = flight.timeOut
        set(timeOut) {
            flight = flight.copy (timeOut = timeOut).autoValues()
        }

    override var timeIn: Instant
        get() = flight.timeIn
        set(timeIn) {
            flight = flight.copy (timeIn = timeIn).autoValues()
        }

    override var date: LocalDate
        get() = timeOut.toLocalDate()
        set(date) {
            val tOut = timeOut.atDate(date)
            val tIn = timeIn.atDate(date).let{
                if (it > tOut) it
                else it.plusDays(1)
            }
            flight = flight.copy (timeOut = tOut, timeIn = tIn).autoValues()
        }

    override var aircraft: Aircraft
        get() = flight.aircraft
        set(aircraft) {
            flight = flight.copy (aircraft = aircraft).autoValues()
        }

    override var takeoffLandings: TakeoffLandings
        get() = flight.takeoffLandings
        set(takeoffLandings) {
            flight = flight.copy (takeoffLandings = takeoffLandings).autoValues()
        }

    override var name: String
        get() = flight.name
        set(name) {
            flight = flight.copy (name = name).autoValues()
        }

    override var name2: List<String>
        get() = flight.name2
        set(name2) {
            flight = flight.copy (name2 = name2).autoValues()
        }

    override var remarks: String
        get() = flight.remarks
        set(remarks) {
            flight = flight.copy (remarks = remarks).autoValues()
        }

    override var multiPilotTime: Int
        get() = flight.multiPilotTime
        set(multiPilotTime) {
            var newFlight = flight.copy (multiPilotTime = multiPilotTime)
            if (!checkAutoValuesStillOK(newFlight)) newFlight = newFlight.copy(autoFill = false)
            flight = newFlight.autoValues()
        }

    // Set to Flight.FLIGHT_IS_VFR is not IFR, set to anything >= 0 to make it IFR.
    override var ifrTime: Int
        get() = flight.ifrTime
        set(ifrTime){
            //if IFR time changed to anything other than totalFlightTime or FLIGHT_IS_VFR, this disables autoFill.
            val af = ifrTime == totalFlightTime || ifrTime == Flight.FLIGHT_IS_VFR
            flight = flight.copy(ifrTime = ifrTime, autoFill = af && autoFill).autoValues()
        }

    override var nightTime: Int
        get() = flight.nightTime
        set(nightTime) {
            var newFlight = flight.copy (nightTime = nightTime)
            if (!checkAutoValuesStillOK(newFlight)) newFlight = newFlight.copy(autoFill = false)
            flight = newFlight.autoValues()
        }

    //This will give actual calculated elapsed time, with augmented crew and all
    //If set, will always disable autofill.
    override var totalFlightTime: Int
        get() = flight.calculateTotalTime()
        set(correctedTotalTime) {
            flight = flightWithNewCorrectedTotalTimeAndAutofillDisabledIfNotZero(correctedTotalTime).autoValues()
        }

    //This will always give correctedTotalTime, useful for undo functions
    override var correctedTotalTime: Int
        get() = flight.correctedTotalTime
        set(correctedTotalTime) {
            flight = flightWithNewCorrectedTotalTimeAndAutofillDisabledIfNotZero(correctedTotalTime).autoValues()
        }

    private fun flightWithNewCorrectedTotalTimeAndAutofillDisabledIfNotZero(correctedTotalTime: Int) =
        flight.copy(
            correctedTotalTime = correctedTotalTime,
            autoFill = autoFill && correctedTotalTime == 0
        )


    override var augmentedCrew: Int                    // parse this in ViewModel
        get() = flight.augmentedCrew
        set(augmentedCrew) {
            flight = flight.copy (augmentedCrew = augmentedCrew).autoValues()
        }

    override var isSim: Boolean
        get() = flight.isSim
        set(isSim) {
            flight = flight.copy (isSim = isSim).autoValues()
        }
    override var simTime: Int
        get() = flight.simTime
        set(simTime) {
            flight = flight.copy(simTime = simTime).autoValues()
        }

    override var signature: String
        get() = flight.signature
        set(signature) {
            flight = flight.copy (signature = signature).autoValues()
        }

    override var isDual: Boolean
        get() = flight.isDual
        set(isDual) {
            flight = flight.copy (isDual = isDual).autoValues()
        }

    override var isInstructor: Boolean
        get() = flight.isInstructor
        set(isInstructor) {
            flight = flight.copy (isInstructor = isInstructor).autoValues()
        }

    override var isPIC: Boolean
        get() = flight.isPIC
        set(isPIC) {
            flight = flight.copy (isPIC = isPIC).autoValues()
        }

    override var isPICUS: Boolean
        get() = flight.isPICUS
        set(isPICUS) {
            flight = flight.copy (isPICUS = isPICUS).autoValues()
        }

    //Normally autoValues takes care of this, so overriding this disables autovalues.
    override var isCoPilot: Boolean
        get() = flight.isCoPilot
        set(isCoPilot){
            flight = flight.copy(isCoPilot = isCoPilot, autoFill = false)
        }


    override var isPF: Boolean
        get() = flight.isPF
        set(isPF) {
            flight = flight.copy (isPF = isPF)
                .setTakeoffLandingsForIsPF() // this will set to/ldg to 1/1 or 0/0 if appropriate.
                .autoValues()
        }

    override var autoFill: Boolean
        get() = flight.autoFill
        set(isAutoValues) {
            flight = flight.copy (autoFill = isAutoValues).autoValues()
        }

    override fun setToFLight(flight: ModelFlight) {
        this.flight = flight
    }

    override fun snapshot(): ModelFlight = flight.copy()

    override fun toggleDualInstructorNeither() {
        val becomesDual = isDual == isInstructor // either both true or both false will make becomesDual true
        val becomesInstructor = isDual && !isInstructor //
        // if it was !isDual && isInstructor, both will be false
        flight = flight.copy (isDual = becomesDual, isInstructor = becomesInstructor).autoValues()
    }

    override fun togglePicusPicNeither() {
        val becomesPICUS = isPICUS == isPIC
        val becomesPIC = isPICUS && !isPIC
        // if it was !isPICUS && isPIC, both will be false
        flight = flight.copy(isPICUS = becomesPICUS, isPIC = becomesPIC).autoValues()
    }

    override suspend fun save(): Flight {
        removeEmptyNames() // Name dialog adds empty names at the end for editing. These should not be saved.
        val f = flight.prepareForSaving()
        FlightRepositoryWithUndo.instance.save(f)
        return f
    }

    override fun close() {
        FlightEditor.close()
    }

    /*
     * Use this when a value that can disable autoFill is changed.
     * If a changed value is actually changed it will return false.
     */
    private fun checkAutoValuesStillOK(updatedFlight: ModelFlight): Boolean =
        updatedFlight == flight

    private fun removeEmptyNames(){
        name2 = name2.filter { it.isNotBlank() }
    }

    // A flight is planned when it is edited to start in the future (or less than 5 minutes before now)
    private fun ModelFlight.isPlanned(): Boolean =
        if (isSim) date() > LocalDate.now()
        else timeIn > Instant.now().plusMinutes(5)

    /*
     * Transforms ModelFlight to Flight
     * and removes values that do not belong in sim/not sim records.
     */
    private fun ModelFlight.prepareForSaving(): Flight =
        if (isSim) prepareSimFlightForSaving()
        else prepareNonSimFlightForSaving()

    private fun ModelFlight.prepareSimFlightForSaving(): Flight{
        require (isSim){ "Only use this for simulator sessions" }
        return toFlight().copy(
            orig = "",
            dest = "",
            name = "",
            ifrTime = 0,
            nightTime = 0,
            multiPilotTime = 0,
            simTime = simTime,
            augmentedCrew = AugmentedCrew().toInt(),
            isPlanned = isPlanned()
        )
    }

    private fun ModelFlight.prepareNonSimFlightForSaving(): Flight {
        require (!isSim){ "Only use this for non-simulator sessions" }
        return toFlight().copy(
            isPlanned = isPlanned()
        )
    }
}


