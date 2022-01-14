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

package nl.joozd.logbookapp.data.miscClasses

/**
 * Balances forward for filling pages in logbook. All values in minutes.
 * ie. 1 hr 17 = 77 mins
 * */
data class TotalsForward (var multiPilot: Int = 0, var totalTime: Int = 0, var landingDay: Int = 0,
                          var landingNight: Int = 0, var nightTime: Int = 0, var ifrTime: Int = 0,
                          var picTime: Int = 0, var copilotTime: Int = 0, var dualTime: Int = 0,
                          var instructorTime: Int = 0, var simTime: Int = 0)