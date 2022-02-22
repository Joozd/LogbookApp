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

import nl.joozd.joozdlogimporter.interfaces.CompleteLogbookExtractor

class MccPilotLogFile(lines: List<String>): CompleteLogbookFile(lines) {
    override val extractor: CompleteLogbookExtractor
        get() = TODO("Not yet implemented")

    companion object {
        private const val TEXT_TO_SEARCH_FOR =
            "\"mcc_DATE\";\"Is_PREVEXP\";\"AC_IsSIM\";\"FlightNumber\";\"AF_DEP\";\"TIME_DEP\";\"TIME_DEPSCH\";\"AF_ARR\";\"TIME_ARR\";\"TIME_ARRSCH\";\"AC_MODEL\";\"AC_REG\";\"PILOT1_ID\";\"PILOT1_NAME\";\"PILOT1_PHONE\";\"PILOT1_EMAIL\";\"PILOT2_ID\";\"PILOT2_NAME\";\"PILOT2_PHONE\";\"PILOT2_EMAIL\";\"PILOT3_ID\";\"PILOT3_NAME\";\"PILOT3_PHONE\";\"PILOT3_EMAIL\";\"PILOT4_ID\";\"PILOT4_NAME\";\"PILOT4_PHONE\";\"PILOT4_EMAIL\";\"TIME_TOTAL\";\"TIME_PIC\";\"TIME_PICUS\";\"TIME_SIC\";\"TIME_DUAL\";\"TIME_INSTRUCTOR\";\"TIME_EXAMINER\";\"TIME_NIGHT\";\"TIME_RELIEF\";\"TIME_IFR\";\"TIME_ACTUAL\";\"TIME_HOOD\";\"TIME_XC\";\"PF\";\"TO_DAY\";\"TO_NIGHT\";\"LDG_DAY\";\"LDG_NIGHT\";\"AUTOLAND\";\"HOLDING\";\"LIFT\";\"INSTRUCTION\";\"REMARKS\";\"APP_1\";\"APP_2\";\"APP_3\";\"Pax\";\"DEICE\";\"FUEL\";\"FUELUSED\";\"DELAY\";\"FLIGHTLOG\";\"TIME_TO\";\"TIME_LDG\";\"TIME_AIR\""

        fun buildIfMatches(lines: List<String>): MccPilotLogFile? =
            if ((lines.firstOrNull() ?: "").startsWith(TEXT_TO_SEARCH_FOR))
                MccPilotLogFile(lines)
            else null
    }
}
