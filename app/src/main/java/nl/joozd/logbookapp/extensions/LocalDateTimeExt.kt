/*
 * JoozdLog Pilot's Logbook
 * Copyright (C) 2020 Joost Welle
 *
 *     This program is free software: you can redistribute it and/or modify
 *     it under the terms of the GNU Affero General Public License as
 *     published by the Free Software Foundation, either version 3 of the
 *     License, or (at your option) any later version.
 *
 *     This program is distributed in the hope that it will be useful,
 *     but WITHOUT ANY WARRANTY; without even the implied warranty of
 *     MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 *     GNU Affero General Public License for more details.
 *
 *     You should have received a copy of the GNU Affero General Public License
 *     along with this program.  If not, see https://www.gnu.org/licenses
 */

package nl.joozd.logbookapp.extensions

import java.time.LocalDate
import java.time.LocalDateTime
import java.time.LocalTime
import java.time.format.DateTimeFormatter

private val dateFormatter = DateTimeFormatter.ofPattern("dd MMM yyyy")
private val timeFormatter = DateTimeFormatter.ofPattern("HH:mm")
private val monthYearFormatter = DateTimeFormatter.ofPattern(("MMM yyyy"))
private val timeFormatterNoColon = DateTimeFormatter.ofPattern("HH:mm")
private val dateAndTimeFormatter =  DateTimeFormatter.ofPattern("dd MMM yyyy HH:mm")

fun LocalDate.toDateString() = this.format(dateFormatter)
fun LocalDateTime.toDateString() = this.format(dateFormatter)
fun LocalDateTime.toTimeString() = this.format(timeFormatter)
fun LocalDateTime.toMonthYear() = this.format(monthYearFormatter)
fun LocalDateTime.noColon() = this.format(timeFormatterNoColon)

fun String.makeLocalDate(): LocalDate = LocalDate.parse(this,
    dateFormatter
)
fun String.makeLocalDateTime(): LocalDateTime = LocalDateTime.parse(this,
    dateAndTimeFormatter
)
fun String.makeLocalDateTimeTime(): LocalDateTime = LocalDateTime.parse(this,
    timeFormatter
)
fun String.addColonToTime() = LocalDateTime.parse(this,
    timeFormatterNoColon
).toTimeString()
fun String.makeLocalTime() = LocalTime.parse(this, timeFormatter)

fun LocalDateTime.roundToMinutes() =
    if (this.second < 30) this.withSecond(0)
    else this.withSecond(0).plusMinutes(1)

fun LocalDateTime.atDate(date: LocalDate) = LocalDateTime.of(date, this.toLocalTime())