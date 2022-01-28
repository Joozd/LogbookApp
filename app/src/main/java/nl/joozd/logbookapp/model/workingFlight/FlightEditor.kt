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
import nl.joozd.logbookapp.model.ModelFlight
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
interface FlightEditor {
    /**
     * True if this is a new flight, false if it is an existing flight
     */
    val isNewFlight: Boolean

    /**
     * A flow of the Flight we are currently working on
     */
    val flightFlow: Flow<ModelFlight>

    /**
     * Get/Set date for this flight without changing times
     */
    var date: LocalDate

    /**
     * Flight Number for this flight
     */
    var flightNumber: String

    /**
     * Origin for this flight
     */
    var orig: Airport

    /**
     * Destination for this flight
     */
    var dest: Airport

    /**
     * timeOut for this flight
     */
    var timeOut: Instant

    /**
     * timeIn for this flight
     */
    var timeIn: Instant

    /**
     * [Aircraft] for this flight
     */
    var aircraft: Aircraft

    /**
     * [TakeoffLandings] for this flight
     */
    var takeoffLandings: TakeoffLandings

    /**
     * PIC Name for this flight
     */
    var name: String

    /**
     * other names for this flight
     */
    var name2: List<String>

    /**
     * Remarks for this flight
     */
    var remarks: String

    /**
     * signature for this flight
     */
    var signature: String

    /**
     * isDual toggle for this flight
     */
    var isDual: Boolean

    /**
     * isInstructor toggle for this flight
     */
    var isInstructor: Boolean

    /**
     * isSim toggle for this flight
     */
    var isSim: Boolean


    /**
     * isMultiPilot toggle for this flight
     */
    var multiPilotTime: Int

    /**
     * IFR Time toggle for this flight
     */
    var ifrTime: Int

    /**
     * Night time for this flight
     */
    var nightTime: Int

    /**
     * Corrected Total Time time for this flight
     */
    var correctedTotalTime: Int

    /**
     * isPIC toggle for this flight
     */
    var isPIC: Boolean

    /**
     * isPICUS toggle for this flight
     */
    var isPICUS: Boolean

    /**
     * isSim toggle for this flight
     */
    var isPF: Boolean

    /**
     * the augmented crew status
     */
    var augmentedCrew: Int

    /**
     * Set isAutoValues toggle for this flight
     */
    var autoFill: Boolean

    /**
     * Save flight to DB
     */
    suspend fun save()

    companion object{
        val instanceFlow: Flow<FlightEditor?> = MutableStateFlow(null)
        private var INSTANCE: FlightEditor? by CastFlowToMutableFlowShortcut(instanceFlow)

        // NOTE this is not a singleton. Take care to close any dialogs editing this WorkingFlight
        // when instanceFlow emits.
        val instance: FlightEditor? get() = INSTANCE

        fun setFromFlight(flight: ModelFlight) {
            INSTANCE = FlightEditorImpl(flight)
        }

        fun setNewflight() = setFromFlight(ModelFlight.createEmpty())

        fun close() {
            INSTANCE = null
        }
    }
}