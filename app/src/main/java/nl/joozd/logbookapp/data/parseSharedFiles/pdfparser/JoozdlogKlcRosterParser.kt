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

package nl.joozd.logbookapp.data.parseSharedFiles.pdfparser

import android.util.Log
import nl.joozd.klcrosterparser.Activities
import nl.joozd.klcrosterparser.KlcRosterEvent
import nl.joozd.klcrosterparser.KlcRosterParser
import nl.joozd.logbookapp.model.dataclasses.Flight
import nl.joozd.logbookapp.utils.TimestampMaker
import nl.joozd.logbookapp.utils.reversed
import java.io.InputStream
import java.time.Instant

class JoozdlogKlcRosterParser(inputStream: InputStream):
    JoozdlogRosterParser {

    /*********************************************************************************************
     * Private parts
     *********************************************************************************************/

    private val roster = KlcRosterParser(inputStream)

    //flights with [orig] and [dest] in IATA format
    private val flightsToPlan: List<Flight> = eventsToFlights(roster.days.map { it.events }.flatten()
        .filter { it.type == Activities.FLIGHT })

    // simsToPlan are all actual sim times in roster
    private val simsToPlan: List<Flight> = eventsToSims(roster.days.map { it.events }.flatten()
        .filter { it.type == Activities.ACTUALSIM })


    /**
     * Makes KlcRosterEvents into Flights
     * NOTE: Airports are IATA format, flightID = -1
     */
    private fun eventsToFlights(events: List<KlcRosterEvent>): List<Flight> =
        events.map { rf ->
            val flightNumOrigArrowDest = rf.description.split(" ").map { it.trim() }
            val flightNumber = flightNumOrigArrowDest[0]
            val orig = flightNumOrigArrowDest[1]
            val dest = flightNumOrigArrowDest[3]

            Flight(
                flightID = -1,
                orig = orig,
                dest = dest,
                timeOut = rf.startEpochSecond,
                timeIn = rf.endEpochSecond,
                flightNumber = flightNumber,
                timeStamp = TimestampMaker.nowForSycPurposes,
                isPlanned = true,
                unknownToServer = true
            )
        }
    private fun eventsToSims(events: List<KlcRosterEvent>): List<Flight> =
        events.map { rf ->
            // val description = rf.description
            val simTime: Int = (rf.endEpochSecond - rf.startEpochSecond).toInt() / 60

            Flight(
                flightID = -1,
                timeOut = rf.startEpochSecond,
                timeIn = rf.endEpochSecond,
                simTime = simTime,
                timeStamp = TimestampMaker.nowForSycPurposes,
                isSim = true,
                isPlanned = true,
                unknownToServer = true
            )
        }

    /*********************************************************************************************
     * Public parts
     *********************************************************************************************/

    override val isValid = roster.seemsValid
    
    /**
     * getFlights needs an icaoIataMap
     * @param icaoIataMap: map that holds icao names as keys and iata names as values
     * @return list of flights (flightIDs are -1)
     */
    override fun getFlights(icaoIataMap: Map<String, String>?): List<Flight>{
        val iataIcaoMap = icaoIataMap?.reversed() ?: emptyMap<String, String>()
        Log.d("KLC Roster Parser", "found ${flightsToPlan.size} flights")
        return flightsToPlan.map{it.copy (orig = iataIcaoMap[it.orig] ?: it.orig, dest = iataIcaoMap[it.dest] ?: it.dest)} +
                simsToPlan
    }

    val period = (Instant.ofEpochSecond(roster.period!!.start)..Instant.ofEpochSecond(roster.period!!.endInclusive))

}


