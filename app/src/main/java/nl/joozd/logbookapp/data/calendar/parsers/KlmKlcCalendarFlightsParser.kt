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
import nl.joozd.logbookapp.data.parseSharedFiles.pdfparser.JoozdlogRosterParser
import nl.joozd.logbookapp.utils.reversed

/**
 * Parses JoozdCalendarEvents and builds a list of Flights.
 * All Flights have ID -1
 * suggested use: KlmCrewCalendarFlightsParser(listOfEvents).getFlights(icaoIataMap)
 */

class KlmKlcCalendarFlightsParser(events: List<JoozdCalendarEvent>):
    JoozdlogRosterParser {
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


    override fun getFlights(icaoIataMap: Map<String, String>?): List<Flight>{
        val iataIcaoMap = icaoIataMap?.reversed() ?: emptyMap<String, String>()
        return allFlightsWithIataAirportNames.map {it.copy (orig = iataIcaoMap[it.orig] ?: it.orig, dest = iataIcaoMap[it.dest] ?: it.dest )}
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