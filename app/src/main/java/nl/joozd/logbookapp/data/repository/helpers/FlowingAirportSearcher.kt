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

import kotlinx.coroutines.flow.flow
import nl.joozd.logbookapp.data.dataclasses.Airport
import java.util.*

object FlowingAirportSearcher{
    /**
     * Makes a Flow that emits lists of matching Airports to query in order:
     * - Iata_code
     * - Ident
     * - City
     * - Airport name
     * @param fullList: Complete list of Airports to search
     * @param query: query to search by
     * @return Flow<List<Airport>> with found airports untill search is finished
     */
    fun makeFlow(fullList: List<Airport>,  query: String?) = flow {
        if (query == null) {
            emit(fullList)
        } else {
            val upperQuery = query.uppercase(Locale.ROOT)
            val result = mutableListOf<Airport>()
            // pass1
            fullList.forEach {
                if (upperQuery in it.iata_code.uppercase(Locale.ROOT)) result += it
                emit(result)
            }
            //pass2
            fullList.forEach {
                if (upperQuery in it.ident.uppercase(Locale.ROOT) && it !in result) result += it
                emit(result)
            }
            //pass3
            fullList.forEach {
                if (upperQuery in it.municipality.uppercase(Locale.ROOT) && it !in result) result += it
                emit(result)
            }
            //pass4
            fullList.forEach {
                if (upperQuery in it.name.uppercase(Locale.ROOT) && it !in result) result += it
                emit(result)
            }
        }
    }
}