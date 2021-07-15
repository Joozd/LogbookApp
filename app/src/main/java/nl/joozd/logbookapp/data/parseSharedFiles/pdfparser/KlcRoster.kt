/*
 *  JoozdLog Pilot's Logbook
 *  Copyright (c) 2021 Joost Welle
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

import com.itextpdf.text.pdf.PdfReader
import com.itextpdf.text.pdf.parser.PdfTextExtractor
import com.itextpdf.text.pdf.parser.SimpleTextExtractionStrategy
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import nl.joozd.logbookapp.data.parseSharedFiles.interfaces.Roster
import nl.joozd.logbookapp.extensions.plusDays
import nl.joozd.logbookapp.extensions.toLocalDate
import nl.joozd.logbookapp.model.dataclasses.Flight
import java.io.InputStream
import java.time.Instant
import java.time.LocalDate
import java.time.LocalTime
import java.time.ZoneOffset
import java.time.format.DateTimeFormatter
import java.time.temporal.TemporalAdjusters

class KlcRoster(inputString: String?): Roster {
    private val markerRegex = """^Individual duty plan for .* NetLine/Crew\(KLC\)""".toRegex(RegexOption.MULTILINE)
    private val periodRegex = """^Period: ($DATE_REGEX) - ($DATE_REGEX)""".toRegex(RegexOption.MULTILINE)
    private val flightRegex = """^(KL \d{3,4}) R?\w?($AIRPORT_IDENT) ($TIME) ($TIME) ($AIRPORT_IDENT)""".toRegex(RegexOption.MULTILINE)
    private val currentDayRegex = """^$DAY_OF_WEEK(\d\d)""".toRegex()

    private val dateFormatter = DateTimeFormatter.ofPattern("ddMMMyy")
    private val timeFormatter = DateTimeFormatter.ofPattern("HHmm")


    override val isValid: Boolean = inputString?.let { markerRegex.containsMatchIn(it) } ?: false

    override val period: ClosedRange<Instant> = periodRegex.find(inputString?: "")?.let{
        LocalDate.parse(it.groupValues[1], dateFormatter).atStartOfDay(ZoneOffset.UTC).toInstant()..LocalDate.parse(it.groupValues[2], dateFormatter).atStartOfDay(ZoneOffset.UTC).toInstant().plusDays(1)
    } ?: Instant.EPOCH..Instant.EPOCH

    override val flights: List<Flight>
        get() = _flights

    // This will be populated by flights to fill [flights]
    private var _flights = ArrayList<Flight>()

    init{
        //The date we are currently parsing flights for
        var currentDate = period.start.toLocalDate(ZoneOffset.UTC)
        println("currentDate now $currentDate")

        inputString?.lines()?.forEach { line ->
            /*
                set current day to the day found if this line containsMatchIn [currentDayRegex]
                If this day is earlier than the previous current day (ie first of the month when previous was last of the month) add a month.
             */
            currentDayRegex.find(line)?.let{
                val day = it.groupValues[1].toInt()
                currentDate = if (currentDate.isLastDayOfMonth()) currentDate.withDayOfMonth(day).plusMonths(1) else currentDate.withDayOfMonth(day)
                println("currentDate now $currentDate")
            }

            /*
                If current line is a flight, add it to [_flights]
             */
            flightRegex.find(line)?.let{
                it.groupValues.let{ r ->
                    val flightNumber = r[1].filter {c -> c != ' '} // remove whitespace
                    val orig = r[2]
                    val dest = r[5]
                    val tOut = LocalTime.parse(r[3], timeFormatter).atDate(currentDate).toInstant(ZoneOffset.UTC).epochSecond
                    val tIn = LocalTime.parse(r[4], timeFormatter).atDate(currentDate).toInstant(ZoneOffset.UTC).epochSecond
                    _flights.add(Flight(-1, flightNumber = flightNumber, orig = orig, dest = dest, timeOut = tOut, timeIn = tIn))
                } // KL 1218 TRF 1330 1515 AMS, KL 1218, TRF, 1330, 1515, AMS
            }
        }
    }




    private fun LocalDate.isLastDayOfMonth(): Boolean = this == this.with(TemporalAdjusters.lastDayOfMonth())


    companion object{
        private const val DATE_REGEX = """\d\d[A-Z][a-z]{2}\d\d"""
        private const val AIRPORT_IDENT = """[A-Z]{3}"""
        private const val TIME = """\d{4}"""
        private const val DAY_OF_WEEK = "(?:Mon|Tue|Wed|Thu|Fri|Sat|Sun)"

        suspend fun ofInputStream(inputStream: InputStream): KlcRoster {
            @Suppress("BlockingMethodInNonBlockingContext") val reader = try {
                withContext(Dispatchers.IO){ PdfReader(inputStream) } // Dispatchers.IO doesn't have a problem with blocking methods
            } catch (e: Exception) {
                return KlcRoster(null)
            }
            if (reader.numberOfPages == 0) return KlcRoster(null)
            val roster = (1..reader.numberOfPages).joinToString("\n") { page ->
                PdfTextExtractor.getTextFromPage(reader, page, SimpleTextExtractionStrategy())
            }
            return KlcRoster(roster)
        }

    }
}