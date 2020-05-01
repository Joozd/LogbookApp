package nl.joozd.logbookapp.data.dataclasses

import nl.joozd.joozdlogcommon.AircraftType

data class AircraftTypeConsensus(
    val registration: String,
    val aircraftType: AircraftType
)