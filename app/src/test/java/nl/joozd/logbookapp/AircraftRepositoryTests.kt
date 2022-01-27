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

package nl.joozd.logbookapp


import kotlinx.coroutines.*
import kotlinx.coroutines.test.TestCoroutineScheduler
import kotlinx.coroutines.test.UnconfinedTestDispatcher
import kotlinx.coroutines.test.runTest
import nl.joozd.joozdlogcommon.AircraftType
import nl.joozd.logbookapp.data.repository.aircraftrepository.AircraftRepository
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

    @Before
    fun setUp(){
        DispatcherProvider.switchToTestDispatchers(UnconfinedTestDispatcher(TestCoroutineScheduler()))
    }

    @After
    fun cleanUp(){
        DispatcherProvider.switchToNormalDispatchers()
    }

    @Test
    fun test() {
        runTest {
            var currentTypesList: List<AircraftType> = emptyList()
            val aircraftRepository = AircraftRepository.mock(MockDatabase())
                // DispatcherProvider.default() provides UnconfinedTestDispatcher(TestCoroutineScheduler()) for my test.
            launch(DispatcherProvider.default()) {
                aircraftRepository.aircraftTypesFlow.collect {
                    println("emitted ${it.size} flights: $it")
                    currentTypesList = it
                }
            }
            aircraftRepository.replaceAllTypesWith(TestData.aircraftTypes)
            delay(500)
            println("Done waiting")
            assertEquals (2, currentTypesList.size)
        }
    }
}
