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

package nl.joozd.logbookapp.data.calendar.parsers

import android.util.Log
import nl.joozd.logbookapp.data.calendar.dataclasses.JoozdCalendarEvent
import nl.joozd.logbookapp.model.dataclasses.Flight
import nl.joozd.logbookapp.utils.TimestampMaker
import nl.joozd.logbookapp.data.parseSharedFiles.interfaces.Roster
import java.time.Instant

/**
 * Parses JoozdCalendarEvents and builds a list of Flights.
 * All Flights have ID -1
 * suggested use: KlmCrewCalendarFlightsParser(listOfEvents).getFlights(icaoIataMap)
 * @param events: Events that might contain a flight (a flight is anything that matches [flightRegEx])
 * period: start of day of start of first flight until end of day of end of last flight
 */

class KlmKlcCalendarFlightsParser(events: List<JoozdCalendarEvent>, override val period: ClosedRange<Instant>):
    Roster {
    init{
        Log.d(this::class.simpleName, "Got ${events.size} events")
    }
    private val flightEvents = events.filter {flightRegEx.containsMatchIn(it.eventType)}.also{
        Log.d(this::class.simpleName, "Got ${it.size} flight events")
    }
    override val flights = flightEvents.mapNotNull { event ->
        flightRegEx.find(event.description)?.let { data ->
            Flight(
                -1,
                orig = data.orig(),
                dest = data.dest(),
                flightNumber = data.flightNumber(),
                timeOut = event.startTime.epochSecond,
                timeIn = event.endTime.epochSecond,
                unknownToServer = true,
                timeStamp = TimestampMaker.nowForSycPurposes,
                isPlanned = true
            )
        }
    }

    private fun MatchResult.flightNumber() = (groups[FLIGHTNUMBER]?.value ?: error ("ERROR 0003 NO FLIGHTNUMBER"))
    private fun MatchResult.orig() = (groups[ORIG]?.value ?: error ("ERROR 0004 NO ORIG"))
    private fun MatchResult.dest() = (groups[DEST]?.value ?: error ("ERROR 0005 NO DEST"))


    /**
     * Identifier of the carrier.
     * See companion object.
     */
    override val carrier: String? = null

    override val isValid: Boolean
        get() = true

    override fun close() {
        // intentionally left blank
    }

    companion object{
        const val FLIGHT_EVENT_IDENTIFIER = "FLIGHT"
        //TODO make regex for eg. `FLIGHT KL0887 AMS - HKG`

        private const val FLIGHTNUMBER = 1
        private const val ORIG = 2
        private const val DEST = 3
        val flightRegEx = """([A-Z]{2}\d{3,4})\s([A-Z]{3})\s?-\s?([A-Z]{3})""".toRegex()

    }
}