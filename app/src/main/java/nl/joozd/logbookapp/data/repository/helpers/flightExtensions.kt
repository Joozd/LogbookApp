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

package nl.joozd.logbookapp.data.repository.helpers

import nl.joozd.logbookapp.data.sharedPrefs.Preferences
import nl.joozd.logbookapp.extensions.atEndOfDay
import nl.joozd.logbookapp.extensions.nullIfEmpty
import nl.joozd.logbookapp.extensions.nullIfZero
import nl.joozd.logbookapp.model.dataclasses.Flight
import nl.joozd.logbookapp.utils.TimestampMaker
import java.time.*
import java.util.*

const val PLANNING_MARGIN = 300 // seconds = 5 minutes. Flights saved with timeIn more than this
                                // amount of time into the future will be marked isPlanned

fun Flight.prepareForSave(): Flight{
    val now = Instant.now().epochSecond
    //planned if time in later than now (with a bit of margin) or sim later than end of local day
    return this.copy(isPlanned = if (isSim) timeIn > Instant.now().atEndOfDay(ZonedDateTime.now().offset).epochSecond else (timeIn > now + PLANNING_MARGIN), timeStamp = TimestampMaker.nowForSycPurposes)
}

fun Flight.isSamedPlannedFlightAs(f: Flight) =
    isSameFlightAs(f) && isPlanned

/**
 * Checks if two flights are the same physical flight (ie the same orig to same dest at the same time with the same flightnumber)
 */
fun Flight.isSameFlightAs(f: Flight) = (Preferences.maxChronoAdjustment * 60L).let { margin ->
           orig == f.orig
                && dest == f.dest
                && timeOut in (f.timeOut - margin..f.timeOut + margin)
                && timeIn in (f.timeIn - margin..f.timeIn + margin)
                && hasSameflightNumberAs(f)
}

/**
 * Checks if flights are the same, times may be off by max [margin] seconds
 * This is to be used when entering flights from Monthly Overviews, to detect flights with slightly incorrect times
 */
    fun Flight.isSameCompletedFlight(f: Flight, margin: Long) =
    orig == f.orig
            && dest == f.dest
            && timeOut in (f.timeOut-margin .. f.timeOut+margin)
            && timeIn in (f.timeIn-margin .. f.timeIn+margin)

    fun Flight.isSameCompletedFlight(f: Flight) = isSameCompletedFlight(f, Preferences.maxChronoAdjustment * 60L)


/**
 * Checks if flights are the same
 * Counts as the same as same orig/dest/flightnumber on same departure date (Z time)
 */
fun Flight.isSameFlightOnSameDay(f: Flight) =
    orig == f.orig
            && dest == f.dest
            && tOut().toLocalDate() == f.tOut().toLocalDate()
            && hasSameflightNumberAs(f)

/**
 * true if this flights time in or out are between other flights time in and out
 * returns false if other is null
 */
fun Flight.overlaps(other: Flight?): Boolean {
    if (other == null) return false
    val range = other.timeOut..other.timeIn
    return timeOut in range || timeIn in range
}

/**
 * Merge data into other flight, if filled
 *  - Flight number
 *  - time out (not overwritten if 0)
 *  - time in (not overwritten if 0)
 *  - Registration
 *  - Aircraft Type
 *  - isPlanned flag if not planned
 */
fun Flight.mergeInto(other: Flight) = other.copy(
    flightNumber = flightNumber.nullIfEmpty() ?: other.flightNumber,
    timeOut = timeOut.nullIfZero() ?: other.timeOut,
    timeIn = timeIn.nullIfZero() ?: other.timeIn,
    registration = registration.nullIfEmpty() ?: other.registration,
    aircraftType = aircraftType.nullIfEmpty() ?: other.aircraftType,
    isPlanned = isPlanned && other.isPlanned
)


fun Flight.hasSameflightNumberAs(other: Flight) = flightNumber.toUpperCase(Locale.ROOT).trim() == other.flightNumber.toUpperCase(Locale.ROOT).trim()


fun Flight.shortString() = "$flightID: ${tOut()} $orig-$dest / $registration"