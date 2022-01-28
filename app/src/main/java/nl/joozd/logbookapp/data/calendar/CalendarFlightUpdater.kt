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
import androidx.annotation.RequiresPermission
import nl.joozd.logbookapp.App
import nl.joozd.logbookapp.data.calendar.dataclasses.JoozdCalendar
import nl.joozd.logbookapp.data.calendar.dataclasses.JoozdCalendarEvent
import nl.joozd.logbookapp.data.calendar.parsers.KlmKlcCalendarFlightsParser
import nl.joozd.logbookapp.data.importing.interfaces.AutoRetrievedCalendar
import nl.joozd.logbookapp.data.importing.interfaces.Roster
import nl.joozd.logbookapp.data.sharedPrefs.Preferences
import java.time.Duration
import java.time.Instant

/**
 * This will scrape planned flights from calendar.
 * It is used to create an object that has a function to grab a roster from your device's calendar
 * This function will return an [AutoRetrievedCalendar] (which is a [Roster] with a validity period)
 * The period of this Roster is from highest of Now and [Preferences.calendarDisabledUntil] until [Preferences.calendarSyncAmountOfDays] days later
 */


class CalendarFlightUpdater {
    private val context = App.instance.ctx
    private val calendarScraper = CalendarScraper(context)
    private val startCutoff
        get() = maxOf(Instant.now(), Instant.ofEpochSecond(Preferences.calendarDisabledUntil))

    private val period
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