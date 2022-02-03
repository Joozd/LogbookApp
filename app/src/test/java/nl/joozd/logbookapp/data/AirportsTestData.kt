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

package nl.joozd.logbookapp.data

import nl.joozd.logbookapp.data.dataclasses.Airport

object AirportsTestData {
    val eham = Airport(1, "EHAM", "big airport", "Schiphol", 10.0, 20.0, -20, "Amsterdam", "AMS")
    val ebbr = Airport(3, "EBBR", "big airport", "Zaventem", 12.3, 45.6, 200, "Brussels", "BRU")
    val ehhv = Airport(17, "EHHV", "smol airport", "Hilversum Airfield", 1.0, 2.0, 0, "Hilversum", "")

    val airports = listOf(eham, ebbr, ehhv)
}