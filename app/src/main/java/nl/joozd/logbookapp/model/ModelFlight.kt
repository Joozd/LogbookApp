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
import nl.joozd.logbookapp.model.dataclasses.Flight
import nl.joozd.logbookapp.model.workingFlight.TakeoffLandings
import nl.joozd.logbookapp.utils.TwilightCalculator
import java.time.Duration
import java.time.Instant

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
    val unknownToServer: Boolean,                    // Changed 1 means server doesn't know about this flight and it can be safely hard-deleted from DB.
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
        unknownToServer,
        autoFill,
        augmentedCrew,
        DELETEFLAG = false,
        timeStamp = Flight.NO_TIMESTAMP,
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
            takeoffLandings = makeTakeoffLandings().updateTakeoffLandingsForNightTime(),
            multiPilotTime = calculateMultiPilotTime(),
            isCoPilot = isMultiPilot(multiPilotTime)
        )


    private fun isMultiPilot(multiPilotTime: Int) = multiPilotTime != 0 && !isPIC

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

    private fun calculateNightTime(): Int =
        TwilightCalculator(timeOut).minutesOfNight(orig, dest, timeOut, timeIn)

    private fun calculateIfrTime() =
        if (ifrTime != Flight.FLIGHT_IS_VFR)  calculateTotalTime()
        else 0

    private fun calculateMultiPilotTime() =
        if (aircraft.type?.multiPilot == true) calculateTotalTime()
        else 0

    private fun calculateTotalTime(): Int =
        if (isSim) 0
        else AugmentedCrew.of(augmentedCrew).getLogTime(getDurationOfFlight(), isPIC)

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
                name2.split(";"),
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
                unknownToServer,
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