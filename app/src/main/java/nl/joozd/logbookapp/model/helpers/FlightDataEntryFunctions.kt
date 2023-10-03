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

package nl.joozd.logbookapp.model.helpers
import nl.joozd.logbookapp.extensions.nullIfEmpty

object FlightDataEntryFunctions {
    fun hoursAndMinutesStringToInt(hoursAndMinutes: String?): Int? {
        if (hoursAndMinutes == null) return null
        if (hoursAndMinutes.all{it.isDigit()}) {
            return if (hoursAndMinutes.length <= 2)
                (hoursAndMinutes.nullIfEmpty() ?: "0").toInt()
            else hoursAndMinutes.takeLast(2).toInt() + hoursAndMinutes.dropLast(2).toInt()*60
        }
        val hoursAndMinutesSplits = hoursAndMinutes.split(*"+- :/.h".toCharArray())
        //check if only digits left
        if (hoursAndMinutesSplits.joinToString("").any{!it.isDigit()}) return null
        val hoursAndMinutesInts = hoursAndMinutesSplits.map{it.toInt()}
        if (hoursAndMinutesInts.size == 1) return hoursAndMinutesInts[0]
        return hoursAndMinutesInts[0]*60 + hoursAndMinutesInts[1]
    }
}