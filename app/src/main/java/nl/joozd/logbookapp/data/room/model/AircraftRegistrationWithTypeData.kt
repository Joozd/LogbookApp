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

import androidx.room.Entity
import androidx.room.PrimaryKey

/**
 * This class matches aircraft registrations to types
 * This is what is used FIRST if looking to match an aircraft to a type
 * @param registration: Primary key, aircraft registration (can be any string, but usually something like "PH-EZE"
 * @param type: AircraftTypeData.name<String> to match with AircraftType
 * @param knownToServer: If false, let server know about this for consensus
 * @param previousType: Previous type known by server
 *      if ![knownToServer] this is to be sent to server for consensus (subtract counter)
 *      if [knownToServer], if [type] is changed, change this to previous type
 */

@Entity
data class AircraftRegistrationWithTypeData(
    @PrimaryKey val registration: String,
    var type: String = UNKNOWN,
    var knownToServer: Boolean = false,
    var previousType: String = UNKNOWN,
    var timestamp: Long = -1
){
    companion object {
        const val UNKNOWN = "UNKNOWN_TYPE"
    }
}