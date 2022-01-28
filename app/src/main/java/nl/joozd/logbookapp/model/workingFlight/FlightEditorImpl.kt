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
import nl.joozd.joozdlogcommon.AircraftType
import nl.joozd.logbookapp.data.dataclasses.Aircraft
import nl.joozd.logbookapp.data.dataclasses.Airport
import nl.joozd.logbookapp.data.miscClasses.crew.AugmentedCrew
import nl.joozd.logbookapp.data.repository.flightRepository.FlightRepositoryWithUndo
import nl.joozd.logbookapp.extensions.atDate
import nl.joozd.logbookapp.extensions.plusDays
import nl.joozd.logbookapp.extensions.toLocalDate
import nl.joozd.logbookapp.model.ModelFlight
import nl.joozd.logbookapp.model.dataclasses.Flight
import nl.joozd.logbookapp.utils.CastFlowToMutableFlowShortcut
import nl.joozd.logbookapp.utils.TwilightCalculator
import java.time.Duration
import java.time.Instant
import java.time.LocalDate

/**
 * This holds a ModelFlight with entries to edit it.
 */
class FlightEditorImpl(flight: ModelFlight): FlightEditor {
    override val isNewFlight: Boolean = flight.flightID == Flight.FLIGHT_ID_NOT_INITIALIZED

    override val flightFlow: Flow<ModelFlight> = MutableStateFlow(flight)

    private var _flight: ModelFlight by CastFlowToMutableFlowShortcut(flightFlow)


    override var flightNumber: String = flight.flightNumber

    override var orig: Airport
        get() = _flight.orig
        set(orig) {
            _flight = _flight.copy (orig = orig).autoValues()
        }

    override var dest: Airport
        get() = _flight.dest
        set(dest) {
            _flight = _flight.copy (dest = dest).autoValues()
        }

    override var timeOut: Instant
        get() = _flight.timeOut
        set(timeOut) {
            _flight = _flight.copy (timeOut = timeOut).autoValues()
        }

    override var timeIn: Instant
        get() = _flight.timeIn
        set(timeIn) {
            _flight = _flight.copy (timeIn = timeIn).autoValues()
        }

    override var date: LocalDate
        get() = timeOut.toLocalDate()
        set(date) {
            val tOut = timeOut.atDate(date)
            val tIn = timeIn.atDate(date).let{
                if (it > timeOut) it
                else it.plusDays(1)
            }
            _flight = _flight.copy (timeOut = tOut, timeIn = tIn).autoValues()
        }

    override var aircraft: Aircraft
        get() = _flight.aircraft
        set(aircraft) {
            _flight = _flight.copy (aircraft = aircraft).autoValues()
        }

    override var takeoffLandings: TakeoffLandings
        get() = _flight.takeoffLandings
        set(takeoffLandings) {
            _flight = _flight.copy (takeoffLandings = takeoffLandings).autoValues()
        }

    override var name: String
        get() = _flight.name
        set(name) {
            _flight = _flight.copy (name = name).autoValues()
        }

    override var name2: List<String>
        get() = _flight.name2
        set(name2) {
            _flight = _flight.copy (name2 = name2).autoValues()
        }

    override var remarks: String
        get() = _flight.remarks
        set(remarks) {
            _flight = _flight.copy (remarks = remarks).autoValues()
        }

    override var multiPilotTime: Int
        get() = _flight.multiPilotTime
        set(multiPilotTime) {
            _flight = _flight.copy (multiPilotTime = multiPilotTime).autoValues()
        }

    override var ifrTime: Int
        get() = _flight.ifrTime
        set(ifrTime){
            if (ifrTime != _flight.ifrTime)
            _flight = _flight.copy(ifrTime = ifrTime, autoFill = false).autoValues()
        }

    override var nightTime: Int
        get() = _flight.nightTime
        set(nightTime) {
            _flight = _flight.copy (nightTime = nightTime).autoValues()
        }

    override var correctedTotalTime: Int            // this is always 0 when autoValues
        get() = _flight.correctedTotalTime
        set(correctedTotalTime) {
            _flight = _flight.copy (correctedTotalTime = correctedTotalTime).autoValues()
        }

    override var augmentedCrew: Int                    // parse this in ViewModel
        get() = _flight.augmentedCrew
        set(augmentedCrew) {
            _flight = _flight.copy (augmentedCrew = augmentedCrew).autoValues()
        }

    override var isSim: Boolean
        get() = _flight.isSim
        set(isSim) {
            _flight = _flight.copy (isSim = isSim).autoValues()
        }

    override var signature: String
        get() = _flight.signature
        set(signature) {
            _flight = _flight.copy (signature = signature).autoValues()
        }

    override var isDual: Boolean
        get() = _flight.isDual
        set(isDual) {
            _flight = _flight.copy (isDual = isDual).autoValues()
        }

    override var isInstructor: Boolean
        get() = _flight.isInstructor
        set(isInstructor) {
            _flight = _flight.copy (isInstructor = isInstructor).autoValues()
        }

    override var isPIC: Boolean
        get() = _flight.isPIC
        set(isPIC) {
            _flight = _flight.copy (isPIC = isPIC).autoValues()
        }

    override var isPICUS: Boolean
        get() = _flight.isPICUS
        set(isPICUS) {
            _flight = _flight.copy (isPICUS = isPICUS).autoValues()
        }

    override var isPF: Boolean
        get() = _flight.isPF
        set(isPF) {
            _flight = _flight.copy (isPF = isPF).autoValues()
        }

    override var autoFill: Boolean
        get() = _flight.autoFill
        set(isAutoValues) {
            _flight = _flight.copy (autoFill = isAutoValues).autoValues()
        }

    override suspend fun save() {
        FlightRepositoryWithUndo.instance.save(_flight.toFlight())
    }
}


