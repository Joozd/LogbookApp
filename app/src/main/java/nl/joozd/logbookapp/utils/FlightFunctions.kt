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

package nl.joozd.logbookapp.utils

import android.util.Log
import nl.joozd.logbookapp.data.dataclasses.Flight
import java.time.*


/**
 * will return most recent flight that is not isPlanned
 * In case no most recent not-planned flight found, it will return an empty flight with flightID -1 (that needs to be adjusted before saving)
 */
fun mostRecentCompleteFlight(flights: List<Flight>?): Flight {
    Log.d("mostRecentCompleteFlight", "started")
    return flights?.filter{!it.sim}?.maxBy { if (it.isPlanned == 0 && it.DELETEFLAG == 0) it.tOut else LocalDateTime.of(1980, 11, 27, 10, 0) } ?: Flight.createEmpty().also{ Log.d("LALALALALALALA", "YADDA YADDA YADDA")}

}

// returns a flight that is the return flight of the flight given (ie dest and orig swapped, flightnr plus one)
fun reverseFlight(flight: Flight, newID: Int): Flight {
    var flightnumber= flight.flightNumber
    var flightnumberDigits = ""
    while (flightnumber.isNotEmpty() && flightnumber.last().isDigit()){
        flightnumberDigits = flightnumber.last() + flightnumberDigits
        flightnumber = flightnumber.dropLast(1)
    }
    flightnumber = if (flightnumberDigits.isEmpty()) flightnumber else flightnumber+(flightnumberDigits.toLong() + 1).toString()
    val midnight = LocalDate.now().atStartOfDay().atZone(ZoneOffset.UTC).toInstant().epochSecond
    // return flight.copy(flightID = newID, orig=flight.dest, dest=flight.orig, flightNumber = flightnumber, timeOut = Instant.now().epochSecond -60, timeIn = Instant.now().epochSecond, remarks = "", correctedTotalTime = 0, ifrTime = 0, nightTime = 0) // nighttime etc wont be correct but times need to be edited anyway
    return flight.copy(flightID = newID, orig=flight.dest, dest=flight.orig, flightNumber = flightnumber, timeOut = midnight, timeIn = midnight, remarks = "", correctedTotalTime = 0, ifrTime = 0, nightTime = 0) // nighttime etc wont be correct but times need to be edited anyway
}


