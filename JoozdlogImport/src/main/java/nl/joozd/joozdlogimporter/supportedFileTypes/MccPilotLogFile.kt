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

package nl.joozd.joozdlogimporter.supportedFileTypes

import nl.joozd.joozdlogimporter.enumclasses.AirportIdentFormat
import nl.joozd.joozdlogimporter.interfaces.CompleteLogbookExtractor
import nl.joozd.joozdlogimporter.supportedFileTypes.extractors.MccPilotLogExtractor

class MccPilotLogFile(lines: List<String>): CompleteLogbookFile(lines) {
    override val extractor: CompleteLogbookExtractor
        get() = MccPilotLogExtractor()

    // MCC PilotLog CSVs can be both ICAO or IATA,
    // so we look at the first extracted flight's origin and see if is has 4 letters.
    override val identFormat by lazy {
        if (extractor.extractFlightsFromLines(data)?.firstOrNull()?.orig?.length == 3)
            AirportIdentFormat.IATA
        else AirportIdentFormat.ICAO
    }

    companion object {
        private const val TEXT_TO_SEARCH_FOR = "mcc_DATE;IS_PREVEXP;AC_ISSIM"

        fun buildIfMatches(lines: List<String>): MccPilotLogFile? =
            if (firstLineWithoutQuotesMatches(lines))
                MccPilotLogFile(lines)
            else null

        private fun firstLineWithoutQuotesMatches(lines: List<String>) =
            (lines.firstOrNull()?.filter { it != '\"' } ?: "").startsWith(TEXT_TO_SEARCH_FOR, ignoreCase = true)
    }
}
