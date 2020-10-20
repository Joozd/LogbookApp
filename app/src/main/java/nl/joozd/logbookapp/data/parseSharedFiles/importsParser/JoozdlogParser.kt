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

package nl.joozd.logbookapp.data.parseSharedFiles.importsParser

import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import nl.joozd.logbookapp.data.export.FlightsRepositoryExporter
import nl.joozd.logbookapp.data.parseSharedFiles.interfaces.ImportedLogbook
import nl.joozd.logbookapp.model.dataclasses.Flight
import java.io.InputStream

class JoozdlogParser(private val lines: List<String>): ImportedLogbook {
    override val needsCleaning = false

    private val firstLines = listOf (FlightsRepositoryExporter.FIRST_LINE_V4, FlightsRepositoryExporter.FIRST_LINE_V5)

    override val validImportedLogbook: Boolean
        get() = lines.isNotEmpty() && lines.first() in firstLines

    /**
     * List of flights
     * null means a line that failed to import but didn't break the other flights
     */
    override val flights: List<Flight?>?
        get() = FlightsRepositoryExporter.csvToFlights(lines)

    //TODO not fixing corrupt files atm
    override val errorLines: List<String>? = null

    companion object{
        suspend fun ofInputStream(inputstream: InputStream) = withContext(Dispatchers.IO) {
            JoozdlogParser(inputstream.reader().readLines())
        }
    }
}