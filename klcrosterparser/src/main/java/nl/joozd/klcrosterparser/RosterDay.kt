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

package nl.joozd.klcrosterparser

import java.time.LocalDate
import java.time.LocalDateTime
import java.time.LocalTime
import java.time.ZoneOffset


data class RosterDay(val date: LocalDate, val events: List<KlcRosterEvent>, val extraInfo: String) {
    val startOfDay = LocalDateTime.of(date, LocalTime.MIDNIGHT).atZone(ZoneOffset.UTC).toInstant()
    val startOfDayEpochSecond= startOfDay.epochSecond

    val endOfDay = LocalDateTime.of(date.plusDays(1), LocalTime.MIDNIGHT).atZone(ZoneOffset.UTC).toInstant()
    val endOfDayEpochSecond = endOfDay.epochSecond

    override fun toString() = "Date: $date, events: $events, extraInfo: $extraInfo"
}