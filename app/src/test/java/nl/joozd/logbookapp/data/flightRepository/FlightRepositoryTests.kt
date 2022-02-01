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

package nl.joozd.logbookapp.data.flightRepository

import app.cash.turbine.test
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.test.TestCoroutineScheduler
import kotlinx.coroutines.test.UnconfinedTestDispatcher
import kotlinx.coroutines.test.runTest
import nl.joozd.logbookapp.data.repository.flightRepository.FlightRepository
import nl.joozd.logbookapp.data.repository.flightRepository.FlightRepositoryWithSpecializedFunctions
import nl.joozd.logbookapp.data.repository.flightRepository.FlightRepositoryWithUndo
import nl.joozd.logbookapp.data.room.FlightsTestData
import nl.joozd.logbookapp.data.room.MockDatabase
import nl.joozd.logbookapp.model.dataclasses.Flight
import nl.joozd.logbookapp.utils.DispatcherProvider
import nl.joozd.logbookapp.utils.TimestampMaker
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Before
import org.junit.Test

@ExperimentalCoroutinesApi
class FlightRepositoryTests {
    @Before
    fun setUp(){
        DispatcherProvider.switchToTestDispatchers(UnconfinedTestDispatcher(TestCoroutineScheduler()))
    }

    @After
    fun cleanUp(){
        DispatcherProvider.switchToNormalDispatchers()
    }

    @Test
    fun testFlightRepositoryFunctions(){
        val repo = FlightRepository.mock(MockDatabase())
        val repoWithUndo = FlightRepositoryWithUndo.mock(MockDatabase())

        val repoWithSpecFunctions = FlightRepositoryWithSpecializedFunctions.mock(MockDatabase())
        testFlightRepo(repo)
        testFlightRepo(repoWithUndo)

        testFlightRepo(repoWithSpecFunctions)

    }

    fun testFlightRepo(repo: FlightRepository){
        var expectedSize = 0
        runTest {
            //mock db should start empty, so we make sure:
            repo.delete(repo.getAllFlights())

            //test getAllFlightsFlow
            repo.getAllFlightsFlow().test {
                assertEquals(expectedSize, expectMostRecentItem().size)


                //test save(Flight)
                val now = TimestampMaker(mock = true).nowForSycPurposes
                repo.save(FlightsTestData.mostRecentCompletedFlight)

                //check correct number of flights (1) saved
                expectedSize++
                var savedFlights = expectMostRecentItem()
                assertEquals(expectedSize, savedFlights.size)

                val firstSavedFlight = savedFlights.first()

                //check saved flights get timestamp
                assert(firstSavedFlight.timeStamp in (now-3..now+3))
                assert(firstSavedFlight.timeStamp != FlightsTestData.mostRecentCompletedFlight.timeStamp)

                //check saved flight is same as orignial flight apart from timestamp
                assertEquals(FlightsTestData.mostRecentCompletedFlight.withTimestampOf(firstSavedFlight), firstSavedFlight)

                //check flights without ID get a new ID assigned and are saved
                repeat(3){
                    repo.save(FlightsTestData.flightWithoutID)
                    expectedSize++
                }
                savedFlights = expectMostRecentItem()
                assertEquals(expectedSize, savedFlights.size)
                assertEquals(expectedSize, savedFlights.map {it.flightID}.toSet().size)

                //test delete(Flight)
                //test delete of flight known to server
                repo.delete(firstSavedFlight)
                expectedSize--
                savedFlights = expectMostRecentItem()
                assertEquals(expectedSize, savedFlights.size)

                //test getFlightByID
                //should not find deleted flight
                assertEquals(null, repo.getFlightByID(firstSavedFlight.flightID))
                //should not find non-existing ID
                assertEquals(null, repo.getFlightByID(Int.MIN_VALUE))

                //should find a saved flight
                val f = FlightsTestData.flightWithoutID
                repo.save(f)
                expectedSize++
                val lastSavedFlightID = savedFlights.last().flightID // last saved flight was
                val found = repo.getFlightByID(lastSavedFlightID)
                assertEquals(f.withTimestampOf(found!!).copy(flightID = lastSavedFlightID), found)

                //test delete(Collection)
                val ff = repo.getAllFlights()
                repo.delete(ff)
                expectedSize = 0
                assertEquals(expectedSize, expectMostRecentItem().size)

                //test save(collection)
                repo.save(FlightsTestData.flights)
                expectedSize = FlightsTestData.flights.filter { !it.DELETEFLAG }.size
                assertEquals(expectedSize, expectMostRecentItem().size)

                //test generateAndReserveNewFlightID
                val freeID = repo.generateAndReserveNewFlightID()
                assertEquals (null, repo.getFlightByID(freeID))

                val nineThousand = 9000
                val freeIDAbove9000 = repo.generateAndReserveNewFlightID(nineThousand)
                assert (freeIDAbove9000 > nineThousand)
                assertEquals (null, repo.getFlightByID(freeIDAbove9000))
            }

            //test flightDataCacheFlow
            repo.flightDataCacheFlow().test{
                expectedSize = repo.getAllFlights().size
                assertEquals(expectedSize, expectMostRecentItem().flights.size)


                repo.save(FlightsTestData.flightWithoutID)
                expectedSize++
                assertEquals(expectedSize, expectMostRecentItem().flights.size)
            }

            //test getFLightDataCache()
            assertEquals(expectedSize, repo.getFLightDataCache().flights.size)
            repo.save(FlightsTestData.flightWithoutID)
            expectedSize++
            assertEquals(expectedSize, repo.getFLightDataCache().flights.size)
        }

    }

    private fun Flight.withTimestampOf(other: Flight) =
        copy(timeStamp = other.timeStamp)
}