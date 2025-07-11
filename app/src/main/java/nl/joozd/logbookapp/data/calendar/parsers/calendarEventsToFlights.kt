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

package nl.joozd.logbookapp.data.calendar.parsers

import nl.joozd.joozdcalendarapi.CalendarEvent
import nl.joozd.joozdlogcommon.BasicFlight
import nl.joozd.joozdlogimporter.dataclasses.ExtractedPlannedFlights
import nl.joozd.joozdlogimporter.enumclasses.AirportIdentFormat
import java.time.Instant

// Matches KL123(4) ABC( )-( )DEF // int this example characters in parentheses are optional
private val flightRegExIATA = """^([A-Z]{2}\d{3,4})\s([A-Z]{3})\s?-\s?([A-Z]{3})""".toRegex()

private const val FLIGHTNUMBER = 1
private const val ORIG = 2
private const val DEST = 3

private const val PIC_IS_SET = false

fun Collection<CalendarEvent>.calendarEventsToExtractedPlannedFlights(
    period: ClosedRange<Long>? = null
): ExtractedPlannedFlights {
    val flightEvents = this.filter {flightRegExIATA.containsMatchIn(it.title)}
    val flights = flightEventsToBasicFlights(flightEvents, flightRegExIATA)
    val p = period ?: getPeriodFromFlights(flights)
    return ExtractedPlannedFlights(p, flights, PIC_IS_SET, AirportIdentFormat.IATA)
}

private fun flightEventsToBasicFlights(
    flightEvents: List<CalendarEvent>,
    flightRegEx: Regex // this is here in case I want to support ICAO regex as well
) = flightEvents.mapNotNull { event ->
    flightRegEx.find(event.title)?.let { data ->
        BasicFlight.PROTOTYPE.copy(
            orig = data.orig(),
            dest = data.dest(),
            flightNumber = data.flightNumber(),
            timeOut = event.startEpochMillis / 1000, // given in epochMillis, need epochSeconds
            timeIn = event.endEpochMillis / 1000,    // given in epochMillis, need epochSeconds
            isPlanned = true
        )
    }
}

private fun getPeriodFromFlights(flights: Collection<BasicFlight>): ClosedRange<Long>?{
    val start = flights.minOfOrNull { it.timeOut } ?: return null
    val end = flights.maxOfOrNull { it.timeIn } ?: return null
    return (Instant.ofEpochSecond(start).epochSecond..Instant.ofEpochSecond(end).epochSecond)
}

private fun MatchResult.flightNumber() = (groups[FLIGHTNUMBER]?.value ?: error ("ERROR 0003 NO FLIGHTNUMBER"))
private fun MatchResult.orig() = (groups[ORIG]?.value ?: error ("ERROR 0004 NO ORIG"))
private fun MatchResult.dest() = (groups[DEST]?.value ?: error ("ERROR 0005 NO DEST"))

