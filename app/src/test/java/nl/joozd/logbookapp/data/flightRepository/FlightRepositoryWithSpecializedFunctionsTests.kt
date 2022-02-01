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

import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.launch
import kotlinx.coroutines.runBlocking
import kotlinx.coroutines.test.TestCoroutineScheduler
import kotlinx.coroutines.test.UnconfinedTestDispatcher
import kotlinx.coroutines.test.runTest
import nl.joozd.logbookapp.data.repository.flightRepository.FlightRepositoryWithDirectAccess
import nl.joozd.logbookapp.data.repository.flightRepository.FlightRepositoryWithSpecializedFunctions
import nl.joozd.logbookapp.data.room.FlightsTestData
import nl.joozd.logbookapp.data.room.MockDatabase
import nl.joozd.logbookapp.testUtils.CoroutineTestingClass
import nl.joozd.logbookapp.utils.DispatcherProvider
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Before
import org.junit.Test

@ExperimentalCoroutinesApi
class FlightRepositoryWithSpecializedFunctionsTests: CoroutineTestingClass() {
    private val repo = FlightRepositoryWithSpecializedFunctions.mock(MockDatabase())
    private var expectedItems = 0

    @Before
    fun injectFlightsIntoDB(){
        with (repo as FlightRepositoryWithDirectAccess){
            runBlocking{
                saveDirectToDB(FlightsTestData.flights)
            }
        }
    }



    @Test
    fun testFlightRepositoryWithSpecializedFunctionsFunctions(){
        runTest {
            //test getMostRecentTimestampOfACompletedFlight
            assertEquals(FlightsTestData.mostRecentTimestampFlight.timeStamp, repo.getMostRecentTimestampOfACompletedFlight())

            //test getMostRecentCompletedFlight
            assertEquals(FlightsTestData.mostRecentCompletedFlight, repo.getMostRecentCompletedFlight())
        }
    }
}