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

package nl.joozd.logbookapp.data.room.model

import nl.joozd.joozdlogcommon.AircraftType
import nl.joozd.joozdlogcommon.ConsensusData
import nl.joozd.logbookapp.data.dataclasses.AircraftTypeConsensus
import nl.joozd.logbookapp.model.dataclasses.Flight
import nl.joozd.logbookapp.data.dataclasses.FlightData

fun AircraftTypeData.toAircraftType() = AircraftType(
    name,
    shortName,
    multiPilot,
    multiEngine
)

fun AircraftType.toModel() = AircraftTypeData(name, shortName, multiPilot, multiEngine)

fun FlightData.toFlight(): Flight =
    Flight(
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

// This doesn't happen locally, server sends [ConsensusData]
fun AircraftTypeConsensus.toModel() = AircraftTypeConsensusData(registration, aircraftType.serialize())

fun AircraftTypeConsensusData.toAircraftTypeConsensus() = AircraftTypeConsensus(registration, AircraftType.deserialize(serializedType))

fun ConsensusData.toModel() = AircraftTypeConsensusData(registration, serializedType)