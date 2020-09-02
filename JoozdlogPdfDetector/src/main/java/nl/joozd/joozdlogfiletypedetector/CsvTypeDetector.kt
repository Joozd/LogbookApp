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

package nl.joozd.joozdlogfiletypedetector

import nl.joozd.joozdlogfiletypedetector.interfaces.FileTypeDetector
import java.io.InputStream

class CsvTypeDetector(inputStream: InputStream): FileTypeDetector {
    private val lines = try{
        inputStream.reader().readLines()
    } catch (e: Exception){ null }

    override val seemsValid: Boolean
        get() = lines != null && lines.isNotEmpty()
    override val typeOfFile: SupportedTypes
        get() = getType(lines)

    override val debugData: String
        get() = lines?.firstOrNull() ?: "input is invalid or empty"


    private fun getType(lines: List<String>?): SupportedTypes = when (lines?.firstOrNull()){
        MCC_PILOT_LOG_CSV_IDENTIFIER -> SupportedTypes.MCC_PILOT_LOG_LOGBOOK
        else -> SupportedTypes.UNSUPPORTED_CSV
    }

    companion object{
        private const val MCC_PILOT_LOG_CSV_IDENTIFIER = "\"mcc_DATE\";\"Is_PREVEXP\";\"AC_IsSIM\";\"FlightNumber\";\"AF_DEP\";\"TIME_DEP\";\"TIME_DEPSCH\";\"AF_ARR\";\"TIME_ARR\";\"TIME_ARRSCH\";\"AC_MODEL\";\"AC_REG\";\"PILOT1_ID\";\"PILOT1_NAME\";\"PILOT1_PHONE\";\"PILOT1_EMAIL\";\"PILOT2_ID\";\"PILOT2_NAME\";\"PILOT2_PHONE\";\"PILOT2_EMAIL\";\"PILOT3_ID\";\"PILOT3_NAME\";\"PILOT3_PHONE\";\"PILOT3_EMAIL\";\"PILOT4_ID\";\"PILOT4_NAME\";\"PILOT4_PHONE\";\"PILOT4_EMAIL\";\"TIME_TOTAL\";\"TIME_PIC\";\"TIME_PICUS\";\"TIME_SIC\";\"TIME_DUAL\";\"TIME_INSTRUCTOR\";\"TIME_EXAMINER\";\"TIME_NIGHT\";\"TIME_RELIEF\";\"TIME_IFR\";\"TIME_ACTUAL\";\"TIME_HOOD\";\"TIME_XC\";\"PF\";\"TO_DAY\";\"TO_NIGHT\";\"LDG_DAY\";\"LDG_NIGHT\";\"AUTOLAND\";\"HOLDING\";\"LIFT\";\"INSTRUCTION\";\"REMARKS\";\"APP_1\";\"APP_2\";\"APP_3\";\"Pax\";\"DEICE\";\"FUEL\";\"FUELUSED\";\"DELAY\";\"FLIGHTLOG\";\"TIME_TO\";\"TIME_LDG\";\"TIME_AIR\""
    }
}