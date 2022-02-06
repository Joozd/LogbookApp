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

import nl.joozd.logbookapp.App
import nl.joozd.logbookapp.R
import nl.joozd.logbookapp.model.dataclasses.Flight
import nl.joozd.logbookapp.model.helpers.minutesToHoursAndMinutesString
import nl.joozd.logbookapp.ui.activities.totalTimesActivity.TotalTimesList
import nl.joozd.logbookapp.ui.activities.totalTimesActivity.TotalTimesListItem
import java.time.LocalDateTime

class TimesPerYear(flights: List<Flight>): TotalTimesList {
    /**
     * Title of the list (eg. "Times per aircraft"
     */
    override val title = App.instance.ctx.getString(R.string.times_per_year)

    /**
     * List of [TotalTimesListItem]
     * eg. listOf(TotalTimesListItem("PH-EZA","12:34",754), TotalTimesListItem("PH-EZB","12:35",755))
     */
    override val values: List<TotalTimesListItem> = buildList(flights)

    /**
     * Bit mask of available sorting types
     */
    override val sortableBy = TotalTimesList.NAME_DOWN + TotalTimesList.NAME_UP + TotalTimesList.VALUE_DOWN + TotalTimesList.VALUE_UP

    /**
     * Set to true if this list should start open in the expandableListView
     */
    override val autoOpen = false


    private fun buildList(flights: List<Flight>): List<TotalTimesListItem> {
        val yearsToTimes = flights.map { it.tOut().year }.distinct().map { year ->
            year to flights.filter { it.tOut().year == year }.sumOf { it.duration() }
        }.toMap()
        return listOf(flights.filter { it.tOut() >= LocalDateTime.now().minusYears(1) }.sumOf { it.duration() }.let { rollingTime ->
            TotalTimesListItem(App.instance.getString(R.string.last_12_months), rollingTime.minutesToHoursAndMinutesString(), rollingTime, 0)
        }) + yearsToTimes.keys.sorted().map { year ->
            TotalTimesListItem(year.toString(), (yearsToTimes[year] ?: -1).minutesToHoursAndMinutesString(), yearsToTimes[year] ?: -1, 0)
        }
    }

}