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

package nl.joozd.logbookapp.ui.activities.totalTimesActivity

/**
 * Classes implementing this can output a total times list for use in [TotalTimesExpandableListAdapter]
 */
interface TotalTimesList {
    /**
     * Title of the list (eg. "Times per aircraft"
     */
    val title: String

    /**
     * List of [TotalTimesListItem]
     * eg. listOf(TotalTimesListItem("PH-EZA","12:34",754), TotalTimesListItem("PH-EZB","12:35",755))
     */
    val values: List<TotalTimesListItem>

    /**
     * Bit mask of available sorting types
     */
    val sortableBy: Long

    /**
     * Set to true if this list should start open in the expandableListView
     */
    val autoOpen: Boolean


    companion object{
        const val NOT_SORTABLE = 0L
        const val ORIGINAL = 1L
        const val VALUE_DOWN = 2L
        const val VALUE_UP = 4L
        const val NAME_DOWN = 8L
        const val NAME_UP = 16L


    }
}