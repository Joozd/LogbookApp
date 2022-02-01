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

package nl.joozd.logbookapp.data.airportrepository


import nl.joozd.logbookapp.data.dataclasses.Airport
import nl.joozd.logbookapp.data.repository.airportrepository.AirportDataCache
import nl.joozd.logbookapp.data.room.AirportsTestData
import org.junit.Assert.assertEquals
import org.junit.Test

class AirportDataCacheTests {
    private val adc = AirportDataCache.make(AirportsTestData.airports)
    val a = AirportsTestData.eham

    @Test
    fun testAirportDataCacheFunctions(){
        // test empty
        assertEquals(0, AirportDataCache.empty().getAirports().size)

        //test getAirports
        assertEquals(AirportsTestData.airports, adc.getAirports())

        //test icaoToIata
        assertEquals(a.iata_code, adc.icaoToIata(a.ident))
        assertEquals(null, adc.icaoToIata("XXXX"))
        assertEquals(null, adc.icaoToIata(a.iata_code))

        //test iataToIcao
        assertEquals(a.ident, adc.iataToIcao(a.iata_code))
        assertEquals(null, adc.iataToIcao("XXXX"))
        assertEquals(null, adc.iataToIcao(a.ident))

    }
}