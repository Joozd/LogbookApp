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

package nl.joozd.logbookapp.ui.activities.totalTimesActivity.listBuilders

import nl.joozd.logbookapp.App
import nl.joozd.logbookapp.R
import nl.joozd.logbookapp.model.dataclasses.Flight
import nl.joozd.logbookapp.model.helpers.FlightDataPresentationFunctions.minutesToHoursAndMinutesString
import nl.joozd.logbookapp.ui.adapters.totaltimesadapter.TotalTimesList
import nl.joozd.logbookapp.ui.adapters.totaltimesadapter.TotalTimesListItem

/**
 * Displays a list of total times as shown in logbook:
 */
class TotalTimes(flights: List<Flight>): TotalTimesList {
    /**
     * Title of the list (eg. "Times per aircraft"
     */
    //TODO make this a String resource
    override val title = "Total Times"

    /**
     * List of [TotalTimesListItem]
     * eg. listOf(TotalTimesListItem("PH-EZA","12:34",754), TotalTimesListItem("PH-EZB","12:35",755))
     */
    override val values: List<TotalTimesListItem> = buildList(flights)

    private fun buildList(flights: List<Flight>): List<TotalTimesListItem>{
        val categories = App.instance.resources.getStringArray(R.array.total_times_categories)
        val list = emptyList<TotalTimesListItem>().toMutableList()

        flights.sumBy{f -> maxOf(f.duration(), f.simTime)}.let { t -> // total time in minutes
            list.add(TotalTimesListItem(
                categories[TOTAL_TIME_INDEX],
                minutesToHoursAndMinutesString(t),
                t, TOTAL_TIME_INDEX)
            )
        }

        flights.sumBy{it.duration()}.let { t ->
            list.add(TotalTimesListItem(
                categories[FLIGHT_TIME_INDEX],
                minutesToHoursAndMinutesString(t),
                t, FLIGHT_TIME_INDEX)
            )
        }

        flights.sumBy{it.simTime}.let { t ->
            list.add(TotalTimesListItem(
                categories[SIM_TIME_INDEX],
                minutesToHoursAndMinutesString(t),
                t, SIM_TIME_INDEX)
            )
        }

        /**
         * Multipilot needs a bit more work. Maybe put that in crew for faster processing?
         */

 /*       list.add(TotalTimesListItem(
            categories[MULTIPILOT_TIME_INDEX],
            "---",
            0)
  */


        flights.sumBy{it.nightTime}.let { t ->
            list.add(TotalTimesListItem(
                categories[NIGHT_TIME_INDEX],
                minutesToHoursAndMinutesString(t),
                t, NIGHT_TIME_INDEX)
            )
        }

        flights.sumBy{it.ifrTime}.let { t ->
            list.add(TotalTimesListItem(
                categories[IFR_TIME_INDEX],
                minutesToHoursAndMinutesString(t),
                t, IFR_TIME_INDEX)
            )
        }

        flights.filter{it.isPIC || it.isPICUS}.sumBy{it.duration()}.let { t ->
            list.add(TotalTimesListItem(
                categories[PIC_TIME_INDEX],
                minutesToHoursAndMinutesString(t),
                t, PIC_TIME_INDEX)
            )
        }

        flights.filter{it.isCoPilot || it.isPICUS}.sumBy{it.duration()}.let { t ->
            list.add(TotalTimesListItem(
                categories[COPILOT_TIME_INDEX],
                minutesToHoursAndMinutesString(t),
                t, COPILOT_TIME_INDEX)
            )
        }

        flights.filter{it.isDual || it.isPICUS}.sumBy{it.duration()}.let { t ->
            list.add(TotalTimesListItem(
                categories[DUAL_TIME_INDEX],
                minutesToHoursAndMinutesString(t),
                t, DUAL_TIME_INDEX)
            )
        }

        flights.filter{it.isInstructor || it.isPICUS}.sumBy{it.duration()}.let { t ->
            list.add(TotalTimesListItem(
                categories[INSTRUCTOR_TIME_INDEX],
                minutesToHoursAndMinutesString(t),
                t, INSTRUCTOR_TIME_INDEX)
            )
        }

        flights.sumBy{it.landingDay}.let { t ->
            list.add(TotalTimesListItem(
                categories[LANDING_DAY_TIME_INDEX],
                minutesToHoursAndMinutesString(t),
                Int.MAX_VALUE -2, // to make sure these are always bottom two
                LANDING_DAY_TIME_INDEX)
            )
        }

        flights.sumBy{it.landingNight}.let { t ->
            list.add(TotalTimesListItem(
                categories[LANDING_NIGHT_TIME_INDEX],
                minutesToHoursAndMinutesString(t),
                Int.MAX_VALUE -1, // to make sure these are always bottom two
                LANDING_NIGHT_TIME_INDEX)
            )
        }


        return list
    }

    companion object{
        const val TOTAL_TIME_INDEX = 0
        const val FLIGHT_TIME_INDEX = 1
        const val SIM_TIME_INDEX = 2
        const val MULTIPILOT_TIME_INDEX = 3
        const val NIGHT_TIME_INDEX = 4
        const val IFR_TIME_INDEX = 5
        const val PIC_TIME_INDEX = 6
        const val COPILOT_TIME_INDEX = 7
        const val DUAL_TIME_INDEX = 8
        const val INSTRUCTOR_TIME_INDEX = 9
        const val LANDING_DAY_TIME_INDEX = 10
        const val LANDING_NIGHT_TIME_INDEX = 11
    }


}