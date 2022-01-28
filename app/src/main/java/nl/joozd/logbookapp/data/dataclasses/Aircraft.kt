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

package nl.joozd.logbookapp.data.dataclasses

import kotlinx.coroutines.*
import nl.joozd.joozdlogcommon.AircraftType

/**
 * This is one specific Aircraft. It has a [registration], a [type], and a [source] for that type
 */
data class Aircraft(val registration: String = "", val type: AircraftType? = null, val source: Int = NONE): CoroutineScope by MainScope() {
    companion object{
        const val NONE = 999
        const val KNOWN = 1         // user has set it this way
        const val FLIGHT= 2         // Type was in [Flight]
        const val FLIGHT_CONFLICTING = 20         // Type was in [Flight] but not all occurences agree
        const val PRELOADED = 3     // Set from server's forcedType
        const val CONSENSUS = 4     // Set from server's consensus
        const val PREVIOUS = 5      // Set because previously selected was this type
    }

    override fun toString() = type?.let{
        "$registration(${type.shortName})"
    } ?: registration
}