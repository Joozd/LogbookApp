/*
 *  JoozdLog Pilot's Logbook
 *  Copyright (c) 2020 Joost Welle
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

package nl.joozd.logbookapp.model.dataclasses

/*****************
 * ADDING/REMOVING FIELDS: change:
 * -x this
 * -x flightDbHelper - also update table version!
 * -x FlightTable (in Tables.kt)
 * -x FlightData in DbClasses.kt
 * -x DbDataMapper: convertFlightsToDomain and convertFlightsFromDomain
 *
 * ON SERVER:
 * -x Flight.py
 * -x PdfWorker buildFlight
 *
 * -x rebuild user data, when in production make sure backwards compatibility is here!
 */

import nl.joozd.logbookapp.data.miscClasses.crew.Crew
import nl.joozd.joozdlogcommon.BasicFlight
import nl.joozd.logbookapp.data.dataclasses.FlightData
import nl.joozd.logbookapp.model.helpers.FlightDataPresentationFunctions
import java.time.*

data class Flight(
    val flightID: Int,
    val orig: String = "",
    val dest: String = "",
    val timeOut: Long = Instant.now().epochSecond -3600,    // timeOut and timeIn are seconds since epoch
    val timeIn: Long = Instant.now().epochSecond,           // timeOut and timeIn are seconds since epoch
    val correctedTotalTime: Int = 0,                        //  if 0 it will be disregarded
    val multiPilotTime: Int = 0,
    val nightTime: Int = 0,
    val ifrTime: Int = 0,                                    // 0 means 0 minutes, -1 means this is a VFR flight
    val simTime: Int = 0,
    val aircraftType: String = "",                          // overrides standard AircraftType that goes with registration
    val registration: String = "",
    val name: String = "",
    val name2: String = "",
    val takeOffDay: Int = 0,
    val takeOffNight: Int = 0,
    val landingDay: Int = 0,
    val landingNight: Int = 0,
    val autoLand: Int = 0,
    val flightNumber: String = "",
    val remarks: String = "",
    val isPIC: Boolean = false,
    val isPICUS: Boolean = false,
    val isCoPilot: Boolean = false,
    val isDual: Boolean = false,
    val isInstructor: Boolean = false,
    val isSim: Boolean = false,
    val isPF: Boolean = false,
    val isPlanned: Boolean = true,
    val unknownToServer: Boolean = true,                    // Changed 1 means server doesn't know about this flight and it can be safely hard-deleted from DB.
    val autoFill: Boolean = true,
    val augmentedCrew: Int = 0,
    val DELETEFLAG: Boolean = false,
    val timeStamp: Long = -1,                               // timestamp should be moment of creation / last change, or when incoming sync: timestamp of sync
    val signature: String = ""
){
    //can be constructed as (Flight(BasicFlight))
    constructor(b: BasicFlight): this(b.flightID, b.orig, b.dest, b.timeOut, b.timeIn, b.correctedTotalTime, b.multiPilotTime, b.nightTime, b.ifrTime, b.simTime, b.aircraft, b.registration, b.name, b.name2, b.takeOffDay, b.takeOffNight, b.landingDay, b.landingNight, b.autoLand, b.flightNumber, b.remarks, b.isPIC, b.isPICUS, b.isCoPilot, b.isDual, b.isInstructor, b.isSim, b.isPF, b.isPlanned, b.changed, b.autoFill, b.augmentedCrew, b.DELETEFLAG, b.timeStamp, b.signature)

    fun toBasicFlight() = BasicFlight(
        flightID,
        orig,
        dest,
        timeOut,
        timeIn,
        correctedTotalTime,
        multiPilotTime,
        nightTime,
        ifrTime,
        simTime,
        aircraftType,
        registration,
        name,
        name2,
        takeOffDay,
        takeOffNight,
        landingDay,
        landingNight,
        autoLand,
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
        DELETEFLAG,
        timeStamp,
        signature
    )
    fun toModel() = FlightData(
        flightID,
        orig,
        dest,
        timeOut,
        timeIn,
        correctedTotalTime,
        multiPilotTime,
        nightTime,
        ifrTime,
        simTime,
        aircraftType,
        registration,
        name,
        name2,
        takeOffDay,
        takeOffNight,
        landingDay,
        landingNight,
        autoLand,
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
        DELETEFLAG,
        timeStamp,
        signature
    )

    fun landings() = "${landingDay + landingNight}"
    fun takeoffs() = "${takeOffDay + takeOffNight}"
    fun tOut(): LocalDateTime = LocalDateTime.ofInstant(Instant.ofEpochSecond(timeOut), ZoneId.of("UTC"))
    fun tIn(): LocalDateTime = LocalDateTime.ofInstant(Instant.ofEpochSecond(timeIn), ZoneId.of("UTC"))
    fun timeOutString(): String = tOut().format(FlightDataPresentationFunctions.flightTimeFormatter)
    fun timeInString(): String = tIn().format(FlightDataPresentationFunctions.flightTimeFormatter)

    //duration in minutes
    val calculatedDuration: Int
        get() = Crew.of(augmentedCrew).getLogTime(Duration.between(this.tOut(), this.tIn()).toMinutes().toInt(), this.isPIC)

    /**
     * Get the logged duration of a flight in minutes (corrected for augmented crew and [correctedTotalTime])
     */
    fun duration(): Int = (if (correctedTotalTime != 0) correctedTotalTime else calculatedDuration).let{
        // if (it > 0) it else ((it+24*60).also { Log.w("FLIGHT", "NEGATIVE TIME, FIXING AT RUNTIME FOR FLIGHT $this") })
        if (it >= 0 ) it else it+86400

    }

    fun durationString() = FlightDataPresentationFunctions.minutesToHoursAndMinutesString(duration())

    companion object{
        fun createEmpty() = Flight(-1)
    }
}