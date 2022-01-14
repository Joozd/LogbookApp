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

package nl.joozd.logbookapp.extensions

import java.util.*

infix fun String.inIgnoreCase(other: String): Boolean =
    this.uppercase(Locale.ROOT) in other.uppercase(Locale.ROOT)

fun String.nullIfEmpty() = if (this.isEmpty()) null else this

fun String.nullIfBlank() = if (this.isBlank()) null else this

fun String.removeTrailingDigits(): String {
    if (isEmpty()) return this
    val trailingDigits = countHowManyCharsAtTheEndAreDigits()
    return this.dropLast(trailingDigits)
}


private fun String.countHowManyCharsAtTheEndAreDigits(): Int{
    var count = 0
    while (getOrNull(indices.last-count)?.isDigit() == true) count++
    return count
}


fun String.anyWordStartsWith(substring: String, ignoreCase: Boolean = false) =
    if (ignoreCase) anyWordStartsWithIgnoreCase(substring)
    else anyWordStartsWithKeepCase(substring)

private fun String.anyWordStartsWithKeepCase(substring: String) =
    this.split(" ").any { it.startsWith(substring) }

private fun String.anyWordStartsWithIgnoreCase(substring: String) =
    this.uppercase(Locale.ROOT).split(" ").any { it.startsWith(substring.uppercase(Locale.ROOT)) }

