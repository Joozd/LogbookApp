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
import nl.joozd.logbookapp.utils.reversed
import java.time.Instant
import java.time.ZoneOffset

/**
 * Parses JoozdCalendarEvents and builds a list of Flights.
 * All Flights have ID -1
 * suggested use: KlmCrewCalendarFlightsParser(listOfEvents).getFlights(icaoIataMap)
 */

class KlmKlcCalendarFlightsParser(events: List<JoozdCalendarEvent>, val icaoIataMap: Map<String, String>?):
    Roster {
    init{
        Log.d(this::class.simpleName, "Got ${events.size} events")
    }
    private val flightEvents = events.filter {flightRegEx.containsMatchIn(it.eventType)}.also{
        Log.d(this::class.simpleName, "Got ${it.size} flight events")
    }
    private val allFlightsWithIataAirportNames = flightEvents.map{event ->
        flightRegEx.find(event.description)?.let {data ->
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
    }.filterNotNull()

    private fun MatchResult.flightNumber() = (groups[FLIGHTNUMBER]?.value ?: error ("ERROR 0003 NO FLIGHTNUMBER"))
    private fun MatchResult.orig() = (groups[ORIG]?.value ?: error ("ERROR 0004 NO ORIG"))
    private fun MatchResult.dest() = (groups[DEST]?.value ?: error ("ERROR 0005 NO DEST"))


    private fun buildFlights(): List<Flight>{
        val iataIcaoMap = icaoIataMap?.reversed() ?: emptyMap<String, String>()
        return allFlightsWithIataAirportNames.map {it.copy (orig = iataIcaoMap[it.orig] ?: it.orig, dest = iataIcaoMap[it.dest] ?: it.dest )}
    }

    override val flights: List<Flight>?
        get() = buildFlights()

    override val isValid: Boolean
        get() = true

    override val period: ClosedRange<Instant> // if no flights: (0..1)
        get() {
            val start = allFlightsWithIataAirportNames.minByOrNull { it.timeOut }?.tOut()?.toLocalDate()
                ?.atStartOfDay()?.toInstant(ZoneOffset.UTC) ?: Instant.ofEpochSecond(0)
            val end = allFlightsWithIataAirportNames.maxByOrNull { it.timeIn }?.tIn()?.toLocalDate()?.plusDays(1)
                ?.atStartOfDay()?.toInstant(ZoneOffset.UTC) ?: start.plusSeconds(1)
            return(start..end)
        }

    companion object{
        const val FLIGHT_EVENT_IDENTIFIER = "FLIGHT"
        //TODO make regex for eg. `FLIGHT KL0887 AMS - HKG`

        /*
        private const val FLIGHTNUMBER = "flightNumber"
        private const val ORIG = "orig"
        private const val DEST = "dest"
        val flightRegEx = """FLIGHT (?<$FLIGHTNUMBER>[A-Z]{2}\d{3,4}) (?<$ORIG>[A-Z]{3}) - (?<$DEST>[A-Z]{3})""".toRegex()
        */
        private const val FLIGHTNUMBER = 1
        private const val ORIG = 2
        private const val DEST = 3
        val flightRegEx = """([A-Z]{2}\d{3,4})\s([A-Z]{3})\s?-\s?([A-Z]{3})""".toRegex()

    }
}