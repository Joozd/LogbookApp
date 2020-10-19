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

import android.nfc.FormatException
import java.time.LocalDate
import java.time.LocalDateTime
import java.time.LocalTime
import java.time.ZoneOffset
import java.time.format.DateTimeFormatter
import java.time.format.FormatStyle

private val dateFormatter = DateTimeFormatter.ofPattern("dd MMM yyyy")
private val dateFormatterTwoDigitYear = DateTimeFormatter.ofPattern("dd-MM-yy")
private val dateFormatterForFiles = DateTimeFormatter.ofPattern("yyyy-MM-dd")
private val dateFormatterLocalized = DateTimeFormatter.ofLocalizedDate(FormatStyle.SHORT)
private val timeFormatter = DateTimeFormatter.ofPattern("HH:mm")
private val timeFormatterLocalized = DateTimeFormatter.ofLocalizedTime(FormatStyle.SHORT)
private val monthYearFormatter = DateTimeFormatter.ofPattern(("MMM yyyy"))
private val timeFormatterNoColon = DateTimeFormatter.ofPattern("HH:mm")
private val dateAndTimeFormatter =  DateTimeFormatter.ofPattern("dd MMM yyyy HH:mm")

fun LocalDate.toDateString() = this.format(dateFormatter)
fun LocalDate.toDateStringLocalized() = this.format(dateFormatterLocalized)
fun LocalDate.toDateStringForFiles() = this.format(dateFormatterForFiles)
fun LocalDateTime.toDateString() = this.format(dateFormatter)
fun LocalDateTime.toLogbookDate() = this.format(dateFormatterTwoDigitYear)
fun LocalDateTime.toDateStringLocalized() = this.format(dateFormatterLocalized)
fun LocalDateTime.toTimeString() = this.format(timeFormatter)
fun LocalDateTime.toTimeStringLocalized() = this.format(timeFormatterLocalized)
fun LocalDateTime.toMonthYear() = this.format(monthYearFormatter)
fun LocalDateTime.noColon() = this.format(timeFormatterNoColon)

fun String.makeLocalDate(): LocalDate = LocalDate.parse(this, dateFormatter)

fun String.makeLocalDateSmart(): LocalDate{
    val numbers = this.split('-', '/', ' ').map{it.toInt()}
    if (numbers.size != 3) throw FormatException("Need a string with 3 separate numbers, divided by \'-\', \'/\' or \' \'. Got $this")
    return LocalDate.of(numbers[2], numbers[1], numbers[0])
}

fun String.makeLocalDateTime(): LocalDateTime = LocalDateTime.parse(this, dateAndTimeFormatter)

fun String.makeLocalDateTimeTime(): LocalDateTime = LocalDateTime.parse(this, timeFormatter)

fun String.addColonToTime() = LocalDateTime.parse(this, timeFormatterNoColon).toTimeString()

fun String.makeLocalTime() = LocalTime.parse(this, timeFormatter)

fun LocalDateTime.roundToMinutes() =
    if (this.second < 30) this.withSecond(0)
    else this.withSecond(0).plusMinutes(1)

fun LocalDateTime.atDate(date: LocalDate) = LocalDateTime.of(date, this.toLocalTime())

/**
 * Instant at start of day
 * @param zoneOffset Zoneoffset, defaults to UTC
 */
fun LocalDate.toInstant(zoneOffset: ZoneOffset = java.time.ZoneOffset.UTC) = atStartOfDay(zoneOffset).toInstant()