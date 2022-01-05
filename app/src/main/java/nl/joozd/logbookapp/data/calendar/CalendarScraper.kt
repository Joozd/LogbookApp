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
import android.content.Context
import android.provider.CalendarContract
import androidx.annotation.RequiresPermission
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

import nl.joozd.logbookapp.data.calendar.dataclasses.JoozdCalendar
import nl.joozd.logbookapp.data.calendar.dataclasses.JoozdCalendarEvent
import java.time.Instant

/**
 * Scrapes calendar as set in Preferences for events.
 */

class CalendarScraper(private val context: Context) {
    /**
     * Get a list of CalendarDescriptors representing the calendars on this device
     * Should not be done on main thread
     */
    @RequiresPermission(Manifest.permission.READ_CALENDAR)
    suspend fun getCalendarsList(): List<JoozdCalendar> = withContext(Dispatchers.IO){
        val foundCalendars = mutableListOf<JoozdCalendar>()
        context.contentResolver.query(
            CalendarContract.Calendars.CONTENT_URI,
            CALENDAR_PROJECTION,
            null,
            null,
            null
        )?.use { cur ->
            while (cur.moveToNext()) {
                // Get the field values
                val calID: Long = cur.getLong(PROJECTION_ID_INDEX)
                val displayName: String = cur.getString(PROJECTION_DISPLAY_NAME_INDEX)
                val accountName: String = cur.getString(PROJECTION_ACCOUNT_NAME_INDEX)
                val ownerName: String = cur.getString(PROJECTION_OWNER_ACCOUNT_INDEX)
                val name: String = cur.getString(PROJECTION_NAME_INDEX)
                val color: Int = cur.getInt(PROJECTION_CALENDAR_COLOR)
                foundCalendars.add(
                    JoozdCalendar(
                        calID,
                        displayName,
                        accountName,
                        ownerName,
                        name,
                        color
                    )
                ) //, name))
            }
        }
        foundCalendars
    }

    /**
     * Returns a list of JoozdCalendarEvents _starting_ between [start] and [end]
     * A null value means open, so all after a certain time can be gotten by leaving [end] open (or null)
     * @param activeCalendar: a JoozdCalendar object representing the calendar we are looking at
     * @param start: Earliest time for any events, can be empty
     * @param end: Latest time for any events, can be empty
     */
    @RequiresPermission(Manifest.permission.READ_CALENDAR)
    suspend fun getEventsBetween(activeCalendar: JoozdCalendar, start: Instant? = null, end: Instant? = null): List<JoozdCalendarEvent> = withContext(Dispatchers.IO){
        val selection: String
        val selectionArgs: Array<String>
        when {
            start == null && end == null -> {
                selection = "(${CalendarContract.Events.CALENDAR_ID} = ?)"
                selectionArgs = arrayOf(activeCalendar.calID.toString())
            }
            start == null -> {
                selection = "((${CalendarContract.Events.CALENDAR_ID} = ?) AND (" +
                        "${CalendarContract.Events.DTSTART} < ?))"
                selectionArgs = arrayOf(
                    activeCalendar.calID.toString(),
                    end!!.toEpochMilli().toString())
            }
            end == null -> {
                selection = "((${CalendarContract.Events.CALENDAR_ID} = ?) AND (" +
                        "${CalendarContract.Events.DTSTART} >= ?))"
                selectionArgs = arrayOf(
                    activeCalendar.calID.toString(),
                    start.toEpochMilli().toString())
            }
            else -> {
                selection = "((${CalendarContract.Events.CALENDAR_ID} = ?) AND (" +
                        "${CalendarContract.Events.DTSTART} >= ?) AND (" +
                        "${CalendarContract.Events.DTSTART} < ?))"
                selectionArgs = arrayOf(
                    activeCalendar.calID.toString(),
                    start.toEpochMilli().toString(),
                    end.toEpochMilli().toString())
            }
        }
        val foundEvents: MutableList<JoozdCalendarEvent> = mutableListOf()

        context.contentResolver.query(
            CalendarContract.Events.CONTENT_URI,
            EVENT_PROJECTION,
            selection,
            selectionArgs,
            null
        )?.use { cur ->
            while (cur.moveToNext()) {
                if (cur.getLong(EVENT_CALENDAR_ID_INDEX) == activeCalendar.calID && cur.getInt(
                        EVENT_DELETED_INDEX
                    ) == 0
                ) {
                    foundEvents.add(
                        JoozdCalendarEvent(
                            cur.getString(EVENT_TITLE_INDEX) ?: "",
                            cur.getString(EVENT_TITLE_INDEX) ?: "",
                            Instant.ofEpochMilli(cur.getLong(EVENT_DTSTART_INDEX)),
                            Instant.ofEpochMilli(cur.getLong(EVENT_DTEND_INDEX)),
                            cur.getString(EVENT_EVENT_LOCATION_INDEX) ?: "",
                            "",
                            cur.getLong(EVENT_ID_INDEX)
                        )
                    )
                }
            }
        }
        foundEvents
    }


    /*********************************************************************************************
     * Companion object
     *********************************************************************************************/

    companion object {

        /*********************************************************************************************
         * Constants and projection arrays
         *********************************************************************************************/

        // The indices for the projection array below.
        private const val PROJECTION_ID_INDEX: Int = 0
        private const val PROJECTION_ACCOUNT_NAME_INDEX: Int = 1
        private const val PROJECTION_DISPLAY_NAME_INDEX: Int = 2
        private const val PROJECTION_OWNER_ACCOUNT_INDEX: Int = 3
        private const val PROJECTION_NAME_INDEX: Int = 4
        private const val PROJECTION_CALENDAR_COLOR: Int = 5

        private const val EVENT_ID_INDEX: Int = 0
        private const val EVENT_CALENDAR_ID_INDEX: Int = 1
        private const val EVENT_TITLE_INDEX: Int = 2
        private const val EVENT_EVENT_LOCATION_INDEX: Int = 3
        private const val EVENT_DESCRIPTION_INDEX: Int = 4
        private const val EVENT_DTSTART_INDEX: Int = 5
        private const val EVENT_DTEND_INDEX: Int = 6
        private const val EVENT_ALL_DAY_INDEX: Int = 7
        private const val EVENT_DELETED_INDEX: Int = 8

        private val CALENDAR_PROJECTION: Array<String> = arrayOf(
            CalendarContract.Calendars._ID,                     // 0
            CalendarContract.Calendars.ACCOUNT_NAME,            // 1
            CalendarContract.Calendars.CALENDAR_DISPLAY_NAME,   // 2
            CalendarContract.Calendars.OWNER_ACCOUNT,           // 3
            CalendarContract.Calendars.NAME,                    // 4
            CalendarContract.Calendars.CALENDAR_COLOR           // 5
        )

        private val EVENT_PROJECTION: Array<String> = arrayOf(
            CalendarContract.Events._ID,                        // 0
            CalendarContract.Events.CALENDAR_ID,                // 1
            CalendarContract.Events.TITLE,                      // 2
            CalendarContract.Events.EVENT_LOCATION,             // 3
            CalendarContract.Events.DESCRIPTION,                // 4
            CalendarContract.Events.DTSTART,                    // 5
            CalendarContract.Events.DTEND,                      // 6
            CalendarContract.Events.ALL_DAY,                    // 7
            CalendarContract.Events.DELETED                     // 8
        )
    }

}