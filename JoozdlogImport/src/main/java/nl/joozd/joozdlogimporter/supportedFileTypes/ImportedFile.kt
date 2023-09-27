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

import nl.joozd.joozdlogcommon.BasicFlight

sealed class ImportedFile(protected val data: List<String>){
    /**
     * Bitwise list of supported data, like RANK (isPIC) or anything else I think of.
     * Useful to know for postprocessing.
     * Default supports nothing.
     */
    open val supportedData: Int = 0

    fun supports(data: Int) = supportedData and data != 0

    /**
     * Get flights. This function does not do any post-processing like calculating night time.
     * Unsupported files will return null
     */
    fun getFlights(): Collection<BasicFlight>? =
        when(this){
            is CompleteLogbookFile -> extractCompletedFlights().flights
            is CompletedFlightsFile -> extractCompletedFlights().flights
            is PlannedFlightsFile -> extractPlannedFlights().flights
            is UnsupportedFile -> null
    }

    companion object{
        //supoprted functions
        const val PIC = 0b1                             // Extractor sets isPic
        const val AUGMENTED = 0b10                      // extractor sets augmentedCrew
    }
}