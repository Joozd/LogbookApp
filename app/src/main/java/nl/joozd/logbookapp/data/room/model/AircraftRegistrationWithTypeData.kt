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

package nl.joozd.logbookapp.data.room.model

import androidx.room.ColumnInfo
import androidx.room.Entity
import androidx.room.PrimaryKey

/**
 * This class matches aircraft registrations to types
 * This is what is used FIRST if looking to match an aircraft to a type
 * @param registration: Primary key, aircraft registration (can be any string, but usually something like "PH-EZE"
 * @param serializedType: Serialized AircraftType
 * @param knownToServer: knownToServer is not used anymore, I am leaving it in because I don't want to update database. (I am lazy)
 * @param serializedPreviousType: previousType is not used anymore, I am leaving it in because I don't want to update database. (I am lazy)
 */
@Suppress("ArrayInDataClass")
@Entity
data class AircraftRegistrationWithTypeData(
    @PrimaryKey val registration: String,

    @ColumnInfo(typeAffinity = ColumnInfo.BLOB)
    var serializedType: ByteArray,
    var knownToServer: Boolean = false,

    @ColumnInfo(typeAffinity = ColumnInfo.BLOB)
    var serializedPreviousType: ByteArray, // serialized AircraftType
){
    companion object {
        const val UNKNOWN = "UNKNOWN_TYPE"
    }
}