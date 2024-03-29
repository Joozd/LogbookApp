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

package nl.joozd.logbookapp.model.dataclasses


import nl.joozd.joozdlogcommon.AugmentedCrew
import nl.joozd.joozdlogcommon.BasicFlight
import nl.joozd.logbookapp.data.dataclasses.FlightData
import java.time.*
import java.time.format.DateTimeFormatter

data class Flight(
    val flightID: Int = FLIGHT_ID_NOT_INITIALIZED,
    val orig: String = "",
    val dest: String = "",
    val timeOut: Long = Instant.now().epochSecond -3600,    // timeOut and timeIn are seconds since epoch
    val timeIn: Long = Instant.now().epochSecond,           // timeOut and timeIn are seconds since epoch
    val correctedTotalTime: Int = 0,                        //  if 0 it will be disregarded
    val multiPilotTime: Int = 0,
    val nightTime: Int = 0,
    val ifrTime: Int = 0,                                   // 0 means 0 minutes, -1 means this is a VFR flight
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
    val isCoPilot: Boolean = false,                         // if true, entire flight time will also be logged as CoPilot
    val isDual: Boolean = false,
    val isInstructor: Boolean = false,
    val isSim: Boolean = false,
    val isPF: Boolean = false,
    val isPlanned: Boolean = true,
    val autoFill: Boolean = true,
    val augmentedCrew: Int = 0,
    val signature: String = ""
){
    //can be constructed as (Flight(BasicFlight))
    constructor(b: BasicFlight): this(b.flightID, b.orig, b.dest, b.timeOut, b.timeIn, b.correctedTotalTime, b.multiPilotTime, b.nightTime, b.ifrTime, b.simTime, b.aircraft, b.registration, b.name, b.name2, b.takeOffDay, b.takeOffNight, b.landingDay, b.landingNight, b.autoLand, b.flightNumber, b.remarks, b.isPIC, b.isPICUS, b.isCoPilot, b.isDual, b.isInstructor, b.isSim, b.isPF, b.isPlanned, b.autoFill, b.augmentedCrew, b.signature)



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
        autoFill,
        augmentedCrew,
        signature
    )
    fun toData() = FlightData(
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
        autoFill,
        augmentedCrew,
        signature
    )

    fun tOut(): LocalDateTime = LocalDateTime.ofInstant(Instant.ofEpochSecond(timeOut), ZoneId.of("UTC"))
    fun tIn(): LocalDateTime = LocalDateTime.ofInstant(Instant.ofEpochSecond(timeIn), ZoneId.of("UTC"))

    fun timeOutString(): String = tOut().format(DateTimeFormatter.ofPattern("HH:mm"))
    fun timeInString(): String = tIn().format(DateTimeFormatter.ofPattern("HH:mm"))
    fun names(): List<String> =
        (listOf(name) +  name2.split(";"))
            .filter{ it.isNotBlank() }
            .map { it.trim() }

    fun date(): LocalDate =
        tOut().toLocalDate()

    //duration in minutes
    val calculatedDuration: Int
        get() = AugmentedCrew.fromInt(augmentedCrew).getLogTime(Duration.between(this.tOut(), this.tIn()).toMinutes().toInt(), this.isPIC)

    /**
     * Get the logged duration of a flight in minutes (corrected for augmented crew and [correctedTotalTime])
     */
    fun duration(): Int = (if (correctedTotalTime != 0) correctedTotalTime else calculatedDuration).let{
        if (it >= 0 ) it else it+86400
    }

    infix fun isExactMatchOf(other: Flight): Boolean =
        this.copy (flightID = 0) == other.copy(flightID = 0)

    companion object{
        const val FLIGHT_ID_NOT_INITIALIZED = -1
        const val FLIGHT_IS_VFR = -1
    }
}