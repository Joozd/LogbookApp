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

import com.itextpdf.text.pdf.PdfReader
import com.itextpdf.text.pdf.parser.PdfTextExtractor
import com.itextpdf.text.pdf.parser.SimpleTextExtractionStrategy
import nl.joozd.logbookapp.data.parseSharedFiles.interfaces.MonthlyOverview
import nl.joozd.logbookapp.extensions.toInstant
import nl.joozd.logbookapp.extensions.toLocalDate
import nl.joozd.logbookapp.model.dataclasses.Flight
import java.io.InputStream
import java.time.*
import java.time.format.DateTimeFormatter

/**
 * Recommend to create this async as it has some blocking code in constructor (reading of inputstream)
 */
class KlmMonthlyParser(inputStream: InputStream): MonthlyOverview {

    /**
     * regexes
     */
    // Periode: 01-08-2020 t/m 31-08-2020
    private val periodRegex = """Periode: $DATE t/m $DATE""".toRegex()

    // 16 KL 856 PHBVK   14:55 15:38 ICN +9 FO AMS +2 02:34 03:04 10:56 12:09 32:25 12:09 11:39
    //                             1day    2flightnr    3reg          (4c/i) 5out   6orig   Dtime   7Func    8dest  Dtime  9in  ignore rest of line
    private val flightLine = """(\d{2}) $FLIGHTNUMBER $REGISTRATION\s+$TIME $TIME $AIRPORT .?\d+ $FUNCTION $AIRPORT .?\d+ $TIME.*""".toRegex()

    /**
     * Data related to the PDF and parsing and being valid and such
     */
    private val reader = PdfReader(inputStream)
    private val firstPage = PdfTextExtractor.getTextFromPage(reader, 1, SimpleTextExtractionStrategy())
    private var _dataSeemsValid: Boolean = IDENT_LINE in firstPage





    //Whole text of document
    private val text: String =
        (1..reader.numberOfPages).joinToString("\n") { pageNumber ->
            PdfTextExtractor.getTextFromPage(reader, pageNumber, SimpleTextExtractionStrategy())
        }

    private fun makePeriod() = periodRegex.find(text)?.groupValues?.let{ results ->
        val formatter = DateTimeFormatter.ofPattern("dd-MM-yyyy")
        val start = LocalDate.parse(results[1], formatter)
        val end = LocalDate.parse(results[2], formatter)
        (start.toInstant()..end.plusDays(1).toInstant())
    }

    //1day    2flightnr    3reg          (4c/i) 5out   6orig   Dtime   7Func    8dest  Dtime  9in  ignore rest of line
    private fun makeFlight(result: MatchResult): Flight? = result.groupValues.let{values ->
        if (!_dataSeemsValid || period == null) return null
        val timeFormatter = DateTimeFormatter.ofPattern("HH:mm")

        val day = values[1].toInt()
        val flightNumber = values[2].split(' ').joinToString("") // remove any whitespaces
        val registration = values[3]
        val timeOut = LocalTime.parse(values[5], timeFormatter)
        val timeIn = LocalTime.parse(values[9], timeFormatter)
        val function = values[7]
        val orig = values[6]
        val dest = values[8]

        val date = period!!.start.toLocalDate().withDayOfMonth(day)
        val tOut = LocalDateTime.of(date, timeOut).toInstant(ZoneOffset.UTC).epochSecond
        val tIn = LocalDateTime.of(date, timeIn).toInstant(ZoneOffset.UTC).epochSecond.let{
            if (it > tOut) it else it + ONE_DAY
        }

        val coPilots= listOf(COCO, COPILOT)

        Flight(-1, flightNumber = flightNumber,  orig = orig, dest = dest, timeOut = tOut, timeIn = tIn, registration = registration, isPIC = function == CAPTAIN, isCoPilot = function in coPilots)
    }

    private fun makeFlights() = flightLine.findAll(text).map{
        makeFlight(it)
    }.filterNotNull()

    /**
     * true if this seems to be a valid monthly overview (gross error check)
     */
    override val validMonthlyOverview: Boolean
        get() = _dataSeemsValid

    /**
     * List of flights in this Monthly Overview (to be cleaned)
     */
    override val flights: List<Flight>?
        get() =
            if (!_dataSeemsValid) null
            else makeFlights().toList()


    /**
     * Period that this Monthly Overview applies to
     */
    override val period: ClosedRange<Instant>? by lazy {
        makePeriod()
    }


    companion object {
        private const val TIME = "(\\d\\d:\\d\\d)"
        private const val DATE = "(\\d{2}-\\d{2}-\\d{4})"
        private const val FLIGHTNUMBER = "([A-Z]{2} \\d{3})"
        private const val REGISTRATION = "([A-Z]{5})"
        private const val AIRPORT = "([A-Z]{3})"

        private const val CAPTAIN = "CP"
        private const val COPILOT = "FO"
        private const val COCO = "SO"

        private const val FUNCTION = "($CAPTAIN|$COPILOT|$COCO)" // TODO FIND A SO AND CP TO CHECK

        private const val IDENT_LINE = "Chronologisch overzicht Vlieguren"

        private const val ONE_DAY = 86400
    }



}