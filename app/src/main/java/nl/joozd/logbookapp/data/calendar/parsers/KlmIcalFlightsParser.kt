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

import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import nl.joozd.logbookapp.data.parseSharedFiles.interfaces.AutoRetrievedCalendar
import nl.joozd.logbookapp.extensions.atEndOfDay
import nl.joozd.logbookapp.extensions.plusMinutes
import nl.joozd.logbookapp.model.dataclasses.Flight
import java.net.URL
import java.time.Instant
import java.time.LocalDateTime
import java.time.ZoneOffset
import java.time.format.DateTimeFormatter

/**
 * This class contains a calendar scraped from a KLM Ical address.
 * @param icalString: The downloaded iCalendar file, as a single String
 */
class KlmIcalFlightsParser(icalString: String): AutoRetrievedCalendar {
    private val now = Instant.now()
    private val eventRegex = "BEGIN:VEVENT.*?END:VEVENT".toRegex(RegexOption.DOT_MATCHES_ALL)
    private val flightRegex = """SUMMARY:(?:FLIGHT )?($FLIGHT_NUMBER) ($AIRPORT_IDENT)\s?-\s?($AIRPORT_IDENT)""".toRegex()
    private val tOutRegex = """DTSTART:(.*)""".toRegex()
    private val tInRegex = """DTEND:(.*)""".toRegex()
    private val validUntilRegex = """REFRESH-INTERVAL;VALUE=DURATION:PT(\d+)H""".toRegex()

    private val timeFormatter = DateTimeFormatter.ofPattern("""yyyyMMdd'T'HHmmssX""") // 20210303T235500Z

    override val isValid: Boolean = icalString.startsWith(START_IDENTIFIER) && icalString.endsWith(END_IDENTIFIER)

    override val validUntil: Instant = validUntilRegex.find(icalString)?.let{
        Instant.now().plusMinutes(60 * it.groupValues[1].toInt())
    } ?: Instant.now().plusMinutes(15) // fifteen minutes seems ok for a backup validity period?

    override val period: ClosedRange<Instant> by lazy{
        now..(flights.maxByOrNull{ it.tOut()}?.tIn()?.toInstant(ZoneOffset.UTC)?.atEndOfDay() ?: Instant.EPOCH)
    }


    override val flights: List<Flight> = eventRegex.findAll(icalString).mapNotNull { eventResult ->
        flightRegex.find(eventResult.value)?.let{ flightResult ->
            val flightNumber = flightResult.groupValues[1]
            val orig = flightResult.groupValues[2]
            val dest = flightResult.groupValues[3]
            val tOutString = tOutRegex.find(eventResult.value)?.groupValues?.get(1) ?: return@let null
            val tInString = tInRegex.find(eventResult.value)?.groupValues?.get(1) ?: return@let null
            val tOut = LocalDateTime.parse(tOutString, timeFormatter).toInstant(ZoneOffset.UTC).epochSecond
            val tIn = LocalDateTime.parse(tInString, timeFormatter).toInstant(ZoneOffset.UTC).epochSecond

            Flight(-1, flightNumber = flightNumber, orig = orig, dest = dest, timeOut = tOut, timeIn = tIn)
        }
    }.filter {
        it.timeOut > now.epochSecond
    }.toList()

    companion object{
        private const val START_IDENTIFIER = "BEGIN:VCALENDAR"
        private const val END_IDENTIFIER = "END:VCALENDAR"
        private const val FLIGHT_NUMBER = """[A-Z]{2}\d{3,4}"""
        private const val AIRPORT_IDENT = """[A-Z]{3}"""

        suspend fun ofURL(url: URL) = withContext(Dispatchers.IO){
            KlmIcalFlightsParser(url.readText())
        }

        @Suppress("BlockingMethodInNonBlockingContext")
        suspend fun ofString(s: String) = withContext(Dispatchers.IO) {
            println("Creating AAP BANAAN XXX")
            val url = URL(s)
            println("Creating AAP BANAAN YYY")
            ofURL(url).also{
            println("Created AAP BANAAN XXX")
        } }
    }
}