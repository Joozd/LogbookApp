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
import nl.joozd.logbookapp.model.dataclasses.Flight
import nl.joozd.logbookapp.utils.CastFlowToMutableFlowShortcut
import java.time.Instant
import java.time.LocalDate

/**
 * Provide a model for working on a flight.
 * Has Flows with data to display in UI
 * Has functions to receive data from UI
 * Implementation should take care of updating related data
 *  (eg. IFR if aircraft changes, night time if times or airport changes)
 */
interface WorkingFlight {
    /**
     * Date of flight in UTC Flow
     */
    val dateFlow: Flow<LocalDate>

    /**
     * Flightnumber Flow
     */
    val flightNumberFlow: Flow<String>

    /**
     * Origin airport
     */
    val origFlow: Flow<Airport>

    /**
     * Destination Flow
     */
    val destFlow: Flow<Airport>

    /**
     * Instant of departure Flow
     */
    val timeOutFlow: Flow<Instant>

    /**
     * Instant of departure Flow
     */
    val timeInFlow: Flow<Instant>

    /**
     * Aircraft used for this flight Flow
     */
    val aircraftFlow: Flow<Aircraft>

    /**
     * Takeoffs and landings Flow
     */
    val takeoffLandingsFlow: Flow<TakeoffLandings>

    /**
     * Name of PIC flow
     */
    val nameFlow: Flow<String>

    /**
     * Names of other crewmembers
     */
    val name2Flow: Flow<List<String>>

    /**
     * Remarks Flow
     */
    val remarksFlow: Flow<String>

    /**
     * emits true if set to Sim, false if not
     */
    val isSimFlow: Flow<Boolean>

    /**
     * Signature SVG String or empty string if not signed
     */
    val signatureFlow: Flow<String>

    /**
     * emits true if flight is Dual, false if not
     */
    val isDualFlow: Flow<Boolean>

    /**
     * emits true if flight is Dual, false if not
     */
    val isInstructorFlow: Flow<Boolean>

    /**
     * emits true if flight is MultiPilot, false if not
     */
    val isMultiPilotFlow: Flow<Boolean>

    /**
     * emits true if flight is (partially) IFR, false if not
     */
    val isIfrFlow: Flow<Boolean>

    /**
     * emits true if flight is logged as PIC, false if not
     */
    val isPicFlow: Flow<Boolean>

    /**
     * emits true if flight is logged as PICUS, false if not
     */
    val isPicusFlow: Flow<Boolean>

    /**
     * emits true if flight is logged as Copilot, false if not
     */
    val isCopilotFlow: Flow<Boolean>

    /**
     * emits true if flight is logged as PF, false if not
     */
    val isPfFlow: Flow<Boolean>

    /**
     * MultiPilot Time in minutes Flow
     */
    val multiPilotTimeFlow: Flow<Int>

    /**
     * IFR Time in minutes Flow
     */
    val ifrTimeFlow: Flow<Int>

    /**
     * Night Time in minutes Flow
     */
    val nightTimeFlow: Flow<Int>

    /**
     * Total Time in minutes Flow
     */
    val correctedTotalTimeFlow: Flow<Int>

    /**
     * Augmented Crew data Flow
     */
    val augmentedCrewFlow: Flow<AugmentedCrew>

    /**
     * emits true if flight is auto-calculating values, false if not
     */
    val isAutoValuesFlow: Flow<Boolean>

    /**
     * Set date for this flight without changing times
     */
    fun setDate(date: LocalDate)

    /**
     * Set Flight Number for this flight
     */
    fun setFlightNumber(flightNumber: String)

    /**
     * Set Origin for this flight
     */
    fun setOrig(orig: Airport)

    /**
     * Set Destination for this flight
     */
    fun setDest(dest: Airport)

    /**
     * Set timeOut for this flight
     */
    fun setTimeOut(timeOut: Instant)

    /**
     * Set timeIn for this flight
     */
    fun setTimeIn(timeIn: Instant)

    /**
     * Set [Aircraft] for this flight
     */
    fun setAircraft(aircraft: Aircraft)

    /**
     * Set [TakeoffLandings] for this flight
     */
    fun setTakeoffLandings(takeoffLandings: TakeoffLandings)

    /**
     * Set PIC Name for this flight
     */
    fun setName(name: String)

    /**
     * Set other names for this flight
     */
    fun setName2(names: List<String>)

    /**
     * Set Remarks for this flight
     */
    fun setRemarks(remarks: String)

    /**
     * Set signature for this flight
     */
    fun setSignature(signature: String)

    /**
     * Set isDual toggle for this flight
     */
    fun setIsDual(isDual: Boolean)

    /**
     * Set isInstructor toggle for this flight
     */
    fun setIsInstructor(isInstructor: Boolean)

    /**
     * Set isMultiPilot toggle for this flight
     */
    fun setMultiPilotTime(multiPilotTime: Int)

    /**
     * Set isIFR toggle for this flight
     */
    fun setIfrTime(ifrTime: Int)

    /**
     * Set isPIC toggle for this flight
     */
    fun setIsPIC(isPIC: Boolean)

    /**
     * Set isPICUS toggle for this flight
     */
    fun setIsPICUS(isPICUS: Boolean)

    /**
     * Set isSim toggle for this flight
     */
    fun setIsPF(isPF: Boolean)

    /**
     * Set isAutoValues toggle for this flight
     */
    fun setIsAutoValues(isAutoValues: Boolean)

    companion object{
        val instanceFlow: Flow<WorkingFlight?> = MutableStateFlow(null)
        private var INSTANCE: WorkingFlight? by CastFlowToMutableFlowShortcut(instanceFlow)

        // NOTE this is not a singleton. Take care to close any dialogs editing this WorkingFlight
        // when instanceFlow emits.
        val instance: WorkingFlight? get() = INSTANCE

        fun setFromFlight(flight: Flight) {
            INSTANCE = WorkingFlightImpl(flight)
        }

        fun setNewflight() = setFromFlight(Flight.createEmpty())

        fun close() {
            INSTANCE = null
        }
    }
}