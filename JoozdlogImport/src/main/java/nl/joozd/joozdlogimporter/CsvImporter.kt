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

package nl.joozd.joozdlogimporter

import nl.joozd.joozdlogimporter.interfaces.FileImporter
import nl.joozd.joozdlogimporter.supportedFileTypes.*
import java.io.InputStream

/**
 * Detects type in csv and txt files
 */
class CsvImporter(private val inputStream: InputStream): FileImporter() {
    private fun lines() = inputStream.reader().readLines()

    override fun getFile() = getType(lines())

    private fun getType(lines: List<String>): ImportedFile =
        MccPilotLogFile.buildIfMatches(lines)
            ?: LogTenProFile.buildIfMatches(lines)
            ?: JoozdLogV4File.buildIfMatches(lines)
            ?: JoozdLogV5File.buildIfMatches(lines)
            ?: UnsupportedCsvFile(lines)
}