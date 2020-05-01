package nl.joozd.logbookapp.data.room.model

import nl.joozd.joozdlogcommon.AircraftType
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

fun AircraftTypeConsensus.toModel() = AircraftTypeConsensusData(registration, aircraftType.serialize())

fun AircraftTypeConsensusData.toAircraftTypeConsensus() = AircraftTypeConsensus(registration, AircraftType.deserialize(serializedType))