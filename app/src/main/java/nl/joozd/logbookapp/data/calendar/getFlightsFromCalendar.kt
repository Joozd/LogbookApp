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

package nl.joozd.logbookapp.data.calendar

import android.Manifest
import android.content.Context
import androidx.annotation.RequiresPermission
import kotlinx.coroutines.withContext
import nl.joozd.joozdcalendarapi.CalendarDescriptor
import nl.joozd.joozdcalendarapi.CalendarEvent
import nl.joozd.joozdcalendarapi.EventsExtractor
import nl.joozd.joozdcalendarapi.getCalendars
import nl.joozd.joozdlogimporter.dataclasses.ExtractedPlannedFlights
import nl.joozd.logbookapp.data.calendar.parsers.calendarEventsToExtractedPlannedFlights
import nl.joozd.logbookapp.data.sharedPrefs.Prefs
import nl.joozd.logbookapp.extensions.toEpochMilliRange
import nl.joozd.logbookapp.extensions.toEpochSecondRange
import nl.joozd.logbookapp.utils.DispatcherProvider
import java.time.Duration
import java.time.Instant

private val startCutoff
    get() = maxOf(Instant.now(), Instant.ofEpochSecond(Prefs.calendarDisabledUntil.valueBlocking))

private val period
    get() = (startCutoff..Instant.now().plus(Duration.ofDays(Prefs.calendarSyncAmountOfDays)))

@RequiresPermission(Manifest.permission.READ_CALENDAR)
suspend fun Context.getFlightsFromCalendar(): ExtractedPlannedFlights? {
    val foundEvents = activeCalendar()?.let { getEventsInPeriod(it, period) } ?: return null
    return foundEvents.calendarEventsToExtractedPlannedFlights(period.toEpochSecondRange())
}


@RequiresPermission(Manifest.permission.READ_CALENDAR)
private suspend fun Context.activeCalendar(): CalendarDescriptor? = withContext(DispatcherProvider.io()) {
    getCalendars().firstOrNull { it.displayName == Prefs.selectedCalendar() }
}

private suspend fun Context.getEventsInPeriod(calendar: CalendarDescriptor, period: ClosedRange<Instant>): List<CalendarEvent>{
    val extractor = buildEventsStartingInPeriodExtractor(calendar, period)
    return withContext(DispatcherProvider.io()){
        extractor.extract(this@getEventsInPeriod)
    }
}

private fun buildEventsStartingInPeriodExtractor(calendar: CalendarDescriptor, period: ClosedRange<Instant>) =
    EventsExtractor.Builder().apply {
        fromCalendar(calendar)
        startInEpochMilliRange(period.toEpochMilliRange())
    }.build()

