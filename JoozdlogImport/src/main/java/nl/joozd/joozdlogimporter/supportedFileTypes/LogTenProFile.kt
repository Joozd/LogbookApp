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
import nl.joozd.joozdlogimporter.supportedFileTypes.extractors.LogtenProExtractor

class LogTenProFile(lines: List<String>): CompleteLogbookFile(lines) {
    override val extractor: CompleteLogbookExtractor
        get() = LogtenProExtractor()

    override val identFormat = AirportIdentFormat.IATA

    companion object {
        fun buildIfMatches(lines: List<String>): LogTenProFile? =
            lines.firstOrNull()?.let{ line ->
                if (LogtenProExtractor.USED_KEYS.all { key -> key in line })
                    LogTenProFile(lines)
                else
                    null
            }
    }
}