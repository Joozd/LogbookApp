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

package nl.joozd.logbookapp.data.importing.interfaces

import nl.joozd.logbookapp.data.importing.pdfparser.ProcessedRoster
import nl.joozd.logbookapp.model.dataclasses.Flight
import java.time.Instant
/**
 *a Roster to be used for parsing anything into flights
 * This must contain:
 * - A marker [isValid] stating if data is valid
 * - The [period] this Roster covers
 * - a list [flights] of [Flight] objects:
 *      - Airports can be ICAO or IATA format
 *      - ID and Timestamp will be changed, so any placeholder value will do
 *      - Aircraft registrations and types will be looked up later, fill any COMPLETE data you have.
 *          So "PH-EZP" or "PHEZP" are OK, "EZP" is not as this might lead to ambiguous results.
 *
 * Post-processing (not done in Roster but in whatever uses the Roster) should include:
 *  - Changing IATA to ICAO identifiers
 *  - Checking if registration is known, also searching for versions with/without spaces and/or hyphens and changing to known reg + type if found.
 *
 * Repository should take care of:
 *  - flightID
 *  - TimeStamp
 */
interface Roster {
    /**
     * true if the data provided to this parser seems to be valid
     */
    val isValid: Boolean

    val isInvalid
        get() = !isValid

    /**
     * The period covered by this roster.
     * Should start at start of day and end at end of day
     */
    val period: ClosedRange<Instant>
    //fun getFlights(icaoIataMap: Map<String, String>?): List<Flight>



    /**
     * List of all flights in this roster.
     * Airports can be ICAO or IATA format
     * Flights will be cleaned later, so flight ID and timestamp are not necessary.
     */
    val flights: List<Flight>

    /**
     * Cast this to a [ProcessedRoster] data class
     */
    fun toProcessedRoster(): ProcessedRoster = ProcessedRoster(isValid, period, flights)



    companion object{
        const val KLC = "KLC"
        const val KLC_CHECKIN_SHEET = "KLC_CHECKIN_SHEET"
        const val KLM = "KLM"
    }
}