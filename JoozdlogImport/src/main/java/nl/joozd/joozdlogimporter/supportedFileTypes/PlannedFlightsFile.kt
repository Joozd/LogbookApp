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

import nl.joozd.joozdlogimporter.dataclasses.ExtractedPlannedFlights
import nl.joozd.joozdlogimporter.enumclasses.AirportIdentFormat
import nl.joozd.joozdlogimporter.interfaces.PlannedFlightsExtractor

abstract class PlannedFlightsFile(lines: List<String>): ImportedFile(lines){
    /**
     * true if this file defines if flights are marked as PIC
     */
    abstract val picIsSet: Boolean

    abstract val extractor: PlannedFlightsExtractor

    /**
     * either ICAO or IATA idents are saved as airports (e.g. EHAM or AMS)
     */
    abstract val identFormat: AirportIdentFormat

    fun extractPlannedFlights(): ExtractedPlannedFlights {
        val period = extractor.getPeriodFromLines(data)
        val extractedFlights = extractor.extractFlightsFromLines(data)

        return ExtractedPlannedFlights(
            period = period,
            flights = extractedFlights,
            picIsSet = picIsSet,
            identFormat = identFormat
        )
    }
}