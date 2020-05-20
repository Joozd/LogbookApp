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

package nl.joozd.logbookapp.data.utils

import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import nl.joozd.logbookapp.extensions.nullIfEmpty
import nl.joozd.logbookapp.model.dataclasses.Flight

object FlightsListFunctions {
    fun makeListOfNamesOld(flights: List<Flight>): List<String> =
        flights.asSequence().map{listOfNotNull(it.name.nullIfEmpty(), it.name2).joinToString(",").split(",")}
            .flatten()
            .map{it.trim().nullIfEmpty()}
            .filterNotNull()
            .distinct().toList()

    /*
    suspend fun makeListOfNamesAsync (flights: List<Flight>) = withContext(Dispatchers.Default){
        flights.asSequence().map{listOfNotNull(it.name.nullIfEmpty(), it.name2).joinToString(",").split(",")}
            .flatten()
            .map{it.trim().nullIfEmpty()}
            .filterNotNull()
            .distinct().toList()
    }
    */

    suspend fun makeListOfNamesAsync (flights: List<Flight>) = withContext(Dispatchers.Default){
        flights.asSequence().map{listOfNotNull(it.name.nullIfEmpty(), it.name2).joinToString("|").split("|")}
            .flatten()
            .map{it.trim().nullIfEmpty()}
            .filterNotNull()
            .distinct().toList()
    }
}