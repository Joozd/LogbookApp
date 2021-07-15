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
import java.time.format.DateTimeFormatter

/**
 * Gets the date from an Instant. If no [zoneOffset] given, it assumes UTC.
 */
fun Instant.toLocalDate(zoneOffset: ZoneOffset = ZoneOffset.UTC): LocalDate = LocalDateTime.ofInstant(this, zoneOffset).toLocalDate()

/**
 * Gets the time from an Instant. If no [zoneOffset] given, it assumes UTC.
 */
fun Instant.toLocalTime(zoneOffset: ZoneOffset = ZoneOffset.UTC): LocalTime = LocalDateTime.ofInstant(this, zoneOffset).toLocalTime()

/**
 * Changes an instant to the same time at a different date. If no [zoneOffset] given, assumes UTC
 * @param date: New date to set
 * @param zoneOffset: Timezone at which that dat is supposed to be
 */
fun Instant.atDate(date: LocalDate, zoneOffset: ZoneOffset = ZoneOffset.UTC): Instant = LocalDateTime.ofInstant(this, zoneOffset).atDate(date).toInstant(zoneOffset)


/**
 * Changes an instant to a different time at the same date. If no [zoneOffset] given, assumes UTC
 * @param time: New time to set
 * @param zoneOffset: Timezone at which that dat is supposed to be
 */
fun Instant.atTime(time: LocalTime, zoneOffset: ZoneOffset = ZoneOffset.UTC): Instant = LocalDateTime.of(this.toLocalDate(zoneOffset), time).toInstant(zoneOffset)


/**
 * Changes this instant to the instant at start of day. If no timezone given, timezone = UTC
 */
fun Instant.atStartOfDay(zoneOffset: ZoneOffset = ZoneOffset.UTC): Instant = this.toLocalDate(zoneOffset).atStartOfDay().toInstant(zoneOffset)

fun Instant.atEndOfDay(zoneOffset: ZoneOffset = ZoneOffset.UTC): Instant = this.toLocalDate(zoneOffset).plusDays(1).atStartOfDay().toInstant(zoneOffset)

fun Instant.plusDays(daysToAdd: Int): Instant = this.plusSeconds(daysToAdd * 86400L) // 86400 seconds is 1 day

fun Instant.plusMinutes(minutesToAdd: Int): Instant = this.plusSeconds(minutesToAdd * 60L) // 86400 seconds is 1 day

/**
 * Return a string containing only the time part when converted to local time.
 * @param zoneOffset: Timezone at which we want to know the time. Standard is UTC
 * @param formatter: Format of the string. Standard is localized Short (eg. 12:34)
 */
fun Instant.toTimeString(zoneOffset: ZoneOffset = ZoneOffset.UTC, formatter: DateTimeFormatter? = DateTimeFormatter.ofPattern("HH:mm")) =
    this.toLocalTime(zoneOffset).format(formatter)

/**
 * Rounds an instant down to the previous round hour
 * eg 12:34:56.789 becomes 12:00 on same date
 * 12:00:00.000 stays 12:00
 */
fun Instant.roundHoursDown(): Instant{
    val localDateTime = LocalDateTime.ofInstant(this, ZoneOffset.UTC)
    return LocalDateTime.of(localDateTime.toLocalDate(), LocalTime.of(localDateTime.hour, 0)).toInstant(ZoneOffset.UTC)
}


operator fun Instant.minus(other: Instant): Duration = Duration.between(other, this)