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

import android.util.Log
import nl.joozd.logbookapp.data.dataclasses.BalanceForward
import nl.joozd.logbookapp.App
import nl.joozd.logbookapp.R
import nl.joozd.logbookapp.model.dataclasses.Flight
import nl.joozd.logbookapp.model.helpers.FlightDataPresentationFunctions.minutesToHoursAndMinutesString
import nl.joozd.logbookapp.ui.activities.totalTimesActivity.TotalTimesList
import nl.joozd.logbookapp.ui.activities.totalTimesActivity.TotalTimesListItem

/**
 * Displays a list of total times as shown in logbook:
 */
class TotalTimes(flights: List<Flight>, balancesForward: List<BalanceForward>): TotalTimesList {
    private val consolidatedBalancesForward = BalanceForward(-1, "",
        multiPilotTime = balancesForward.sumOf{it.multiPilotTime},
        aircraftTime = balancesForward.sumOf{it.aircraftTime},
        landingDay = balancesForward.sumOf{it.landingDay},
        landingNight = balancesForward.sumOf{it.landingNight},
        nightTime = balancesForward.sumOf{it.nightTime},
        ifrTime = balancesForward.sumOf{it.ifrTime},
        picTime = balancesForward.sumOf{it.picTime},
        copilotTime = balancesForward.sumOf{it.copilotTime},
        dualTime = balancesForward.sumOf{it.dualTime},
        instructortime = balancesForward.sumOf{it.instructortime},
        simTime = balancesForward.sumOf{it.simTime}
    ).also{
        Log.d("bf", "consolidatedBalancesForward: $it")
    }

    /**
     * Title of the list (eg. "Times per aircraft"
     */
    override val title = App.instance.ctx.getString(R.string.totalTimes)

    /**
     * List of [TotalTimesListItem]
     * eg. listOf(TotalTimesListItem("PH-EZA","12:34",754), TotalTimesListItem("PH-EZB","12:35",755))
     */
    override val values: List<TotalTimesListItem> = buildList(flights).also{
        Log.d("Here we go", "rop tjop tjop over de kop kom op 3")
    }

    /**
     * Bit mask of available sorting types
     */
    override val sortableBy = TotalTimesList.ORIGINAL + TotalTimesList.VALUE_DOWN

    /**
     * Set to true if this list should start open in the expandableListView
     */
    override val autoOpen = true

    private fun buildList(flights: List<Flight>): List<TotalTimesListItem>{

        Log.d("Here we go", "rop tjop tjop over de kop kom op 2")
        val categories = App.instance.resources.getStringArray(R.array.total_times_categories)
        val list = emptyList<TotalTimesListItem>().toMutableList()

        flights.sumOf{f -> maxOf(f.duration(), f.simTime)}.let { t -> // total time in minutes
            list.add(
                TotalTimesListItem(
                categories[TOTAL_TIME_INDEX],
                minutesToHoursAndMinutesString(t+consolidatedBalancesForward.grandTotal),
                t+consolidatedBalancesForward.grandTotal, TOTAL_TIME_INDEX)
            )
        }

        flights.sumOf{it.duration()}.let { t ->
            list.add(
                TotalTimesListItem(
                categories[FLIGHT_TIME_INDEX],
                minutesToHoursAndMinutesString(t+consolidatedBalancesForward.aircraftTime),
                t+consolidatedBalancesForward.aircraftTime, FLIGHT_TIME_INDEX)
            )
        }

        flights.filter{it.isSim}.sumOf{it.simTime}.let { t ->
            list.add(
                TotalTimesListItem(
                categories[SIM_TIME_INDEX],
                minutesToHoursAndMinutesString(t+consolidatedBalancesForward.simTime),
                t+consolidatedBalancesForward.simTime, SIM_TIME_INDEX)
            )
        }


            flights.sumOf{it.multiPilotTime}.let { t ->
                list.add(
                    TotalTimesListItem(
                        categories[MULTIPILOT_TIME_INDEX],
                        minutesToHoursAndMinutesString(t+consolidatedBalancesForward.multiPilotTime),
                        t+consolidatedBalancesForward.multiPilotTime, MULTIPILOT_TIME_INDEX)
                )
            }


        flights.sumOf{it.nightTime}.let { t ->
            list.add(
                TotalTimesListItem(
                categories[NIGHT_TIME_INDEX],
                minutesToHoursAndMinutesString(t+consolidatedBalancesForward.nightTime),
                t+consolidatedBalancesForward.nightTime, NIGHT_TIME_INDEX)
            )
        }

        flights.sumOf{it.ifrTime}.let { t ->
            list.add(
                TotalTimesListItem(
                categories[IFR_TIME_INDEX],
                minutesToHoursAndMinutesString(t+consolidatedBalancesForward.ifrTime),
                t+consolidatedBalancesForward.ifrTime, IFR_TIME_INDEX)
            )
        }

        flights.filter{it.isPIC || it.isPICUS}.sumOf{it.duration()}.let { t ->
            list.add(
                TotalTimesListItem(
                categories[PIC_TIME_INDEX],
                minutesToHoursAndMinutesString(t+consolidatedBalancesForward.picTime),
                t+consolidatedBalancesForward.picTime, PIC_TIME_INDEX)
            )
        }

        flights.filter{it.isCoPilot}.sumOf{it.duration()}.let { t ->
            list.add(
                TotalTimesListItem(
                categories[COPILOT_TIME_INDEX],
                minutesToHoursAndMinutesString(t+consolidatedBalancesForward.copilotTime),
                t+consolidatedBalancesForward.copilotTime, COPILOT_TIME_INDEX)
            )
        }

        flights.filter{it.isDual}.sumOf{it.duration()}.let { t ->
            list.add(
                TotalTimesListItem(
                categories[DUAL_TIME_INDEX],
                minutesToHoursAndMinutesString(t+consolidatedBalancesForward.dualTime),
                t+consolidatedBalancesForward.dualTime, DUAL_TIME_INDEX)
            )
        }

        flights.filter{it.isInstructor}.sumOf{it.duration()}.let { t ->
            list.add(
                TotalTimesListItem(
                categories[INSTRUCTOR_TIME_INDEX],
                minutesToHoursAndMinutesString(t+consolidatedBalancesForward.instructortime),
                t+consolidatedBalancesForward.instructortime, INSTRUCTOR_TIME_INDEX)
            )
        }

        flights.sumOf{it.landingDay}.let { t ->
            list.add(
                TotalTimesListItem(
                categories[LANDING_DAY_TIME_INDEX],
                    (t + consolidatedBalancesForward.landingDay).toString(),
                Int.MIN_VALUE + t + consolidatedBalancesForward.landingDay, // to make sure these are always bottom two
                LANDING_DAY_TIME_INDEX)
            )
        }

        flights.sumOf{it.landingNight}.let { t ->
            list.add(
                TotalTimesListItem(
                categories[LANDING_NIGHT_TIME_INDEX],
                    (t + consolidatedBalancesForward.landingNight).toString(),
                    Int.MIN_VALUE + t + consolidatedBalancesForward.landingNight, // to make sure these are always bottom two
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