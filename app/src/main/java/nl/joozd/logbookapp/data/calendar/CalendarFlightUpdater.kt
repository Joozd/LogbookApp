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

package nl.joozd.logbookapp.data.calendar

import android.Manifest
import androidx.annotation.RequiresPermission
import nl.joozd.logbookapp.App
import nl.joozd.logbookapp.data.calendar.dataclasses.JoozdCalendar
import nl.joozd.logbookapp.data.calendar.dataclasses.JoozdCalendarEvent
import nl.joozd.logbookapp.data.calendar.parsers.KlmKlcCalendarFlightsParser
import nl.joozd.logbookapp.data.parseSharedFiles.interfaces.AutoRetrievedCalendar
import nl.joozd.logbookapp.data.parseSharedFiles.interfaces.Roster
import nl.joozd.logbookapp.data.sharedPrefs.Preferences
import nl.joozd.logbookapp.extensions.atStartOfDay
import nl.joozd.logbookapp.model.dataclasses.Flight
import java.time.Duration
import java.time.Instant

/**
 * This will scrape planned flights from calendar.
 * It will need some extra smarts, such as cut-off times etc
 */


class CalendarFlightUpdater {
    private val context = App.instance.ctx
    private val calendarScraper = CalendarScraper(context)
    private val startCutoff
        get() = maxOf(Instant.now().atStartOfDay(), Instant.ofEpochSecond(Preferences.calendarDisabledUntil))

    val period
        get() = (startCutoff..Instant.now().plus(Duration.ofDays(Preferences.calendarSyncAmountOfDays)))

    /**
     * Get active calendar
     */
    @RequiresPermission(Manifest.permission.READ_CALENDAR)
    private suspend fun activeCalendar(): JoozdCalendar? = calendarScraper.getCalendarsList().firstOrNull { it.name == Preferences.selectedCalendar }

    /**
     * This will scrape all flights in [period] from the selected calendar
     * @return a [Roster]
     */
    @RequiresPermission(Manifest.permission.READ_CALENDAR)
    suspend fun getRoster(): AutoRetrievedCalendar? {
        val foundEvents: List<JoozdCalendarEvent> = activeCalendar()?.let{
            calendarScraper.getEventsBetween(it, period.start, period.endInclusive)
        } ?: return null
        return KlmKlcCalendarFlightsParser(foundEvents, period)
    }
}