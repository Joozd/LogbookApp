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

package nl.joozd.logbookapp.extensions

import java.time.*

/**
 * Gets the date from an Instant. If no [zoneOffset] given, it assumes UTC.
 */
fun Instant.toLocalDate(zoneOffset: ZoneOffset = ZoneOffset.UTC) = LocalDateTime.ofInstant(this, zoneOffset).toLocalDate()

/**
 * Gets the time from an Instant. If no [zoneOffset] given, it assumes UTC.
 */
fun Instant.toLocalTime(zoneOffset: ZoneOffset = ZoneOffset.UTC) = LocalDateTime.ofInstant(this, zoneOffset).toLocalTime()

/**
 * Changes an instant to the same time at a different date. If no [zoneOffset] given, assumes UTC
 * @param date: New date to set
 * @param zoneOffset: Timezone at which that dat eis supposed to be
 */
fun Instant.atDate(date: LocalDate, zoneOffset: ZoneOffset = ZoneOffset.UTC) = LocalDateTime.ofInstant(this, zoneOffset).atDate(date).toInstant(zoneOffset)

fun Instant.plusDays(daysToAdd: Int) = this.plusSeconds(daysToAdd * 86400L) // 86400 seconds is 1 day

operator fun Instant.minus(other: Instant) = Duration.between(this, other)