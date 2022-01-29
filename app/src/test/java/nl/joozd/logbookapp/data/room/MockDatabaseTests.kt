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

package nl.joozd.logbookapp.data.room

import app.cash.turbine.test
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.test.TestCoroutineScheduler
import kotlinx.coroutines.test.UnconfinedTestDispatcher
import kotlinx.coroutines.test.runTest
import nl.joozd.logbookapp.AircraftTestData
import nl.joozd.logbookapp.data.room.model.toAircraftRegistrationWithType
import nl.joozd.logbookapp.data.room.model.toAircraftType
import nl.joozd.logbookapp.data.room.model.toData
import nl.joozd.logbookapp.data.room.model.toFlight
import nl.joozd.logbookapp.utils.DispatcherProvider
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Before
import org.junit.Test

@ExperimentalCoroutinesApi
class MockDatabaseTests {
    private val db = MockDatabase()
    @Before
    fun setUp(){
        DispatcherProvider.switchToTestDispatchers(UnconfinedTestDispatcher(TestCoroutineScheduler()))    }

    @After
    fun cleanUp(){
        DispatcherProvider.switchToNormalDispatchers()
    }

    @Test
    fun testMockFlightDao(){
        val dao = db.flightDao()
        runTest {
            // test validFlightsFlow
            dao.validFlightsFlow().test {
                // mock DAO starts empty
                assert(awaitItem().isEmpty())

                //test save(collection) + getAllFlights
                dao.save(FlightsTestData.flights.map { it.toData() })

                // should not emit item with DELETEFLAG
                assertEquals(4, awaitItem().size )

                // 4 flights should be saved
                assertEquals (5, dao.getAllFlights().size)

                //getValidFlights
                assertEquals(4, dao.getValidFlights().size)

                //getFlightByID
                val f1 = FlightsTestData.flight1
                assertEquals(f1, dao.getFlightById(f1.flightID)?.toFlight())

                //getFlightsByID
                assertEquals(2, dao.getFlightsByID((1..2).toList()).size)
                assertEquals(3, dao.getFlightsByID((1..5).toList()).size)

                //highestUsedID
                assertEquals(8, dao.highestUsedID())

                //getMostRecentCompleted
                assertEquals(f1, dao.getMostRecentCompleted()?.toFlight())

                //getMostRecentTimestampOfACompletedFlight
                assertEquals(4000L, dao.getMostRecentTimestampOfACompletedFlight())

                cancelAndConsumeRemainingEvents()
            }
            println("testMockFlightDao OK")
        }
    }

    @Test
    fun testMockAirportDao(){
        val dao = db.airportDao()
        runTest {
            dao.airportsFlow().test {
                val ap1 = AirportsTestData.airport1
                val ap2 = AirportsTestData.airport2
                //mock dao starts empty
                assert(awaitItem().isEmpty())

                //test save
                dao.save(AirportsTestData.airports)
                assertEquals(3, awaitItem().size)

                //test requestAllAirports
                assertEquals(3, dao.requestAllAirports().size)

                //test searchAirportByIdent
                assertEquals(ap1, dao.searchAirportByIdent("EHAM"))
                assertEquals(ap2, dao.searchAirportByIdent("ebbr"))

                //test clearDB
                dao.clearDb()
                assertEquals(0, awaitItem().size)

                cancelAndConsumeRemainingEvents()
                println("testMockAirportDao OK")
            }

        }
    }

    @Test
    fun testMockAircraftTypeDao(){
        val dao = db.aircraftTypeDao()
        runTest{
            val type = AircraftTestData.aircraftType1
            // mock DAO starts empty
            assert(dao.requestAllAircraftTypes().isEmpty())

            //test aircraftTypesFlow
            dao.aircraftTypesFlow().test {
                assert(awaitItem().isEmpty())
                //test save + requestAllAircraftTypes
                dao.save(*AircraftTestData.aircraftTypes.map { it.toData()}.toTypedArray())
                val savedItems = dao.requestAllAircraftTypes().map { it.toAircraftType() }
                assertEquals(savedItems, AircraftTestData.aircraftTypes)
                //test flow emission
                assertEquals(2, awaitItem().size)

                //test getAircraftType
                assertEquals(AircraftTestData.aircraftType1, dao.getAircraftType(type.name)?.toAircraftType())

                //test getAircraftTypeFromShortName
                assertEquals(AircraftTestData.aircraftType1, dao.getAircraftTypeFromShortName(type.shortName)?.toAircraftType())

                //test clearDB
                dao.clearDb()
                assertEquals(0, awaitItem().size)

                cancelAndConsumeRemainingEvents()
                println("testMockAircraftTypeDao OK")
            }
        }
    }

    @Test
    fun testMockRegistrationDao(){
        val dao = db.registrationDao()
        runTest {
            //test flow
            dao.allRegistrationsFlow().test{
                // mock DAO starts empty
                assert(awaitItem().isEmpty())

                //test save
                dao.save(AircraftTestData.arwt1.toData())
                assertEquals(1, awaitItem().size)
                dao.save(AircraftTestData.arwt2.toData())
                assertEquals(2, awaitItem().size)

                //test requestAllRegistrations
                assertEquals(AircraftTestData.regsWithTypes, dao.requestAllRegistrations().map { it.toAircraftRegistrationWithType() })

                //test save overwrite
                dao.save(AircraftTestData.updatedArwt1.toData())
                assertEquals(2, awaitItem().size)

                cancelAndConsumeRemainingEvents()
                println("testMockRegistrationDao OK")
            }
        }
    }


}