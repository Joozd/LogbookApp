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

import nl.joozd.logbookapp.data.AircraftTestData
import nl.joozd.logbookapp.data.room.model.toAircraft
import org.junit.Test

class AircraftTest {
    private val testAircraft = AircraftTestData.arwt1.toAircraft()
    @Test
    fun testAircraftFunctions(){
        val testPartialRegQuery = testAircraft.registration

        assert(testAircraft matches testAircraft.registration)
        assert(testAircraft matches testPartialRegQuery)
        assert(testAircraft matches testPartialRegQuery.lowercase())
        assert(testAircraft matches testPartialRegQuery.uppercase())

        val failRegQuery = "FAIL"
        assert(!(testAircraft matches failRegQuery))

        //test match aircraft type name only in matchesIncludingType
        val testTypeQuery1 = "Test Aircraft 1"
        assert(!(testAircraft matches testTypeQuery1))
        assert(testAircraft matchesIncludingType testTypeQuery1)
        assert(testAircraft matchesIncludingType testTypeQuery1.lowercase())

        //test match aircraft type short name only in matchesIncludingType
        val testTypeQuery2 = "TAC1"
        assert(!(testAircraft matches testTypeQuery2))
        assert(testAircraft matchesIncludingType testTypeQuery2)
        assert(testAircraft matchesIncludingType testTypeQuery2.lowercase())

        //test wrong queries do not match
        val failTypeQuery = "Test Aircraft 2"
        val failTypeQuery2 = "tac2"
        assert(!(testAircraft matchesIncludingType failTypeQuery))
        assert(!(testAircraft matchesIncludingType failTypeQuery2))
        assert(!(testAircraft matchesIncludingType failRegQuery))
    }
}