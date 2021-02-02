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

package nl.joozd.logbookapp.model.helpers

import java.time.Instant
import java.time.LocalDateTime
import java.time.ZoneId
import java.time.format.DateTimeFormatter

/**
 * To be used as part of viewModels that want to use these functions
 */
object FlightDataPresentationFunctions {
    val flightTimeFormatter = DateTimeFormatter.ofPattern("HH:mm") // should always be in Z
    val flightDateFormatter = DateTimeFormatter.ofPattern("dd MMM yyyy")
    /**
     * gets a date string from epoch seconds
     */
    fun getDateStringFromEpochSeconds(epochSeconds: Long): String{
        val dateFormatter = DateTimeFormatter.ofPattern("dd MMM yyyy")
        val time = LocalDateTime.ofInstant(Instant.ofEpochSecond(epochSeconds), ZoneId.of("UTC"))
        return time.format(dateFormatter)
    }

    fun getTimestringFromEpochSeconds(epochSeconds: Long): String{
        val timeFormatter = DateTimeFormatter.ofPattern("HH:mm")
        val time = LocalDateTime.ofInstant(Instant.ofEpochSecond(epochSeconds), ZoneId.of("UTC"))
        return time.format(timeFormatter)
    }

    fun minutesToHoursAndMinutesString(minutes: Int): String = "${minutes/60}:${(minutes%60).toString().padStart(2,'0')}"

    fun minutesToHoursAndMinutesString(minutes: Long) = minutesToHoursAndMinutesString(minutes.toInt())

    /**
     * Make a string like "1:23" into minutes (83 in this case). Also works for "1:23:45.678"
     */






}