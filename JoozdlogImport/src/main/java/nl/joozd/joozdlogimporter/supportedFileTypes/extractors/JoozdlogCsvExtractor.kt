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
import nl.joozd.joozdlogimporter.interfaces.CompleteLogbookExtractor

class JoozdlogCsvExtractor: CompleteLogbookExtractor {
    override fun extractFlightsFromLines(lines: List<String>): Collection<BasicFlight> {
        val fixedLines = fixLines(lines)
        return if (fixedLines.size <= 1) emptyList()
        else fixedLines.drop(1).map {
            csvFlightToBasicFlightv5(it)
        }
    }

    private fun fixLines(lines: List<String>): List<String>{
        val output = ArrayList<String>(lines.size)
        lines.forEach{
            if (it.startsWith("<")) {
                val lastLine = output.removeLast()
                output.add(lastLine + it)
            }
            else output.add(it)
        }
        return output
    }

    private fun csvFlightToBasicFlightv5(csvFlight: String): BasicFlight =
        BasicFlight.ofCsv(csvFlight.also{ println("making flight of $it ")})
}