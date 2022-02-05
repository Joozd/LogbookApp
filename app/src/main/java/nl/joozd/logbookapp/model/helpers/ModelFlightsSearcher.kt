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

class ModelFlightsSearcher {
    fun searchFlights(flights: List<ModelFlight>?, query: String, searchType: Int): List<ModelFlight> {
        if (flights == null) return emptyList()
        if (query.isBlank()) return flights
        return when (searchType) {
            MainActivityViewModelNew.SEARCH_ALL -> searchAll(flights, query)
            MainActivityViewModelNew.SEARCH_AIRPORTS -> searchAirport(flights, query)
            MainActivityViewModelNew.SEARCH_AIRCRAFT -> searchAircraft(flights, query)
            MainActivityViewModelNew.SEARCH_NAMES -> searchNames(flights, query)
            MainActivityViewModelNew.SEARCH_FLIGHTNUMBER -> searchFlightnumber(flights, query)
            else -> flights
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
}