/*
 * JoozdLog Pilot's Logbook
 * Copyright (C) 2020 Joost Welle
 *
 *     This program is free software: you can redistribute it and/or modify
 *     it under the terms of the GNU Affero General Public License as
 *     published by the Free Software Foundation, either version 3 of the
 *     License, or (at your option) any later version.
 *
 *     This program is distributed in the hope that it will be useful,
 *     but WITHOUT ANY WARRANTY; without even the implied warranty of
 *     MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 *     GNU Affero General Public License for more details.
 *
 *     You should have received a copy of the GNU Affero General Public License
 *     along with this program.  If not, see https://www.gnu.org/licenses
 */

/*
 * JoozdLog Pilot's Logbook
 * Copyright (C) 2020 Joost Welle
 *
 *     This program is free software: you can redistribute it and/or modify
 *     it under the terms of the GNU Affero General Public License as
 *     published by the Free Software Foundation, either version 3 of the
 *     License, or (at your option) any later version.
 *
 *     This program is distributed in the hope that it will be useful,
 *     but WITHOUT ANY WARRANTY; without even the implied warranty of
 *     MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 *     GNU Affero General Public License for more details.
 *
 *     You should have received a copy of the GNU Affero General Public License
 *     along with this program.  If not, see https://www.gnu.org/licenses
 */

package nl.joozd.logbookapp.data.dataclasses

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

import androidx.room.Entity
import androidx.room.PrimaryKey
import nl.joozd.logbookapp.data.miscClasses.Crew
import nl.joozd.logbookapp.data.room.model.FlightData
import nl.joozd.joozdlogcommon.BasicFlight
import java.time.*
import java.time.format.DateTimeFormatter

@Entity
data class Flight(
    @PrimaryKey val flightID: Int,
    val orig: String = "",
    val dest: String = "",
    val timeOut: Long = Instant.now().epochSecond -3600,    // timeOut and timeIn are seconds since epoch
    val timeIn: Long = Instant.now().epochSecond,           // timeOut and timeIn are seconds since epoch
    val correctedTotalTime: Int = 0,
    val nightTime: Int = 0,
    val ifrTime:Int = 0,
    val simTime: Int = 0,
    val aircraft: String = "",                              // overrides standard AircraftType that goes with registration
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
    val isPIC: Int = 0,
    val isPICUS: Int = 0,
    val isCoPilot: Int = 0,
    val isDual: Int = 0,
    val isInstructor: Int = 0,
    val isSim: Int = 0,
    val isPF: Int = 0,
    val isPlanned: Int = 1,
    val changed: Int = 1,                                   // Changed 1 means server doesn't know about this flight and it can be safely hard-deleted from DB.
    val autoFill: Int = 1,
    val augmentedCrew: Int = 0,
    val DELETEFLAG: Int = 0,
    val timeStamp: Long = -1,                               // timestamp should be moment of creation / last change, or when incoming sync: timestamp of sync
    val signature: String = ""
    // val signed: Boolean = false



){
    companion object{
        private fun correctDuration(duration: Duration, crew: Crew): Duration{
            var flownMinutes: Long = 0
            if (crew.didLanding) flownMinutes += crew.takeoffLandingTimes
            if (crew.didTakeoff) flownMinutes += crew.takeoffLandingTimes
            flownMinutes += ((duration.toMinutes()-2*crew.takeoffLandingTimes) / crew.crewSize) *2
            return Duration.ofMinutes(flownMinutes)
        }

        fun createEmpty() = Flight(-1)
    }

    //can be constructed as (Flight(BasicFlight))
    constructor(b: BasicFlight): this(b.flightID, b.orig, b.dest, b.timeOut, b.timeIn, b.correctedTotalTime, b.nightTime, b.ifrTime, b.simTime, b.aircraft, b.registration, b.name, b.name2, b.takeOffDay, b.takeOffNight, b.landingDay, b.landingNight, b.autoLand, b.flightNumber, b.remarks, b.isPIC, b.isPICUS, b.isCoPilot, b.isDual, b.isInstructor, b.isSim, b.isPF, b.isPlanned, b.changed, b.autoFill, b.augmentedCrew, b.DELETEFLAG, b.timeStamp /*b.signed*/)

    fun toBasicFlight() = BasicFlight(
        flightID,
        orig,
        dest,
        timeOut,
        timeIn,
        correctedTotalTime,
        nightTime,
        ifrTime,
        simTime,
        aircraft,
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
        changed,
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
        nightTime,
        ifrTime,
        simTime,
        aircraft,
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
        changed,
        autoFill,
        augmentedCrew,
        DELETEFLAG,
        timeStamp,
        signature
    )

    var actualAircraft: Aircraft? = null


    private val dateFormatter = DateTimeFormatter.ofPattern("dd MMM yyyy")
    private val timeFormatter = DateTimeFormatter.ofPattern("HH:mm") // should always be in Z
    // timeOut and timeIn are seconds since epoch
    // nightTime, ifrTime are number of minutes
    // isPIC etc are '0' for false and '1' for true
    // TODO ifrTime "0" is standard by aircraft

    val allNames: String
    val takeoffLanding: String
    init{
        var allNamesBuilder=name
        if (name2 != "") allNamesBuilder += ", $name2"
        allNames = allNamesBuilder
        takeoffLanding = "${takeOffNight+takeOffDay}/${landingNight+landingDay}"
    }

    val tOut: LocalDateTime = LocalDateTime.ofInstant(Instant.ofEpochSecond(timeOut), ZoneId.of("UTC"))
    val tIn: LocalDateTime = LocalDateTime.ofInstant(Instant.ofEpochSecond(timeIn), ZoneId.of("UTC"))
    val dateOut = tOut.toLocalDate()
    val timeOutString: String = tOut.format(timeFormatter)
    val timeInString: String = tIn.format(timeFormatter)

    val duration: Duration = Duration.between(tOut, tIn)

    val pic = isPIC > 0
    val picus = isPICUS > 0
    val coPilot = isCoPilot > 0
    val dual = isDual > 0
    val instructor = isInstructor > 0
    val sim = isSim > 0
    val pf = isPF > 0
    val planned = isPlanned > 0 // only planned flights should be deletable, not planned is flown
    val crew = Crew.of(augmentedCrew)
    private val durationNeedsCorrecting = crew.crewSize > 2
    val correctedDuration: Duration =
        if (durationNeedsCorrecting && autoFill > 0)
            if (correctDuration(
                    duration,
                    crew
                ).toMinutes() > 0)
                correctDuration(
                    duration,
                    crew
                )
            else Duration.ZERO
        else duration

    val date: String = tOut.format(dateFormatter)

    //val flightTimes = "$timeOutString-$timeInString"
    val totalTimeNoHrs = "${duration.seconds/3600}:${((duration.seconds%3600)/60).toString().padStart(2,'0')}"
    val correctedTotalTimeNoHrs = if (durationNeedsCorrecting) "${correctedDuration.seconds/3600}:${((correctedDuration.seconds%3600)/60).toString().padStart(2,'0')}" else totalTimeNoHrs
    val totalTime = "$totalTimeNoHrs hrs"
    val correctedTotalTimeString = "$correctedTotalTimeNoHrs hrs"
    val simTimeNoHrs = "${simTime/60}:${(simTime%60).toString().padStart(2,'0')}"
    val simTimeString =  "$simTimeNoHrs hrs"

    // name2 is a comma separated string of a million names

}