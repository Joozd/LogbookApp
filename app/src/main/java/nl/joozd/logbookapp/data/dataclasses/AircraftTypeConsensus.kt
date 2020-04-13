package nl.joozd.logbookapp.data.dataclasses

import androidx.room.ColumnInfo
import androidx.room.Entity
import androidx.room.PrimaryKey

/**
 * This is meant to be refreshed every now and then, once a day maybe, maybe less often, from server
 */

@Entity
data class AircraftTypeConsensus(
    @PrimaryKey
    val registration: String,

    @ColumnInfo(typeAffinity = ColumnInfo.BLOB)
    val serializedType: ByteArray
)
