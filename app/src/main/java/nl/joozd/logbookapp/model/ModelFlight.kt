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

package nl.joozd.logbookapp.model

import nl.joozd.logbookapp.data.dataclasses.Aircraft
import nl.joozd.logbookapp.data.dataclasses.Airport
import nl.joozd.logbookapp.data.miscClasses.crew.AugmentedCrew
import nl.joozd.logbookapp.data.repository.aircraftrepository.AircraftDataCache
import nl.joozd.logbookapp.data.repository.aircraftrepository.AircraftRepository
import nl.joozd.logbookapp.data.repository.airportrepository.AirportDataCache
import nl.joozd.logbookapp.data.repository.airportrepository.AirportRepository
import nl.joozd.logbookapp.extensions.checkIfValidCoordinates
import nl.joozd.logbookapp.extensions.toLocalDate
import nl.joozd.logbookapp.model.dataclasses.Flight
import nl.joozd.logbookapp.model.workingFlight.TakeoffLandings
import nl.joozd.logbookapp.utils.TwilightCalculator
import java.time.Duration
import java.time.Instant
import java.time.LocalDate
import java.time.format.DateTimeFormatter
import java.time.format.FormatStyle

/**
 * Flight with actual Airport and Aircraft
 * Used for manipulating a Flight in model layer.
 * Can be converted back to [Flight]
 * or constructed with [ofFlightAndRepositories] or [createEmpty]
 */
