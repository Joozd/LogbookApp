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

package nl.joozd.logbookapp.data.calendar.dataclasses

import java.time.Duration
import java.time.Instant

/**
 * Event that will be put into calendar
 * @param eventType: Type of event as defined in Activities object that comes with KlcRosterParser
 * @param description: Description of activity. This will be the calendar item's name
 * @param startTime: Start time of event
 * @param endTime: End time of event
 * @param extraData: Extra data, this till end up in event's location so it will show up on overview
 * @param notes: Notes for activity
 * @param _id: id for keeping track of retrieved events from calendar, to be able to delete them.
 */

data class JoozdCalendarEvent (val eventType: String, val description: String, val startTime: Instant, val endTime: Instant, val extraData: String = "", val notes: String = "", val _id: Long? = null){
    val startInstant: Long
        get() = startTime.toEpochMilli()
    val endInstant: Long
        get() = endTime.toEpochMilli()
    val duration = Duration.between(startTime, endTime)
    override fun toString() = "Event: $eventType / $description / start: $startTime / end: $endTime"
}