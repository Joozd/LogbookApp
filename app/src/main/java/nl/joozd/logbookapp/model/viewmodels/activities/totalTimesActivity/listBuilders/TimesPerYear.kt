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
import nl.joozd.logbookapp.model.helpers.minutesToHoursAndMinutesString
import nl.joozd.logbookapp.model.viewmodels.activities.totalTimesActivity.TotalTimesItem
import nl.joozd.logbookapp.model.viewmodels.activities.totalTimesActivity.sortingStrategy.SortNameDownStrategy
import nl.joozd.logbookapp.model.viewmodels.activities.totalTimesActivity.sortingStrategy.SortNameUpStrategy
import nl.joozd.logbookapp.model.viewmodels.activities.totalTimesActivity.sortingStrategy.SortValueDownStrategy
import nl.joozd.logbookapp.model.viewmodels.activities.totalTimesActivity.sortingStrategy.SortValueUpStrategy
import nl.joozd.logbookapp.ui.activities.totalTimesActivity.TotalTimesListItem
import nl.joozd.logbookapp.utils.DispatcherProvider
import java.time.LocalDateTime

class TimesPerYear(title: String, items: List<TotalTimesListItem>
): TotalTimesItem(title, items, sortableBy) {
    companion object{
        suspend fun of(flights: List<Flight>) = withContext(DispatcherProvider.default()){
            val title = App.instance.ctx.getString(R.string.times_per_year)
            val items = buildList(flights)
            TimesPerYear(title, items)
        }


        private fun buildList(flights: List<Flight>): List<TotalTimesListItem> {
            val yearsToTimesMap = flights.map { it.tOut().year }
                .distinct().associateWith { year ->
                    flights.filter { it.tOut().year == year }.sumOf { it.duration() }
                }
            return listOf(flights.filter { it.tOut() >= LocalDateTime.now().minusYears(1) }.sumOf { it.duration() }.let { rollingTime ->
                TotalTimesListItem(App.instance.getString(R.string.last_12_months), rollingTime.minutesToHoursAndMinutesString(), rollingTime)
            }) + yearsToTimesMap.keys.sorted().map { year ->
                TotalTimesListItem(year.toString(), (yearsToTimesMap[year] ?: -1).minutesToHoursAndMinutesString(), yearsToTimesMap[year] ?: -1)
            }
        }

        private val sortableBy = listOf(
            SortNameDownStrategy(R.string.sorter_year_down),
            SortNameUpStrategy(R.string.sorter_year_up),
            SortValueDownStrategy(R.string.sorter_time_down),
            SortValueUpStrategy(R.string.sorter_time_up)
        )
    }
}