data class ModelFlight(
    val flightID: Int,
    val orig: Airport,
    val dest: Airport,
    val timeOut: Instant,
    val timeIn: Instant,
    val correctedTotalTime: Int,                        //  if 0 it will be disregarded
    val multiPilotTime: Int,
    val nightTime: Int,
    val ifrTime: Int,                                    // 0 means 0 minutes, -1 means this is a VFR flight
    val simTime: Int,
    val aircraft: Aircraft,
    val name: String,
    val name2: List<String>,
    val takeoffLandings: TakeoffLandings,
    val flightNumber: String,
    val remarks: String,
    val isPIC: Boolean,
    val isPICUS: Boolean,
    val isCoPilot: Boolean,                         // if true, entire flight time will also be logged as CoPilot
    val isDual: Boolean,
    val isInstructor: Boolean,
    val isSim: Boolean,
    val isPF: Boolean,
    val isPlanned: Boolean,
    val autoFill: Boolean,
    val augmentedCrew: Int,                             // timestamp should be moment of creation / last change, or when incoming sync: timestamp of sync
    val signature: String
){
    fun toFlight() = Flight(
        flightID,
        orig.ident,
        dest.ident,
        timeOut.epochSecond,
        timeIn.epochSecond,
        correctedTotalTime,
        multiPilotTime,
        nightTime,
        ifrTime,
        simTime,
        aircraft.type?.shortName ?: "",
        aircraft.registration,
        name,
        name2.joinToString(";"),
        takeoffLandings.takeoffDay,
        takeoffLandings.takeoffNight,
        takeoffLandings.landingDay,
        takeoffLandings.landingNight,
        takeoffLandings.autoLand,
        flightNumber,
        remarks,
        isPIC,
        isPICUS,
        isCoPilot,
        isDual,
        isInstructor,
        isSim,
        isPF,
        isPlanned,
        autoFill,
        augmentedCrew,
        signature
    )

    /*
     * autoValues makes sure all related values change if a certain field is changed.
     * Values that need to be calculated:
     * - IFR time
     * - Night time
     * - Takeoff/Landings day/night
     * - multi Pilot Time
     * - isCopilot
     */
    fun autoValues(): ModelFlight =
        if(!autoFill) this
        else this.copy(
            correctedTotalTime = 0,
            ifrTime = calculateIfrTime(),
            nightTime = calculateNightTime(),
            takeoffLandings = takeoffLandings.updateTakeoffLandingsForNightTime(), // This only checks for night time. If toggling isPF should toggle takeoff and landing, call [setTakeoffLandingsIfIsPFChanged]
            multiPilotTime = calculateMultiPilotTime(),
            isCoPilot = isCopilot()
        )

    /**
     * Call this after isPF gets changed.
     * Calling this will update takeoff/landing when:
     * - isPF == true AND to/ldg was 0/0: 1/1
     * - isPF == false AND to/ldg was 1/1: 0/0
     * - else: do not change to/ldg
     */
    fun setTakeoffLandingsForIsPF(): ModelFlight =
        this.copy(takeoffLandings = makeTakeoffLandings())


    fun date(): LocalDate = timeOut.toLocalDate()

    fun dateString(): String {
        val dateFormatter = DateTimeFormatter.ofLocalizedDate(FormatStyle.MEDIUM)
        return date().format(dateFormatter)
    }

    /**
     * Calculates rest time (difference between [getDurationOfFlight] and [calculateTotalTime]
     */
    fun restTime() = getDurationOfFlight().toMinutes() - calculateTotalTime()


    private fun isCopilot() = multiPilotTime != 0 && !isPIC



    private fun makeTakeoffLandings(): TakeoffLandings =
        when{
            takeoffLandings.oneTakeoffOneLanding() && !isPF -> TakeoffLandings(0,0)
            takeoffLandings.noTakeoffNoLanding() && isPF -> TakeoffLandings(1,1)
            else -> takeoffLandings // leave them as is
        }



    /*
     * Will only work for 1/1 takeoff/landing, otherwise will do nothing.
     */
    private fun TakeoffLandings.updateTakeoffLandingsForNightTime(): TakeoffLandings =
        if (oneTakeoffOneLanding()){
            val takeoffDuringDay = TwilightCalculator(timeOut).itIsDayAt(orig, timeOut)
            val landingDuringDay = TwilightCalculator(timeIn).itIsDayAt(dest, timeIn)
            val toDay = if (takeoffDuringDay) 1 else 0
            val toNight = 1 - toDay
            val ldgDay = if (landingDuringDay) 1 else 0
            val ldgNight = 1 - ldgDay
            TakeoffLandings(toDay, toNight, ldgDay, ldgNight, autoLand)
        }
        else this

    private fun TakeoffLandings.oneTakeoffOneLanding() =
        takeOffs == 1 && landings == 1

    private fun TakeoffLandings.noTakeoffNoLanding() =
        takeOffs == 0 && landings == 0

    private fun calculateNightTime(): Int {
        if (!orig.checkIfValidCoordinates() || !dest.checkIfValidCoordinates()) return 0
        val outToInMinutes = getDurationOfFlight().toMinutes().toInt()
        if (outToInMinutes == 0) return 0
        val totalNightMinutes =
            TwilightCalculator(timeOut).minutesOfNight(orig, dest, timeOut, timeIn)
        return totalNightMinutes * calculateTotalTime() / outToInMinutes
    }

    private fun calculateIfrTime() =
        if (ifrTime != Flight.FLIGHT_IS_VFR)  calculateTotalTime()
        else Flight.FLIGHT_IS_VFR

    private fun calculateMultiPilotTime() =
        if (aircraft.type?.multiPilot == true) calculateTotalTime()
        else 0

    /**
     * Total loggable time, in minutes. Rest time (for augmented crews) is removed.
     */
    fun calculateTotalTime(): Int = when {
        isSim -> 0
        correctedTotalTime != 0 -> correctedTotalTime
        else -> AugmentedCrew.fromInt(augmentedCrew).getLogTime(getDurationOfFlight(), isPIC)
    }

    private fun getDurationOfFlight(): Duration =
        Duration.between(timeOut, timeIn)

    companion object{
        suspend fun ofFlightAndRepositories(
            flight: Flight,
            aircraftRepository: AircraftRepository = AircraftRepository.instance,
            airportRepository: AirportRepository = AirportRepository.instance
        ): ModelFlight{
            with (flight) {
                val origin = airportRepository.getAirportByIcaoIdentOrNull(orig)
                    ?: Airport(ident = flight.orig)
                val destination = airportRepository.getAirportByIcaoIdentOrNull(dest)
                    ?: Airport(ident = dest)
                val aircraft = makeAircraft(flight, aircraftRepository)

                return makeModelFlight(origin, destination, aircraft)
            }
        }

        fun ofFlightAndDataCaches(flight: Flight, airportDataCache: AirportDataCache, aircraftDataCache: AircraftDataCache): ModelFlight{
            with (flight) {
                val origin = airportDataCache.getAirportByIcaoIdentOrNull(orig)
                    ?: Airport(ident = flight.orig)
                val destination = airportDataCache.getAirportByIcaoIdentOrNull(dest)
                    ?: Airport(ident = dest)
                val aircraft = makeAircraft(flight, aircraftDataCache)

                return makeModelFlight(origin, destination, aircraft)
            }
        }

        fun createEmpty(): ModelFlight = Flight().makeModelFlight()

        private fun Flight.makeModelFlight(
            origin: Airport = Airport(),
            destination: Airport = Airport(),
            aircraft: Aircraft = Aircraft(),
        ): ModelFlight {
            val tOut = Instant.ofEpochSecond(timeOut)
            val tIn = Instant.ofEpochSecond(timeIn)
            val takeoffLandings = TakeoffLandings.fromFlight(this)
            return ModelFlight(
                flightID,
                origin,
                destination,
                tOut,
                tIn,
                correctedTotalTime,
                multiPilotTime,
                nightTime,
                ifrTime,
                simTime,
                aircraft,
                name,
                name2.split(";").filter{ it.isNotBlank() },
                takeoffLandings,
                flightNumber,
                remarks,
                isPIC,
                isPICUS,
                isCoPilot,
                isDual,
                isInstructor,
                isSim,
                isPF,
                isPlanned,
                autoFill,
                augmentedCrew,
                signature
            )
        }

        private suspend fun makeAircraft(flight: Flight, aircraftRepository: AircraftRepository): Aircraft{
            val type = aircraftRepository.getAircraftTypeByShortName(flight.aircraftType)
            return Aircraft(
                registration = flight.registration,
                type = type,
                source = Aircraft.FLIGHT
            )
        }

        private fun makeAircraft(flight: Flight, aircraftDataCache: AircraftDataCache): Aircraft{
            val type = aircraftDataCache.getAircraftTypeByShortName(flight.aircraftType)
            return Aircraft(
                registration = flight.registration,
                type = type,
                source = Aircraft.FLIGHT
            )
        }


    }
}