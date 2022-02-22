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

import nl.joozd.joozdlogimporter.interfaces.PlannedFlightsExtractor

class KlmIcaRosterFile(lines: List<String>): PlannedFlightsFile(lines) {
    override val extractor: PlannedFlightsExtractor
        get() = TODO("Not yet implemented")

    companion object{
        private const val LINE_TO_LOOK_AT = 0
        private const val TEXT_TO_SEARCH_FOR = "CREW ROSTER FROM "

        //OK for now, but I can imagine this will need a bit more specific matching
        fun buildIfMatches(lines: List<String>): KlmIcaRosterFile? =
            if (lines[LINE_TO_LOOK_AT].startsWith(TEXT_TO_SEARCH_FOR))
                KlmIcaRosterFile(lines)
            else null
    }
}