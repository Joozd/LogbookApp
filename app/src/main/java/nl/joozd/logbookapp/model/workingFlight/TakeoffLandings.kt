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

package nl.joozd.logbookapp.model.workingFlight

/**
 * Model class to help keep track of takeoff and landing data
 * Can be added to each other
 */
data class TakeoffLandings(val takeoffDay: Int = 0, val takeoffNight: Int = 0, val landingDay: Int = 0, val landingNight: Int = 0, val autoLand: Int = 0) {
    operator fun plus(other: TakeoffLandings) = TakeoffLandings(
        takeoffDay = takeoffDay + other.takeoffDay,
        takeoffNight = takeoffNight + other.takeoffNight,
        landingDay = landingDay + other.landingDay,
        landingNight = landingNight + other.landingNight,
        autoLand = autoLand + other.autoLand
    )

    operator fun minus(other: TakeoffLandings) = TakeoffLandings(
        takeoffDay = takeoffDay - other.takeoffDay,
        takeoffNight = takeoffNight - other.takeoffNight,
        landingDay = landingDay - other.landingDay,
        landingNight = landingNight - other.landingNight,
        autoLand = autoLand - other.autoLand
    )
}