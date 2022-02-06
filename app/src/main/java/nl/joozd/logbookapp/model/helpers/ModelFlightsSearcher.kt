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

import nl.joozd.logbookapp.model.ModelFlight
import nl.joozd.logbookapp.model.viewmodels.activities.mainActivity.MainActivityViewModelNew
import java.util.*


fun List<ModelFlight>?.filterByQuery(query: String, searchType: Int): List<ModelFlight> {
    if (this == null) return emptyList()
    if (query.isBlank()) return this
    return when (searchType) {
        MainActivityViewModelNew.SEARCH_ALL -> searchAll(this, query)
        MainActivityViewModelNew.SEARCH_AIRPORTS -> searchAirport(this, query)
        MainActivityViewModelNew.SEARCH_AIRCRAFT -> searchAircraft(this, query)
        MainActivityViewModelNew.SEARCH_NAMES -> searchNames(this, query)
        MainActivityViewModelNew.SEARCH_FLIGHTNUMBER -> searchFlightnumber(this, query)
        else -> this
    }
}

private fun searchAll(fff: List<ModelFlight>, query: String) = fff.filter {
    query in it.name.uppercase(Locale.ROOT)
            || it.name2.any { n-> query in n.uppercase(Locale.ROOT) }
            || query in it.flightNumber.uppercase(Locale.ROOT)
            || it.aircraft matches query
            || it.orig identMatches query
            || it.dest identMatches query
}

private fun searchNames(fff: List<ModelFlight>, query: String) = fff.filter {
    query in it.name.uppercase(Locale.ROOT)
            || it.name2.any { n-> query in n.uppercase(Locale.ROOT) }
}

private fun searchFlightnumber(fff: List<ModelFlight>, query: String) = fff.filter {
    query in it.flightNumber.uppercase(Locale.ROOT)
}

private fun searchAircraft(fff: List<ModelFlight>, query: String) = fff.filter {
    it.aircraft matchesIncludingType query
}

private fun searchAirport(fff: List<ModelFlight>, query: String) = fff.filter {
    it.orig matches query || it.dest matches query
}
