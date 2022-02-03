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

package nl.joozd.logbookapp.model

import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.runBlocking
import kotlinx.coroutines.test.runTest
import nl.joozd.logbookapp.data.AircraftTestData
import nl.joozd.logbookapp.data.AirportsTestData
import nl.joozd.logbookapp.data.FlightsTestData
import nl.joozd.logbookapp.data.dataclasses.Aircraft
import nl.joozd.logbookapp.data.dataclasses.Airport
import nl.joozd.logbookapp.data.repository.aircraftrepository.AircraftRepository
import nl.joozd.logbookapp.data.repository.airportrepository.AirportRepository
import nl.joozd.logbookapp.data.room.MockDatabase
import nl.joozd.logbookapp.data.room.model.toAircraft
import nl.joozd.logbookapp.model.dataclasses.Flight
import nl.joozd.logbookapp.testUtils.CoroutineTestingClass
import org.junit.Assert.assertEquals
import org.junit.Before
import org.junit.Test

@ExperimentalCoroutinesApi
class ModelFlightTest: CoroutineTestingClass() {
    private val airportRepo = AirportRepository.mock(MockDatabase())
    private val aircraftRepo = AircraftRepository.mock(MockDatabase())
    private val flightWithMatchingAircraftAndAirportsInOtherSets =
        FlightsTestData.prototypeFlight.copy(
            flightID = Flight.FLIGHT_ID_NOT_INITIALIZED,
            orig = AirportsTestData.eham.ident,
            dest = AirportsTestData.ebbr.ident,
            registration = AircraftTestData.arwt1.registration,
            aircraftType = AircraftTestData.arwt1.type.shortName,
            timeStamp = 1000,
            unknownToServer = false
        )

    @Before
    fun fillDatabases(){
        runBlocking {
            aircraftRepo.replaceAllTypesWith(AircraftTestData.aircraftTypes)
            aircraftRepo.saveAircraft(AircraftTestData.arwt1.toAircraft())
            airportRepo.replaceDbWith(AirportsTestData.airports)
        }
    }

    @Test
    fun testModelFlightCreation(){
        runTest{
            //test ofFlightAndRepositories() equals ofDataCaches()
            val mf1 = ModelFlight.ofFlightAndRepositories(
                flightWithMatchingAircraftAndAirportsInOtherSets,
                aircraftRepository = aircraftRepo,
                airportRepository = airportRepo
            )
            val mf2 = ModelFlight.ofFlightAndDataCaches(
                flightWithMatchingAircraftAndAirportsInOtherSets,
                aircraftDataCache = aircraftRepo.getAircraftDataCache(),
                airportDataCache = airportRepo.getAirportDataCache()
            )

            val mfWithUnknownOrig = ModelFlight.ofFlightAndDataCaches(
                FlightsTestData.mostRecentTimestampFlight,
                aircraftDataCache = aircraftRepo.getAircraftDataCache(),
                airportDataCache = airportRepo.getAirportDataCache()
            )

            assertEquals(mf1, mf2)

            //test created flight is not an empty flight
            assert(mf1 != ModelFlight.createEmpty())

            assertEquals(AirportsTestData.eham, mf1.orig)
            assertEquals(AirportsTestData.ebbr, mf1.dest)
            assertEquals(AircraftTestData.arwt1.toAircraft().copy(source = Aircraft.NONE), mf1.aircraft.copy(source = Aircraft.NONE))

            //check unknown airport generates a blank airport with ICAO ident
            assertEquals(Airport(ident = FlightsTestData.mostRecentTimestampFlight.orig), mfWithUnknownOrig.orig)

        }
    }
}