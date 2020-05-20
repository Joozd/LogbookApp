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

package nl.joozd.logbookapp.ui.activities.helpers

import nl.joozd.klcrosterparser.KlcRosterEvent
import nl.joozd.logbookapp.data.sharedPrefs.Preferences
import nl.joozd.logbookapp.model.dataclasses.Flight
import java.time.Instant

object PdfRosterFunctions{
    fun makeFlightsList(events: List<KlcRosterEvent>, startingID: Int, airportsMap: Map<String, String>): List<Flight> =
        events.mapIndexed { index: Int, rf: KlcRosterEvent ->
            val flightNumOrigArrowDest = rf.description.split(" ").map { it.trim() }
            val flightNumber = flightNumOrigArrowDest[0]
            val orig = flightNumOrigArrowDest[1]
            val dest = flightNumOrigArrowDest[3]

            Flight(
                flightID = startingID + index,
                orig = airportsMap[orig] ?: "XXXX",
                dest = airportsMap[dest] ?: "XXXX",
                timeOut = rf.startEpochSecond,
                timeIn = rf.endEpochSecond,
                flightNumber = flightNumber,
                timeStamp = Instant.now().epochSecond + Preferences.serverTimeOffset,
                unknownToServer = true
            )
        }
}
