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
import nl.joozd.logbookapp.data.repository.aircraftrepository.AircraftRepository
import nl.joozd.logbookapp.model.dataclasses.Flight
import nl.joozd.logbookapp.model.helpers.minutesToHoursAndMinutesString
import nl.joozd.logbookapp.model.viewmodels.activities.totalTimesActivity.TotalTimesItem
import nl.joozd.logbookapp.model.viewmodels.activities.totalTimesActivity.sortingStrategy.SortNameDownStrategy
import nl.joozd.logbookapp.model.viewmodels.activities.totalTimesActivity.sortingStrategy.SortNameUpStrategy
import nl.joozd.logbookapp.model.viewmodels.activities.totalTimesActivity.sortingStrategy.SortValueDownStrategy
import nl.joozd.logbookapp.model.viewmodels.activities.totalTimesActivity.sortingStrategy.SortValueUpStrategy
import nl.joozd.logbookapp.ui.activities.totalTimesActivity.TotalTimesListItem
import nl.joozd.logbookapp.utils.DispatcherProvider

class TimesPerType(title: String, items: List<TotalTimesListItem>
): TotalTimesItem(title, items, sortableBy) {

    companion object{
        suspend fun of(flights: List<Flight>) = withContext(DispatcherProvider.default()){
            val title = App.instance.ctx.getString(R.string.times_per_type)
            val items = buildList(flights)
            TimesPerType(title, items)
        }


        private suspend fun buildList(flights: List<Flight>): List<TotalTimesListItem> {
            val adc = AircraftRepository.instance.getAircraftDataCache()
            val typesToTimes = flights.filter{!it.isSim}.map { it.aircraftType }.distinct().map { type ->
                type to flights.filter { it.aircraftType == type }.sumOf { it.duration() }
            }.toMap()
            return typesToTimes.keys.sorted().map { type ->
                val typeLongName: String = adc.getAircraftTypeByShortName(type)?.name ?: type
                TotalTimesListItem(typeLongName, (typesToTimes[type] ?: -1).minutesToHoursAndMinutesString(), typesToTimes[type] ?: -1)
            }
        }

        private val sortableBy = listOf(
            SortNameUpStrategy(R.string.sorter_type_up),
            SortNameDownStrategy(R.string.sorter_type_down),
            SortValueDownStrategy(R.string.sorter_time_down),
            SortValueUpStrategy(R.string.sorter_time_up)
        )
    }
}