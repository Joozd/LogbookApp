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

package nl.joozd.logbookapp.extensions

import java.time.*
import java.time.format.DateTimeFormatter

fun Instant.toLocalDate(zoneOffset: ZoneOffset = ZoneOffset.UTC): LocalDate =
    LocalDateTime.ofInstant(this, zoneOffset).toLocalDate()

fun Instant.toLocalTime(zoneOffset: ZoneOffset = ZoneOffset.UTC): LocalTime =
    LocalDateTime.ofInstant(this, zoneOffset).toLocalTime()

/**
 * Changes an instant to the same time at a different date.
 */
fun Instant.atDate(date: LocalDate, zoneOffset: ZoneOffset = ZoneOffset.UTC): Instant =
    LocalDateTime.ofInstant(this, zoneOffset).atDate(date).toInstant(zoneOffset)


/**
 * Changes an instant to a different time at the same date.
 */
fun Instant.atTime(time: LocalTime, zoneOffset: ZoneOffset = ZoneOffset.UTC): Instant =
    LocalDateTime.of(this.toLocalDate(zoneOffset), time).toInstant(zoneOffset)


fun Instant.atStartOfDay(zoneOffset: ZoneOffset = ZoneOffset.UTC): Instant =
    this.toLocalDate(zoneOffset).atStartOfDay().toInstant(zoneOffset)

fun Instant.atEndOfDay(zoneOffset: ZoneOffset = ZoneOffset.UTC): Instant =
    this.toLocalDate(zoneOffset).plusDays(1).atStartOfDay().toInstant(zoneOffset)

fun Instant.plusDays(daysToAdd: Int): Instant =
    this.plusSeconds(daysToAdd * 86400L) // 86400 seconds is 1 day

fun Instant.plusMinutes(minutesToAdd: Int): Instant =
    this.plusSeconds(minutesToAdd * 60L)

fun Instant.toMonthYear(zoneOffset: ZoneOffset = ZoneOffset.UTC): String =
    LocalDateTime.ofInstant(this, zoneOffset).toMonthYear()

/**
 * Return a string containing only the time part when converted to local time.
 */
fun Instant.toTimeString(zoneOffset: ZoneOffset = ZoneOffset.UTC): String =
    LocalDateTime.ofInstant(this, zoneOffset).toLocalTimeString()

/**
 * Rounds an instant down to the previous round hour
 * eg 12:34:56.789 becomes 12:00 on same date
 * 12:00:00.000 stays 12:00
 */
fun Instant.roundHoursDown(): Instant{
    val localDateTime = LocalDateTime.ofInstant(this, ZoneOffset.UTC)
    return LocalDateTime.of(
        localDateTime.toLocalDate(),
        LocalTime.of(localDateTime.hour, 0)
    ).toInstant(ZoneOffset.UTC)
}

operator fun Instant.minus(other: Instant): Duration = Duration.between(other, this)