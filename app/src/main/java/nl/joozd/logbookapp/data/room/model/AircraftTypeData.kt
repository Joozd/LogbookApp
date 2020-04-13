package nl.joozd.logbookapp.data.room.model

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity
data class AircraftTypeData(
    @PrimaryKey val name: String,
    val shortName: String,
    val multiPilot:Boolean,
    val multiEngine:Boolean
)