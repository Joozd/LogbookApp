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

/**
 * Gets the first hit in [findSortedHitsForRegistration]
 * Might make own implementation for slightly faster performance (it currently builds complete list but only needs first entry)
 */
fun findBestHitForRegistration(query: String, registrations: Collection<String>): String? =
    if (query.isBlank()) null else
    findSortedHitsForRegistration(query, registrations).firstOrNull()


/**
 * Searches aircraft registrations for hits, sorted by usefulness:
 * - Exact match ("PH-EZA" -> "PH-EZA")
 * - exact match without '-' ( "PHEZA -> "PH-EZA")
 * - Last matches("ZA" -> PH-EZA")
 * - Part after hyphen matches ( "EZ" -> PH-EZA")
 * - First part matches("PH" -> "PH-EZA")
 * - First part matches ignoring '-' ("PHE"-> "PH-EZA"
 * - Any part matches ("H-E" -> "PH-EZA")
 * - Partial match ignoring '-' ("HEZ -> PH-EZA")
 * @return List of registrations
 */
fun findSortedHitsForRegistration(query: String, registrations: Collection<String>, caseSensitive: Boolean = false): List<String>{
    val q = if (caseSensitive) query else query.uppercase(Locale.ROOT)
    val searchableRegs = registrations.filter{it.length > q.length}.filter{reg -> q.filter{ c -> c != '-'} in reg.filter{ c -> c != '-'} }.let { rrr->
        if (caseSensitive) rrr else rrr.map { it.uppercase(Locale.ROOT) }
    }
        return  (searchableRegs.filter {it == q} +
                searchableRegs.filter{it.filter {c -> c != '-'} == q} +
                searchableRegs.filter{it.endsWith((q))} +
                searchableRegs.filter{'-' in it}.map {it.split('-')}.filter{it[1].startsWith(q)}.map{it.joinToString("-")} +
                searchableRegs.filter{it.startsWith(q)} +
                searchableRegs.filter{it.filter{c -> c != '-'}.startsWith(q)} +
                searchableRegs.filter{q in it} +
                searchableRegs.filter{q in it.filter{ c -> c != '-'} }
                ).distinct()
}