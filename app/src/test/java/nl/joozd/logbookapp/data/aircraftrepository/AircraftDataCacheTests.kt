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

import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.runBlocking
import kotlinx.coroutines.test.TestCoroutineScheduler
import kotlinx.coroutines.test.UnconfinedTestDispatcher
import kotlinx.coroutines.test.runTest
import nl.joozd.logbookapp.data.AircraftTestData
import nl.joozd.logbookapp.data.repository.aircraftrepository.AircraftRepository
import nl.joozd.logbookapp.data.room.MockDatabase
import nl.joozd.logbookapp.data.room.model.toAircraft
import nl.joozd.logbookapp.utils.DispatcherProvider
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Before
import org.junit.Test

@ExperimentalCoroutinesApi
class AircraftDataCacheTests {
    private val repo = AircraftRepository.mock(MockDatabase())

    //Fill repo with some flights and types
    @Before
    fun setup(){
        DispatcherProvider.switchToTestDispatchers(
            UnconfinedTestDispatcher(TestCoroutineScheduler())
        )

        runBlocking {
            repo.replaceAllTypesWith(AircraftTestData.aircraftTypes)
            repo.replaceAllPreloadedWith(AircraftTestData.preloadedList)
            val ac = AircraftTestData.regsWithTypes.first().toAircraft()
            repo.saveAircraft(ac)
        }
    }

    @After
    fun cleanUp(){
        DispatcherProvider.switchToNormalDispatchers()
    }

    @Test
    fun testAircraftDataCache(){
        runTest{
            with(repo.getAircraftDataCache()) {
                // test getAircraftTypes()
                assertEquals(AircraftTestData.aircraftTypes, getAircraftTypes())

                //test getRegistrationToAircraftMap()
                assertEquals(repo.registrationToAircraftMap(), getRegistrationToAircraftMap())

                //test getAircraftFromRegistration()
                val regToFind1 = AircraftTestData.preloaded1.registration
                val regToFind2 = AircraftTestData.arwt1.registration
                assertEquals(repo.getAircraftFromRegistration(regToFind1), getAircraftFromRegistration(regToFind1))
                assertEquals(repo.getAircraftFromRegistration(regToFind2), getAircraftFromRegistration(regToFind2))

                //test getAircraftTypeByShortName
                val t = AircraftTestData.aircraftType2
                assertEquals(t, getAircraftTypeByShortName(t.shortName))
            }
            println("testAircraftDataCache Done")
        }

    }
}