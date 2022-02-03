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

package nl.joozd.logbookapp.data.dataclasses

import nl.joozd.logbookapp.data.AirportsTestData
import org.junit.Assert.assertEquals
import org.junit.Test

class AirportTest {
    private val ap = AirportsTestData.eham

    @Test
    fun testAirportFunctions(){
        //test converting to and from BasicAirport
        assertEquals(ap, Airport(ap.toBasicAirport()))

        //test identMatches()
        val failedQuery = "FAIL THIS QUERY 1234567890987654321"
        val name = ap.name
        val muni = ap.municipality

        assert(!(ap identMatches failedQuery))
        assert(!(ap identMatches name))
        assert(!(ap identMatches muni))

        val identQuery = ap.ident
        assert((ap identMatches identQuery))
        assert((ap identMatches identQuery.uppercase()))
        assert((ap identMatches identQuery.lowercase()))

        val identQueryPart = ap.ident.takeLast(2)
        assert((ap identMatches identQueryPart))
        assert((ap identMatches identQueryPart.uppercase()))
        assert((ap identMatches identQueryPart.lowercase()))

        //test matches()
        assert(!(ap matches failedQuery))

        val partialName = name.take(3)
        assert(ap matches name)
        assert(ap matches partialName)
        assert(ap matches partialName.uppercase())
        assert(ap matches partialName.lowercase())

        val partialMuni = muni.take(3)
        assert(ap matches muni)
        assert(ap matches partialMuni)
        assert(ap matches partialMuni.uppercase())
        assert(ap matches partialMuni.lowercase())
    }
}