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

package nl.joozd.logbookapp.data.aircraftrepository


import app.cash.turbine.test
import kotlinx.coroutines.*
import kotlinx.coroutines.test.TestCoroutineScheduler
import kotlinx.coroutines.test.UnconfinedTestDispatcher
import kotlinx.coroutines.test.runTest
import nl.joozd.logbookapp.data.AircraftTestData
import nl.joozd.logbookapp.data.FlightsTestData
import nl.joozd.logbookapp.data.repository.aircraftrepository.AircraftRepository
import nl.joozd.logbookapp.data.repository.flightRepository.FlightRepository
import nl.joozd.logbookapp.data.room.MockDatabase
import nl.joozd.logbookapp.utils.DispatcherProvider
import org.junit.After

import org.junit.Assert.*
import org.junit.Before
import org.junit.Test

/**
 * Example local unit test, which will execute on the development machine (host).
 *
 * See [testing documentation](http://d.android.com/tools/testing).
 */
@ExperimentalCoroutinesApi
class AircraftRepositoryTests {
    private val mockDB = MockDatabase()
    private val mockFlightRepo = FlightRepository.mock(mockDB)
    @Before
    fun setUp(){
        DispatcherProvider.switchToTestDispatchers(UnconfinedTestDispatcher(TestCoroutineScheduler()))
        runBlocking{
            mockFlightRepo.save(FlightsTestData.flightWithAircraft)
            mockFlightRepo.save(FlightsTestData.flightWithUnknownAircraft)
        }
    }

    @After
    fun cleanUp(){
        DispatcherProvider.switchToNormalDispatchers()
    }

    @Test
    fun testAircraftRepositoryFunctions() {
        val repo = AircraftRepository.mock(mockDB, mockFlightRepo)
        var expectedSize = 0

        runTest {
            //test aircraftTypes flow, add types to types DB
            repo.aircraftTypesFlow().test {
                //should start empty
                assertEquals(0, awaitItem().size)
                repo.updateAircraftTypes(AircraftTestData.aircraftTypes)
                assertEquals(2, awaitItem().size)
                repo.updateAircraftTypes(emptyList())
                assertEquals(0, awaitItem().size)
                repo.updateAircraftTypes(AircraftTestData.aircraftTypes)
                assertEquals(2, awaitItem().size)

                cancelAndConsumeRemainingEvents()
            }

            //test aircraftMapFlow
            repo.aircraftMapFlow().test{
                assertEquals(0, awaitItem().size) // map starts empty

                //test replaceAllPreloaded
                repo.updateForcedTypes(AircraftTestData.preloadedList)
                expectedSize += AircraftTestData.preloadedList.size
                assertEquals(expectedSize, expectMostRecentItem().size)

                cancelAndConsumeRemainingEvents()
            }

            //test aircraftDataCacheFlow
            repo.aircraftDataCacheFlow().test{
                assertEquals(expectedSize, awaitItem().getRegistrationToAircraftMap().size)
                cancelAndConsumeRemainingEvents()
            }

            //test getAircraftDataCache
            val adc = repo.getAircraftDataCache()
            assertEquals(AircraftTestData.aircraftTypes.size, adc.getAircraftTypes().size)

            //TODO make test for aircraft from flights
        }
    }
}
