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

package nl.joozd.logbookapp.model.helpers

import nl.joozd.logbookapp.data.repository.helpers.isSamedPlannedFlightAs
import nl.joozd.logbookapp.data.repository.helpers.prepareForSave
import nl.joozd.logbookapp.data.sharedPrefs.Preferences
import nl.joozd.logbookapp.model.dataclasses.Flight
import java.time.Instant

object FlightConflictChecker {
    /**
     * This will check if changing a flight from [originalFlight] to [changedFlight] will create a conflict with Calendar Sync.
     * @param originalFlight The flight before saving, can be null (if this is a new flight)
     * Checks a bunch of things
     * - Is it a planned flight?
     * - Is calendarSync on?
     * - Is the flight changed in a way that will make it not match a planned flight?     *
     * @return time (epochSecond) to disable calendarSync to if conflict, 0 if not
     */
    fun checkConflictingWithCalendarSync(originalFlight: Flight?, changedFlight: Flight): Long {
        return when {
            !Preferences.useCalendarSync -> 0L                                                                           // not using calendar sync
            Preferences.calendarDisabledUntil >= maxOf(originalFlight?.timeIn ?: 0, changedFlight.timeIn) -> 0L              // not using calendar sync for flight being edited
            !changedFlight.prepareForSave().isPlanned -> 0L                                                                     // not planned, no problem
            originalFlight?.isSamedPlannedFlightAs(changedFlight.prepareForSave()) == true -> 0L                                // editing a planned flight in a way that doesn't break sync
            maxOf (originalFlight?.timeOut ?: 0, changedFlight.timeOut) <
                    maxOf(Preferences.calendarDisabledUntil,Instant.now().epochSecond) -> 0L                                    // editing a flight that starts before calendar sync cutoff
            originalFlight == null && changedFlight.timeOut > Instant.now().epochSecond -> changedFlight.timeIn + 1L            // If editing a new flight that starts in the future, 1 second after end of that flight
            else -> maxOf(originalFlight?.timeIn ?: 0, changedFlight.timeIn) + 1L                                            // In other cases, 1 second after latest timeIn of planned flight and workingFlight
        }
    }
}

