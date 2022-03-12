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

package nl.joozd.joozdlogimporter.supportedFileTypes.extractors

import nl.joozd.joozdlogcommon.BasicFlight
import nl.joozd.joozdlogimporter.interfaces.CompletedFlightsExtractor
import java.time.*
import java.time.format.DateTimeFormatter

class KlcMonthlyExtractor: CompletedFlightsExtractor {
    /**
     * Look for line
     * Period: From dd-MM-yyyy to dd-MM-yyyy
     */
    override fun getPeriodFromLines(lines: List<String>): ClosedRange<Instant>? {
        val periodLine = lines.firstOrNull{ it.startsWith(PERIOD_LINE_IDENTIFIER)} ?: return null
        val searchResult = getTwoDateStringsFromLineOrNull(periodLine) ?: return null
        return makeInstantRangeFromTwoDateStrings(searchResult)
    }

    override fun extractFlightsFromLines(lines: List<String>): Collection<BasicFlight>? {
        // must match lines like:
        // 17 KL1711 07:59 AMS OPO NXB 10:26 02:27 € 65,26 Stop at OPO for 19:14h.
        val flightRegEx = """(\d{1,2}) ([A-Z]{2}\d{3,4}) (\d\d:\d\d) ([A-Z]{3}) ([A-Z]{3}) ([A-Z]{3}) (\d\d:\d\d) (\d\d:\d\d)""".toRegex()
        val monthStart = getMonthStart(lines) ?: return null
        val matches = lines.mapNotNull { flightRegEx.find(it) }
        return try{
            matchesToBasicFlights(matches, monthStart)
        } catch (e: IllegalStateException){
            println("Error while parsing: ${e.message}")
            null
        }
    }


    private fun matchesToBasicFlights(
        matches: List<MatchResult>,
        monthStart: LocalDate
    ) = matches.map { r ->
        basicFlightFromMatchResult(r, monthStart)
    }

    private fun basicFlightFromMatchResult(r: MatchResult, monthStart: LocalDate): BasicFlight {
        val instantOut: Instant = r.makeTimeOut(monthStart)
        val instantIn: Instant = r.makeTimeIn(monthStart)
        return BasicFlight.PROTOTYPE.copy(
            flightNumber = r.flightNumber(),
            orig = r.orig(), dest = r.dest(),
            timeOut = instantOut.epochSecond,
            timeIn = instantIn.epochSecond,
            registration = r.reg(),
            isPlanned = false
        )
    }

    private fun MatchResult.makeTimeOut(monthStart: LocalDate) =
        timeOut()
            .atDate(monthStart.plusDays(dayOfMonth() - 1)) // because month doesn't start at day 0
            .toInstant(ZoneOffset.UTC)

    private fun MatchResult.makeTimeIn(monthStart: LocalDate) =
        timeIn()
            .atDate(monthStart.plusDays(dayOfMonth() - 1))
            .toInstant(ZoneOffset.UTC).let {
                if (it <= makeTimeOut(monthStart)) it.plusSeconds(ONE_DAY_IN_SECONDS) // in case arriving after midnight
                else it
            }

    private fun getTwoDateStringsFromLineOrNull(periodLine: String): List<String>? {
        val dateRegEx = """\d{2}-\d{2}-\d{4}""".toRegex() // matches dd-MM-yyyy
        return dateRegEx.findAll(periodLine)
            .map{ it.value }
            .toList()
            .takeIf { it.size == 2 }
    }

    private fun makeInstantRangeFromTwoDateStrings(ss: List<String>): ClosedRange<Instant>{
        val dateFormatter = DateTimeFormatter.ofPattern("dd-MM-yyyy")
        return ss.map{ LocalDate.parse(it, dateFormatter)}.let{
            it.first().instantAtStartOfDay()..it.last().instantAtEndOfDay()
        }
    }

    private fun getMonthStart(lines: List<String>): LocalDate? {
        val period = getPeriodFromLines(lines) ?: return null
        val startDate = LocalDateTime.ofInstant(period.start, ZoneOffset.UTC).toLocalDate()
        return startDate.withDayOfMonth(1)
    }

    private fun LocalDate.instantAtStartOfDay() =
        atStartOfDay().toInstant(ZoneOffset.UTC)

    private fun LocalDate.instantAtEndOfDay() =
        plusDays(1).instantAtStartOfDay()

    private val timeFormatter = DateTimeFormatter.ofPattern("HH:mm")
    private fun MatchResult.dayOfMonth(): Long = (this.groups[DAY]?.value ?: error ("ERROR 0003 NO DAY")).toLong()
    private fun MatchResult.flightNumber(): String = (this.groups[FLIGHTNUMBER]?.value ?: error ("ERROR 0003 NO FLIGHT NUMBER"))
    private fun MatchResult.timeOut() = LocalTime.parse(groups[TIME_OUT]?.value ?: error ("ERROR 0001 NO TIME_OUT"), timeFormatter)
    private fun MatchResult.timeIn() = LocalTime.parse(groups[TIME_IN]?.value ?: error ("ERROR 0002 NO TIME_IN"), timeFormatter)
    private fun MatchResult.orig() = (groups[ORIG]?.value ?: error ("ERROR 0004 NO ORIG"))
    private fun MatchResult.dest() = (groups[DEST]?.value ?: error ("ERROR 0005 NO DEST"))
    private fun MatchResult.reg() = (groups[REGISTRATION]?.value?.let { "PH-$it" } ?: error ("ERROR 0005 NO REGISTRATION"))

    companion object {
        const val PERIOD_LINE_IDENTIFIER = "Period: From "

        const val DAY = 1
        const val FLIGHTNUMBER = 2
        const val TIME_OUT = 3
        const val ORIG = 4
        const val DEST = 5
        const val REGISTRATION = 6
        const val TIME_IN = 7
        const val TOTAL_TIME = 8

        const val ONE_DAY_IN_SECONDS = 86400L
    }
}