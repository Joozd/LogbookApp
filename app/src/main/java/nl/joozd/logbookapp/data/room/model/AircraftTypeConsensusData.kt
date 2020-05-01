package nl.joozd.logbookapp.data.room.model

import android.annotation.SuppressLint
import androidx.room.ColumnInfo
import androidx.room.Entity
import androidx.room.PrimaryKey

/**
 * This is meant to be refreshed every now and then, once a day maybe, maybe less often, from server.
 * Locally changed will override server
 */

@Suppress("ArrayInDataClass")
@Entity
data class AircraftTypeConsensusData(
    @PrimaryKey
    val registration: String,

    @ColumnInfo(typeAffinity = ColumnInfo.BLOB)
    val serializedType: ByteArray // serialized AircraftType
)
