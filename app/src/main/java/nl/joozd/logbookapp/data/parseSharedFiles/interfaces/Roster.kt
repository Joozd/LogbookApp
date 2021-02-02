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

package nl.joozd.logbookapp.data.parseSharedFiles.interfaces

import nl.joozd.logbookapp.model.dataclasses.Flight
import java.time.Instant
/**
 *a Roster to be used for parsing anything into flights
 */
interface Roster {
    /**
     * Identifier of the carrier.
     * See companion object.
     */
    val carrier: String?

    /**
     * true if the data provided to this parser seems to be valid
     */
    val isValid: Boolean

    /**
     * List of all flights in this roster.
     * Airports can be ICAO or IATA format
     * Flights will be cleaned later, so flight ID and timestamp are not necessary.
     */
    val flights: List<Flight>?

    /**
     * The period covered by this roster.
     * Should start at start of day and end at end of day
     */
    val period: ClosedRange<Instant>?
    //fun getFlights(icaoIataMap: Map<String, String>?): List<Flight>

    companion object{
        const val KLC = "KLC"
        const val KLC_CHECKIN_SHEET = "KLC_CHECKIN_SHEET"
        const val KLM = "KLM"
    }
}