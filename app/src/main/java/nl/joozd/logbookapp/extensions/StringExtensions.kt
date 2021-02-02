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

package nl.joozd.logbookapp.extensions

import android.util.Log
import java.util.*

/**
 * returns null if String is empty
 */
fun String.nullIfEmpty() = if (this.isEmpty()) null else this

fun String.nullIfBlank() = if (this.isBlank()) null else this

fun String.emptyIfNotTrue(keep: Boolean) = if (keep) this else ""

fun String.nullIfNotTrue(keep: Boolean?) = if (keep == true) this else null

fun String.removeTrailingDigits(): String{
    if (isEmpty()) return this
    var trailingDigits: Int = 0
    while (this.dropLast(trailingDigits).last().isDigit()) trailingDigits++
    //We now know how many digits this EditText's text ends with (spoiler: it's [trailingDigtis]

    return this.dropLast(trailingDigits)
}

fun String.anyWordStartsWith(substring: String, ignoreCase: Boolean = false) =
    if (ignoreCase) this.toUpperCase(Locale.ROOT).split(" ").any{it.startsWith(substring.toUpperCase(Locale.ROOT))}
    else this.split(" ").any{it.startsWith(substring)}

/**
 * Checks if [this] in [other], ignoring case
 */
@Suppress("FunctionName")
infix fun String.in_ignoreCase(other: String): Boolean = this.toUpperCase(Locale.ROOT) in other.toUpperCase(Locale.ROOT)