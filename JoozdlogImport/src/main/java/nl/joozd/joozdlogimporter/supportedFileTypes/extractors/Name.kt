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

package nl.joozd.joozdlogimporter.supportedFileTypes.extractors

import java.util.*

/**
 * Helper class for names.
 */
data class Name(val first: String = "", val last: String = "", val middle: String = "") {
    val checkMyName = (if (middle.isNotEmpty()) "$first $last, $middle" else "$first $last")
        .uppercase(Locale.ROOT)

    override fun toString() = listOf(first, middle.lowercase(Locale.ROOT), last).filter { it.isNotBlank() }.joinToString(" ")

    companion object {
        /**
         * A complete name is 2 or 3 names long (Jan-Henk Nicolaas, van de, Wilde Wetering)
         * If 2 names, all are words are Capitalized, else only first and last
         */
        fun ofList(names: List<String>): Name {
            return when (names.size) {
                0 -> Name()
                1 -> Name(names.first().withCapital())
                2 -> Name(capitalizeAllWords(names[1]), capitalizeAllWords(names[0]))
                else -> // 3 or more, ignore any words past [2]
                    Name(capitalizeAllWords(names[2]), capitalizeAllWords(names[0]), names[1].lowercase(
                        Locale.ROOT))
            }
        }

        private fun capitalizeAllWords(line: String): String = line.split(' ')
            .filter { !it.isBlank() } // remove any extra spaces
            .joinToString(" ") {
                it.split('-')
                    .joinToString("-") { it.withCapital() }
            }.trim()

        private fun String.withCapital(): String = lowercase(Locale.ROOT).replaceFirstChar { if (it.isLowerCase()) it.titlecase(
            Locale.ROOT) else it.toString() }
    }
}