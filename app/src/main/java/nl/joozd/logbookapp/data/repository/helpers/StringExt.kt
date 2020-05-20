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

fun String.findBestHitForRegistration(registrations: List<String>): String?{
    if (this in registrations) return this
    var searchableRegs = registrations.filter{it.length > length}.filter{this in it}

    //we'll want to search from end to start. As soon as wel have one, we're good.
    do {
        searchableRegs.firstOrNull { it.endsWith(this) }?.let {return it}
        if (searchableRegs.none { this in it }) return null
        searchableRegs = searchableRegs.map { it.dropLast(1) }.filter { it.isNotEmpty() }
    } while (searchableRegs.none{it.endsWith(this)})
    return null
}