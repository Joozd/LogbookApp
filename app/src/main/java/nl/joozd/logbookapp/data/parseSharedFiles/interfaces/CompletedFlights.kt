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

package nl.joozd.logbookapp.data.parseSharedFiles.interfaces

import nl.joozd.logbookapp.data.parseSharedFiles.pdfparser.ProcessedCompleteFlights
import nl.joozd.logbookapp.model.dataclasses.Flight
import java.io.Closeable
import java.time.Instant

interface CompletedFlights: Closeable {
    /**
     * true if this seems to be a valid monthly overview (gross error check)
     */
    val isValid: Boolean

    val isInvalid
        get() = !isValid

    /**
     * List of flights in this Monthly Overview (to be cleaned)
     */
    val flights: List<Flight>

    /**
     * Period that this Monthly Overview applies to
     */
    val period: ClosedRange<Instant>

    fun toProcessedCompletedFlights(): ProcessedCompleteFlights = ProcessedCompleteFlights(isValid, flights, period)
}