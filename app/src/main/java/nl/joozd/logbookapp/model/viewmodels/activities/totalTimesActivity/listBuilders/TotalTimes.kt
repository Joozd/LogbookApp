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
import kotlinx.coroutines.withContext
import nl.joozd.logbookapp.data.dataclasses.BalanceForward
import nl.joozd.logbookapp.core.App
import nl.joozd.logbookapp.R
import nl.joozd.logbookapp.model.dataclasses.Flight
import nl.joozd.logbookapp.model.helpers.minutesToHoursAndMinutesString
import nl.joozd.logbookapp.model.viewmodels.activities.totalTimesActivity.TotalTimesItem
import nl.joozd.logbookapp.model.viewmodels.activities.totalTimesActivity.sortingStrategy.*
import nl.joozd.logbookapp.ui.activities.totalTimesActivity.TotalTimesListItem
import nl.joozd.logbookapp.utils.DispatcherProvider

/**
 * Displays a list of total times as shown in logbook:
 */
class TotalTimes(title: String, items: List<TotalTimesListItem>
): TotalTimesItem(title, items, sortableBy, isOpen = true) {


    companion object{
        suspend fun of(
            flights: List<Flight>,
            balancesForward: List<BalanceForward>
        ) = withContext(DispatcherProvider.default()){
            val title = App.instance.ctx.getString(R.string.totalTimes)
            val items = buildList(flights, balancesForward)
            TotalTimes(title, items)
        }


        private fun getConsolidatedBalancesForward(balancesForward: List<BalanceForward>) = BalanceForward(-1, "",
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
        )

        private fun buildList(flights: List<Flight>, balancesForward: List<BalanceForward>): List<TotalTimesListItem>{
            val consolidatedBalancesForward = getConsolidatedBalancesForward(balancesForward)
            Log.d("Here we go", "rop tjop tjop over de kop kom op 2")
            val categories = App.instance.resources.getStringArray(R.array.total_times_categories)
            return listOf(
                makeTotalTimesItem(flights, categories, consolidatedBalancesForward),
                makeFlightTimesItem(flights, categories, consolidatedBalancesForward),
                makeSimTimeItem(flights, categories, consolidatedBalancesForward),
                makeMultiPilotTimeItem(flights, categories, consolidatedBalancesForward),
                makeNightTimeItem(flights, categories, consolidatedBalancesForward),
                makeIfrTimeItem(flights, categories, consolidatedBalancesForward),
                makePicTimeItem(flights, categories, consolidatedBalancesForward),
                makeCoPilotTimeItem(flights, categories, consolidatedBalancesForward),
                makeDualTimeItem(flights, categories, consolidatedBalancesForward),
                makeInstructorTimeItem(flights, categories, consolidatedBalancesForward),
                makeLandingDayItem(flights, categories, consolidatedBalancesForward),
                makeLandingNightItem(flights, categories, consolidatedBalancesForward)
            )
        }

        private fun makeLandingNightItem(
            flights: List<Flight>,
            categories: Array<String>,
            consolidatedBalancesForward: BalanceForward
        ) =
            flights.sumOf { it.landingNight }.let { t ->
                TotalTimesListItem(
                    categories[LANDING_NIGHT_TIME_INDEX],
                    (t + consolidatedBalancesForward.landingNight).toString(),
                    Int.MIN_VALUE + t + consolidatedBalancesForward.landingNight // to make sure these are always bottom two
                )
            }


        private fun makeLandingDayItem(
            flights: List<Flight>,
            categories: Array<String>,
            consolidatedBalancesForward: BalanceForward
        ) =
            flights.sumOf { it.landingDay }.let { t ->
                TotalTimesListItem(
                    categories[LANDING_DAY_TIME_INDEX],
                    (t + consolidatedBalancesForward.landingDay).toString(),
                    Int.MIN_VALUE + t + consolidatedBalancesForward.landingDay // to make sure these are always bottom two
                )
            }


        private fun makeInstructorTimeItem(
            flights: List<Flight>,
            categories: Array<String>,
            consolidatedBalancesForward: BalanceForward
        ) =
            flights.filter { it.isInstructor }.sumOf { it.duration() }.let { t ->
                TotalTimesListItem(
                    categories[INSTRUCTOR_TIME_INDEX],
                    (t + consolidatedBalancesForward.instructortime).minutesToHoursAndMinutesString(),
                    t + consolidatedBalancesForward.instructortime
                )
            }


        private fun makeDualTimeItem(
            flights: List<Flight>,
            categories: Array<String>,
            consolidatedBalancesForward: BalanceForward
        ) =
            flights.filter { it.isDual }.sumOf { it.duration() }.let { t ->
                TotalTimesListItem(
                    categories[DUAL_TIME_INDEX],
                    (t + consolidatedBalancesForward.dualTime).minutesToHoursAndMinutesString(),
                    t + consolidatedBalancesForward.dualTime
                )
            }


        private fun makeCoPilotTimeItem(
            flights: List<Flight>,
            categories: Array<String>,
            consolidatedBalancesForward: BalanceForward
        ) =
            flights.filter { it.isCoPilot }.sumOf { it.duration() }.let { t ->
                TotalTimesListItem(
                    categories[COPILOT_TIME_INDEX],
                    (t + consolidatedBalancesForward.copilotTime).minutesToHoursAndMinutesString(),
                    t + consolidatedBalancesForward.copilotTime
                )
            }


        private fun makePicTimeItem(
            flights: List<Flight>,
            categories: Array<String>,
            consolidatedBalancesForward: BalanceForward
        ) =
            flights.filter { it.isPIC || it.isPICUS }.sumOf { it.duration() }.let { t ->
                TotalTimesListItem(
                    categories[PIC_TIME_INDEX],
                    (t + consolidatedBalancesForward.picTime).minutesToHoursAndMinutesString(),
                    t + consolidatedBalancesForward.picTime
                )
            }


        private fun makeIfrTimeItem(
            flights: List<Flight>,
            categories: Array<String>,
            consolidatedBalancesForward: BalanceForward
        ) =
            flights.sumOf { it.ifrTime }.let { t ->
                TotalTimesListItem(
                    categories[IFR_TIME_INDEX],
                    (t + consolidatedBalancesForward.ifrTime).minutesToHoursAndMinutesString(),
                    t + consolidatedBalancesForward.ifrTime
                )
            }


        private fun makeNightTimeItem(
            flights: List<Flight>,
            categories: Array<String>,
            consolidatedBalancesForward: BalanceForward
        ) =
            flights.sumOf { it.nightTime }.let { t ->
                TotalTimesListItem(
                    categories[NIGHT_TIME_INDEX],
                    (t + consolidatedBalancesForward.nightTime).minutesToHoursAndMinutesString(),
                    t + consolidatedBalancesForward.nightTime
                )
            }


        private fun makeMultiPilotTimeItem(
            flights: List<Flight>,
            categories: Array<String>,
            consolidatedBalancesForward: BalanceForward
        ) =
            flights.sumOf { it.multiPilotTime }.let { t ->
                TotalTimesListItem(
                    categories[MULTIPILOT_TIME_INDEX],
                    (t + consolidatedBalancesForward.multiPilotTime).minutesToHoursAndMinutesString(),
                    t + consolidatedBalancesForward.multiPilotTime
                )
            }


        private fun makeSimTimeItem(
            flights: List<Flight>,
            categories: Array<String>,
            consolidatedBalancesForward: BalanceForward
        ) =
            flights.filter { it.isSim }.sumOf { it.simTime }.let { t ->
                TotalTimesListItem(
                    categories[SIM_TIME_INDEX],
                    (t + consolidatedBalancesForward.simTime).minutesToHoursAndMinutesString(),
                    t + consolidatedBalancesForward.simTime
                )
            }


        private fun makeFlightTimesItem(
            flights: List<Flight>,
            categories: Array<String>,
            consolidatedBalancesForward: BalanceForward
        ) =
            flights.sumOf { it.duration() }.let { t ->
                TotalTimesListItem(
                    categories[FLIGHT_TIME_INDEX],
                    (t + consolidatedBalancesForward.aircraftTime).minutesToHoursAndMinutesString(),
                    t + consolidatedBalancesForward.aircraftTime
                )
            }


        private fun makeTotalTimesItem(
            flights: List<Flight>,
            categories: Array<String>,
            consolidatedBalancesForward: BalanceForward
        ) =
            flights.sumOf { f -> maxOf(f.duration(), f.simTime) }.let { t ->
                TotalTimesListItem(
                    categories[TOTAL_TIME_INDEX],
                    (t + consolidatedBalancesForward.grandTotal).minutesToHoursAndMinutesString(),
                    t + consolidatedBalancesForward.grandTotal
                )
            }

        private val sortableBy = listOf(
            UnsortedStrategy(),
            SortValueDownStrategy(R.string.sorter_time_down),
            SortValueUpStrategy(R.string.sorter_time_up)
        )



        private const val TOTAL_TIME_INDEX = 0
        private const val FLIGHT_TIME_INDEX = 1
        private const val SIM_TIME_INDEX = 2
        private const val MULTIPILOT_TIME_INDEX = 3
        private const val NIGHT_TIME_INDEX = 4
        private const val IFR_TIME_INDEX = 5
        private const val PIC_TIME_INDEX = 6
        private const val COPILOT_TIME_INDEX = 7
        private const val DUAL_TIME_INDEX = 8
        private const val INSTRUCTOR_TIME_INDEX = 9
        private const val LANDING_DAY_TIME_INDEX = 10
        private const val LANDING_NIGHT_TIME_INDEX = 11
    }


}