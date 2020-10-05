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

package nl.joozd.logbookapp.data.repository.helpers

import java.util.*

fun String.findBestHitForRegistration(registrations: Collection<String>): String? =
    if (this.isBlank()) null else
    findSortedHitsForRegistration(registrations).firstOrNull()

    //we'll want to search from end to start. As soon as wel have one, we're good.
    /*
    do {
        searchableRegs.firstOrNull { it.endsWith(this) }?.let {return it}
        if (searchableRegs.none { this in it }) return null
        searchableRegs = searchableRegs.map { it.dropLast(1) }.filter { it.isNotEmpty() }
    } while (searchableRegs.none{it.endsWith(this)})
     */



/**
 * Searches aircraft registrations for hits, sorted by usefulness:
 * - Exact match ("PH-EZA" -> "PH-EZA")
 * - exact match without '-' ( "PHEZA -> "PH-EZA")
 * - Last matches("ZA" -> PH-EZA")
 * - Part after hyphen matches ( "EZ" -> PH-EZA")
 * - First part matches("PH" -> "PH-EZA")
 * - Any part matches ("H-E" -> "PH-EZA")
 */
fun String.findSortedHitsForRegistration(registrations: Collection<String>, caseSensitive: Boolean = false): List<String>{
    val query = if (caseSensitive) this else this.toUpperCase(Locale.ROOT)
    val searchableRegs = registrations.filter{it.length > length}.filter{query in it}.let { rrr->
        if (caseSensitive) rrr else rrr.map { it.toUpperCase(Locale.ROOT) }
    }
        return  (searchableRegs.filter {it == query} +
                searchableRegs.filter{it.filter {c -> c != '-'} == query} +
                searchableRegs.filter{it.endsWith((query))} +
                searchableRegs.filter{'-' in it}.map {it.split('-')}.filter{it[1].startsWith(query)}.map{it.joinToString("-")} +
                searchableRegs.filter{it.startsWith((query))} +
                searchableRegs).distinct()
}