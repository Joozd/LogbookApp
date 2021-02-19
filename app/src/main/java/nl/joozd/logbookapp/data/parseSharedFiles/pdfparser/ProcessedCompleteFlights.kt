/*
 *  JoozdLog Pilot's Logbook
 *  Copyright (c) 2021 Joost Welle
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

import nl.joozd.logbookapp.data.parseSharedFiles.interfaces.CompletedFlights
import nl.joozd.logbookapp.data.parseSharedFiles.interfaces.Roster
import nl.joozd.logbookapp.model.dataclasses.Flight
import java.time.Instant

/**
 * Model class for processed data from any [CompletedFlights] source.
 * TODO This class can be given to repository for saving/replacing data
 */
data class ProcessedCompleteFlights(override val isValid: Boolean, override val flights: List<Flight>, override val period: ClosedRange<Instant>): CompletedFlights{
    override fun close() {
        // intentionally left blank
    }
}
