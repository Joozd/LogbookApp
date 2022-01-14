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

import android.nfc.FormatException
import java.time.*
import java.time.format.DateTimeFormatter
import java.time.format.FormatStyle

private val dateFormatter = DateTimeFormatter.ofPattern("dd MMM yyyy")
private val dateFormatterTwoDigitYear = DateTimeFormatter.ofPattern("dd-MM-yy")
private val dateFormatterForFiles = DateTimeFormatter.ofPattern("yyyy-MM-dd")
private val dateFormatterLocalized = DateTimeFormatter.ofLocalizedDate(FormatStyle.SHORT)
private val timeFormatter = DateTimeFormatter.ofPattern("HH:mm")
private val timeFormatterLocalized = DateTimeFormatter.ofLocalizedTime(FormatStyle.SHORT)
private val monthYearFormatter = DateTimeFormatter.ofPattern(("MMM yyyy"))
private val dateAndTimeFormatter =  DateTimeFormatter.ofPattern("dd MMM yyyy HH:mm")

fun LocalDate.toDateString(): String = this.format(dateFormatter)
fun LocalDate.toDateStringForFiles(): String = this.format(dateFormatterForFiles)
fun LocalDateTime.toLogbookDate(): String = this.format(dateFormatterTwoDigitYear)
fun LocalDateTime.toDateStringLocalized(): String = this.format(dateFormatterLocalized)
fun LocalDateTime.toTimeStringLocalized(): String = this.format(timeFormatterLocalized)
fun LocalDateTime.toMonthYear() = this.format(monthYearFormatter).filter {it != '.'} // I don't want "MRT. 2020" but "MRT 2020"

fun String.makeLocalDateSmart(): LocalDate{
    val numbers = this.split('-', '/', ' ').map{it.toInt()}
    if (numbers.size != 3) throw FormatException("Need a string with 3 separate numbers, divided by \'-\', \'/\' or \' \'. Got $this")
    return LocalDate.of(numbers[2], numbers[1], numbers[0])
}

fun String.makeLocalTime(): LocalTime = LocalTime.parse(this, timeFormatter)

fun LocalDateTime.roundToMinutes(): LocalDateTime =
    if (this.second < 30) this.withSecond(0)
    else this.withSecond(0).plusMinutes(1)

fun LocalDateTime.atDate(date: LocalDate): LocalDateTime = LocalDateTime.of(date, this.toLocalTime())

fun LocalDate.toInstantAtStartOfDay(zoneOffset: ZoneOffset = ZoneOffset.UTC): Instant = atStartOfDay(zoneOffset).toInstant()

