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
import nl.joozd.logbookapp.data.repository.AircraftRepository
import nl.joozd.logbookapp.model.dataclasses.Flight
import nl.joozd.logbookapp.model.helpers.FlightDataPresentationFunctions
import nl.joozd.logbookapp.ui.activities.totalTimesActivity.TotalTimesList
import nl.joozd.logbookapp.ui.activities.totalTimesActivity.TotalTimesListItem

class TimesPerType(flights: List<Flight>): TotalTimesList {
    /**
     * Title of the list (eg. "Times per aircraft"
     */
    override val title = App.instance.ctx.getString(R.string.times_per_type)

    /**
     * List of [TotalTimesListItem]
     * eg. listOf(TotalTimesListItem("PH-EZA","12:34",754), TotalTimesListItem("PH-EZB","12:35",755))
     */
    override val values: List<TotalTimesListItem> by lazy { buildList(flights) }

    /**
     * Bit mask of available sorting types
     */
    override val sortableBy = TotalTimesList.NAME_DOWN + TotalTimesList.NAME_UP + TotalTimesList.VALUE_DOWN + TotalTimesList.VALUE_UP

    /**
     * Set to true if this list should start open in the expandableListView
     */
    override val autoOpen = false

    private fun buildList(flights: List<Flight>): List<TotalTimesListItem> {
        val typesToTimes = flights.filter{!it.isSim}.map { it.aircraftType }.distinct().map { type ->
            type to flights.filter { it.aircraftType == type }.sumOf { it.duration() }
        }.toMap()
        return typesToTimes.keys.sorted().map { type ->
            val typeLongName: String = AircraftRepository.getInstance().getAircraftTypeByShortName(type)?.name ?: type // if AircraftRepo isn't initialized yet it will show short names else full names
            TotalTimesListItem(typeLongName, FlightDataPresentationFunctions.minutesToHoursAndMinutesString(typesToTimes[type] ?: -1), typesToTimes[type] ?: -1, 0)
        }
    }
}