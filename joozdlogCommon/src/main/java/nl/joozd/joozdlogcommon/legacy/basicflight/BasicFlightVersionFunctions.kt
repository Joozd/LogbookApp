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

package nl.joozd.joozdlogcommon.legacy.basicflight

import nl.joozd.joozdlogcommon.BasicFlight
import nl.joozd.joozdlogcommon.BasicFlight_version3
import nl.joozd.joozdlogcommon.BasicFlight_version4
import nl.joozd.serializing.packSerialized
import nl.joozd.serializing.unpackSerialized

object BasicFlightVersionFunctions {
    /**
     * upgrades FlightData to current version
     */
    fun unpackWithVersion(serializedOriginals: ByteArray, actualVersion: Int): List<BasicFlight> = when (actualVersion){
        2 -> unpackSerialized(serializedOriginals).map { fv2 -> upgrade2to3(BasicFlight_version2.deserialize(fv2)) }.map{ fv3 -> upgrade3to4(fv3)}.map{fv4 -> upgrade4to5(fv4)}
        3 -> unpackSerialized(serializedOriginals).map { fv3 -> upgrade3to4(BasicFlight_version3.deserialize(fv3))}.map{fv4 -> upgrade4to5(fv4)}
        4 -> unpackSerialized(serializedOriginals).map { fv4 ->  upgrade4to5(BasicFlight_version4.deserialize (fv4))}
        5 -> unpackSerialized(serializedOriginals).map { fv5 ->  BasicFlight.deserialize(fv5)}
        else -> error("version $actualVersion not supported")
    }

    fun makeVersionAndSerialize(basicFlights: List<BasicFlight>, requestedVersion: Int): ByteArray = when (requestedVersion){
        5 -> packSerialized(basicFlights.map{it.serialize()})
        4 -> packSerialized(basicFlights.map{f -> downgrade5to4(f).serialize()})
        3 -> packSerialized(basicFlights.map{f -> downgrade4to3(downgrade5to4(f)).serialize()})
        else -> error ("version $requestedVersion not supported")
    }


    private fun upgrade2to3(old: BasicFlight_version2): BasicFlight_version3 = with (old) {
        BasicFlight_version3(flightID = flightID, orig = orig, dest = dest, timeOut = timeOut, timeIn = timeIn, correctedTotalTime = correctedTotalTime, nightTime = nightTime, ifrTime = ifrTime, simTime = simTime,
            aircraft = aircraft, registration = registration, name = name, name2 = name2, takeOffDay = takeOffDay, takeOffNight = takeOffNight, landingDay = landingDay, landingNight = landingNight,
            autoLand = autoLand, flightNumber = flightNumber, remarks = remarks, isPIC = isPIC, isPICUS = isPICUS, isCoPilot = isCoPilot, isDual = isDual, isInstructor = isInstructor, isSim = isSim,
            isPF = isPF, isPlanned = isPlanned, changed = changed, autoFill = autoFill, augmentedCrew = augmentedCrew, DELETEFLAG = DELETEFLAG, timeStamp = timeStamp, signature = ""
        )
    }
    private fun upgrade3to4(old: BasicFlight_version3): BasicFlight_version4 = with (old) {
        BasicFlight_version4(flightID = flightID, orig = orig, dest = dest, timeOut = timeOut, timeIn = timeIn, correctedTotalTime = correctedTotalTime, nightTime = nightTime, ifrTime = ifrTime, simTime = simTime,
            aircraft = aircraft, registration = registration, name = name, name2 = name2, takeOffDay = takeOffDay, takeOffNight = takeOffNight, landingDay = landingDay, landingNight = landingNight,
            autoLand = autoLand, flightNumber = flightNumber, remarks = remarks, isPIC = isPIC.toBoolean(), isPICUS = isPICUS.toBoolean(), isCoPilot = isCoPilot.toBoolean(), isDual = isDual.toBoolean(), isInstructor = isInstructor.toBoolean(), isSim = isSim.toBoolean(),
            isPF = isPF.toBoolean(), isPlanned = isPlanned.toBoolean(), changed = changed.toBoolean(), autoFill = autoFill.toBoolean(), augmentedCrew = augmentedCrew, DELETEFLAG = DELETEFLAG.toBoolean(), timeStamp = timeStamp, signature = signature
        )
    }

    fun upgrade4to5(old: BasicFlight_version4): BasicFlight = with (old) {
        BasicFlight(flightID = flightID, orig = orig, dest = dest, timeOut = timeOut, timeIn = timeIn, correctedTotalTime = correctedTotalTime, multiPilotTime = 0, nightTime = nightTime, ifrTime = ifrTime, simTime = simTime,
            aircraft = aircraft, registration = registration, name = name, name2 = name2, takeOffDay = takeOffDay, takeOffNight = takeOffNight, landingDay = landingDay, landingNight = landingNight,
            autoLand = autoLand, flightNumber = flightNumber, remarks = remarks, isPIC = isPIC, isPICUS = isPICUS, isCoPilot = isCoPilot, isDual = isDual, isInstructor = isInstructor, isSim = isSim,
            isPF = isPF, isPlanned = isPlanned, unknownToServer = changed, autoFill = autoFill, augmentedCrew = augmentedCrew, DELETEFLAG = DELETEFLAG, timeStamp = timeStamp, signature = signature
        )
    }

    private fun downgrade4to3(original: BasicFlight_version4): BasicFlight_version3 = with (original){
        BasicFlight_version3(flightID = flightID, orig = orig, dest = dest, timeOut = timeOut, timeIn = timeIn, correctedTotalTime = correctedTotalTime, nightTime = nightTime, ifrTime = ifrTime, simTime = simTime,
            aircraft = aircraft, registration = registration, name = name, name2 = name2, takeOffDay = takeOffDay, takeOffNight = takeOffNight, landingDay = landingDay, landingNight = landingNight,
            autoLand = autoLand, flightNumber = flightNumber, remarks = remarks, isPIC = isPIC.toInt(), isPICUS = isPICUS.toInt(), isCoPilot = isCoPilot.toInt(), isDual = isDual.toInt(), isInstructor = isInstructor.toInt(), isSim = isSim.toInt(),
            isPF = isPF.toInt(), isPlanned = isPlanned.toInt(), changed = changed.toInt(), autoFill = autoFill.toInt(), augmentedCrew = augmentedCrew, DELETEFLAG = DELETEFLAG.toInt(), timeStamp = timeStamp, signature = signature
        )
    }

    private fun downgrade5to4(original: BasicFlight): BasicFlight_version4 = with (original){
        BasicFlight_version4(flightID = flightID, orig = orig, dest = dest, timeOut = timeOut, timeIn = timeIn, correctedTotalTime = correctedTotalTime, nightTime = nightTime, ifrTime = ifrTime, simTime = simTime,
            aircraft = aircraft, registration = registration, name = name, name2 = name2, takeOffDay = takeOffDay, takeOffNight = takeOffNight, landingDay = landingDay, landingNight = landingNight,
            autoLand = autoLand, flightNumber = flightNumber, remarks = remarks, isPIC = isPIC, isPICUS = isPICUS, isCoPilot = isCoPilot, isDual = isDual, isInstructor = isInstructor, isSim = isSim,
            isPF = isPF, isPlanned = isPlanned, changed = unknownToServer, autoFill = autoFill, augmentedCrew = augmentedCrew, DELETEFLAG = DELETEFLAG, timeStamp = timeStamp, signature = signature
        )
    }


    private fun Int.toBoolean() = this > 0
    private fun Boolean.toInt() = if (this) 1 else 0
}