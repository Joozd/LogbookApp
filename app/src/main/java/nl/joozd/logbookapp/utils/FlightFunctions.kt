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

package nl.joozd.logbookapp.utils

import android.util.Log
import nl.joozd.logbookapp.extensions.toInt
import nl.joozd.logbookapp.model.dataclasses.Flight
import java.time.*


// returns a flight that is the return flight of the flight given (ie dest and orig swapped, flightnr plus one)
fun reverseFlight(flight: Flight, newID: Int, unknownToServer: Boolean = true): Flight {
    var flightnumber= flight.flightNumber
    var flightnumberDigits = ""
    while (flightnumber.isNotEmpty() && flightnumber.last().isDigit()){
        flightnumberDigits = flightnumber.last() + flightnumberDigits
        flightnumber = flightnumber.dropLast(1)
    }
    flightnumber = if (flightnumberDigits.isEmpty()) flightnumber else flightnumber+(flightnumberDigits.toLong() + 1).toString()
    val midnight = LocalDate.now().atStartOfDay().atZone(ZoneOffset.UTC).toInstant().epochSecond
    return flight.copy(flightID = newID, orig=flight.dest, dest=flight.orig, flightNumber = flightnumber, timeOut = midnight, timeIn = midnight, remarks = "", correctedTotalTime = 0, ifrTime = 0, nightTime = 0, unknownToServer = unknownToServer).also{Log.d("reverseFlight", "$it")}
}


