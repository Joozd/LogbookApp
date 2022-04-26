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

package nl.joozd.logbookapp.model.viewmodels.activities.totalTimesActivity.listBuilders

import kotlinx.coroutines.withContext
import nl.joozd.logbookapp.core.App
import nl.joozd.logbookapp.R
import nl.joozd.logbookapp.model.dataclasses.Flight
import nl.joozd.logbookapp.model.viewmodels.activities.totalTimesActivity.TotalTimesItem
import nl.joozd.logbookapp.model.viewmodels.activities.totalTimesActivity.sortingStrategy.SortNameDownStrategy
import nl.joozd.logbookapp.model.viewmodels.activities.totalTimesActivity.sortingStrategy.SortNameUpStrategy
import nl.joozd.logbookapp.model.viewmodels.activities.totalTimesActivity.sortingStrategy.SortValueDownStrategy
import nl.joozd.logbookapp.model.viewmodels.activities.totalTimesActivity.sortingStrategy.SortValueUpStrategy
import nl.joozd.logbookapp.ui.activities.totalTimesActivity.TotalTimesListItem
import nl.joozd.logbookapp.utils.DispatcherProvider

class AirportsArrived(title: String, items: List<TotalTimesListItem>
): TotalTimesItem(title,items, sortableBy) {
    companion object{
        //This runs suspended as it is nice and heavy work for longer lists.
        suspend fun of(flights: List<Flight>) = withContext(DispatcherProvider.default()){
            val title = App.instance.ctx.getString(R.string.arrivals_per_airport)
            val list = buildList(flights)
            AirportsArrived(title, list)
        }

        private fun buildList(flights: List<Flight>): List<TotalTimesListItem> {
            val destsToVisits =
                flights.filter{!it.isSim}
                    .map { it.dest }
                    .distinct()
                    .map { dest ->
                        dest to flights.filter { it.dest == dest }.size
                    }
                    .toMap()
            return destsToVisits.keys.sorted().map { icaoName ->
                TotalTimesListItem(icaoName, (destsToVisits[icaoName] ?: -1).toString(), destsToVisits[icaoName] ?: -1)
            }
        }

        private val sortableBy = listOf(SortValueDownStrategy, SortNameDownStrategy, SortNameUpStrategy, SortValueUpStrategy)
    }
}