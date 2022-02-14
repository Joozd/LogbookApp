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

// 3 minutes will become "0:03", 6003 minutes will become  "100:03"
fun Int.minutesToHoursAndMinutesString(): String =
    "${this/60}:${(this%60).toString().padStart(2,'0')}"

fun Long.minutesToHoursAndMinutesString() = this.toInt().minutesToHoursAndMinutesString()

/**
 * A string that represents hours and minutes gets converted to an Int that represents minutes.
 * Last two digits are minutes, any before that are hours (so 123456 == 1234:56)
 * 76 minutes is fine, 104 is 64 minutes.
 * Users will probably enter something like 330 when they mean 3:30.
 * Allowed characters are digits and [+- :/.h] (without square brackets)
 * Empty string is 0, invalid input is null.
 * Examples:
 * 23 = 23
 * 83 = 83; 1:23 = 83; 123 = 83
 * 123456 = 1234:56 = 74096
 */
fun String.hoursAndMinutesStringToInt(): Int? {
    if (this.isBlank()) return 0

    val hoursAndMinutesSplits = splitIntoHoursAndMinutes(this)

    val hoursAndMinutesInts = try {
        hoursAndMinutesSplits.map{it.toInt()}
    } catch (e: NumberFormatException) { return null } // if not only digits left,

    if (hoursAndMinutesInts.isEmpty()) return null // This happens when only characters in "+- :/.h" were in string

    return if (hoursAndMinutesInts.size == 1) {
        val v = hoursAndMinutesInts.first()

        if (v <= 99) v
        else v%100 + v/100*60
    } else hoursAndMinutesInts[0]*60 + hoursAndMinutesInts[1]
}

private fun splitIntoHoursAndMinutes(hoursAndMinutes: String) =
    hoursAndMinutes.trim()
        .split(*"+- :/.h".toCharArray())
        .filter { it.isNotBlank() } // remove any excess blank spaces that might have been in here