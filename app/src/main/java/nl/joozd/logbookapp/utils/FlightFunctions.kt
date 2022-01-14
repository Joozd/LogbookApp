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

package nl.joozd.logbookapp.utils

import android.util.Log
import nl.joozd.logbookapp.extensions.roundHoursDown
import nl.joozd.logbookapp.model.dataclasses.Flight
import java.time.*


/**
 * Creates a flight that is the return flight of the flight given (ie dest and orig swapped, flightNumber++)
 * If flight doesn't end with digits, it will keep same flightNumber.
 * @param flight: Flight to reverse
 * @param newID: New ID to force, if none given will set to -1 (meaning it will be assigned on save)
 */

fun reverseFlight(flight: Flight, newID: Int = -1): Flight {
    val flightNumberDigitsRegex = ".*(\\d+$)".toRegex()

    val fn = flight.flightNumber
    // Find numbers that flightnumber ends with. If found, increase by one
    val flightNumber = flightNumberDigitsRegex.find(fn)?.let{
        val digits = it.groupValues[1]
        if (digits.isBlank()) fn
        else fn.dropLast(digits.length) + (digits.toInt()+1).toString()
    } ?: fn
    val isIfr = flight.ifrTime > 0


    val now = Instant.now().roundHoursDown().epochSecond
    return flight.copy(flightID = newID, orig=flight.dest, dest=flight.orig, flightNumber = flightNumber, timeOut = now, timeIn = now + 3600, remarks = "", correctedTotalTime = 0, ifrTime = if (isIfr) 60 else 0, nightTime = 0, unknownToServer = true, isPlanned = true).also{Log.d("reverseFlight", "$it")}
}


