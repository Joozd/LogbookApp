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
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.test.TestDispatcher
import kotlinx.coroutines.test.runTest
import nl.joozd.logbookapp.data.repository.flightRepository.FlightRepositoryWithUndo
import nl.joozd.logbookapp.data.room.FlightsTestData
import nl.joozd.logbookapp.data.room.MockDatabase
import nl.joozd.logbookapp.testUtils.CoroutineTestingClass
import nl.joozd.logbookapp.utils.DispatcherProvider
import org.junit.Assert.assertEquals
import org.junit.Test

@ExperimentalCoroutinesApi
class FlightRepositoryWithUndoTest: CoroutineTestingClass() {
    @Test
    fun testFlightRepositoryWithUndoSaveFunctions(){
        val repo = FlightRepositoryWithUndo.mock(MockDatabase())
        //combine flows so we can check both in a single Turbine
        val combinedUndoRedoFlow = combine(repo.undoAvailable, repo.redoAvailable){ u, r ->
            u to r
        }
        var expectedSize = 0

        runTest {
            //test undoAvailable and redoAvailable flows
            //This also tests if undo and redo cause redo and undo events to be made
            combinedUndoRedoFlow.test {
                assertEquals(Pair(false, false), expectMostRecentItem())
                assert(DispatcherProvider.main() is TestDispatcher)
                //test save
                repeat(2) {
                    repo.save(FlightsTestData.flightWithoutID)
                    expectedSize++
                }
                // should have 2 undo and 0 redo items
                val savedFlights = repo.getAllFlights()
                assertEquals(expectedSize, repo.getAllFlights().size)
                assertEquals(Pair(true, false), expectMostRecentItem())

                //test undo
                repo.undo() // should have 1 undo and 1 redo items
                expectedSize--
                println(repo.getAllFlights().joinToString("\n"))
                assertEquals(expectedSize, repo.getAllFlights().size)
                assertEquals(Pair(true, true), expectMostRecentItem())

                repo.undo() // should have 0 undo and 2 redo items
                expectedSize--
                assertEquals(expectedSize, repo.getAllFlights().size)
                assertEquals(Pair(false, true), expectMostRecentItem())

                //test redo
                repo.redo() // should have 1 undo and 1 redo items
                expectedSize++
                assertEquals(expectedSize, repo.getAllFlights().size)
                assertEquals(Pair(true, true), expectMostRecentItem())

                repo.redo() // should have 2 undo and 0 redo items
                expectedSize++
                assertEquals(expectedSize, repo.getAllFlights().size)
                assertEquals(Pair(true, false), expectMostRecentItem())

                assertEquals(savedFlights, repo.getAllFlights())

                cancelAndConsumeRemainingEvents()
            }
        }
    }
    @Test
    fun testFlightRepositoryWithUndoDeleteFunctions() {
        val repo = FlightRepositoryWithUndo.mock(MockDatabase())
        //combine flows so we can check both in a single Turbine
        val combinedUndoRedoFlow = combine(repo.undoAvailable, repo.redoAvailable) { u, r ->
            u to r
        }
        var expectedSize = 0

        runTest {
            //test undoAvailable and redoAvailable flows
            //This also tests if undo and redo cause redo and undo events to be made
            combinedUndoRedoFlow.test {
                assertEquals(Pair(false, false), expectMostRecentItem())

                val fl = MutableList(3) { FlightsTestData.flightWithoutID }
                repo.save(fl)
                expectedSize += 3
                assertEquals(expectedSize, repo.getAllFlights().size)
                val savedFlights = repo.getAllFlights()
                repeat(3){
                    repo.delete(savedFlights[it])
                    expectedSize--
                }
                assertEquals(expectedSize, repo.getAllFlights().size)
                // should have 4 undo and 0 redo items
                assertEquals(Pair(true, false), expectMostRecentItem())

                repo.undo() // should have 3 undo and 1 redo items
                expectedSize++
                assertEquals(Pair(true, true), expectMostRecentItem())
                assertEquals(expectedSize, repo.getAllFlights().size)

                repeat(2) {
                    repo.undo()
                    expectedSize++
                    assertEquals(expectedSize, repo.getAllFlights().size)
                }// should have 1 undo and 3 redo items

                repo.undo() // undo initial save
                expectedSize -= 3
                assertEquals(expectedSize, repo.getAllFlights().size)
                assertEquals(Pair(false, true), expectMostRecentItem())

                repo.redo()
                expectedSize += 3
                assertEquals(expectedSize, repo.getAllFlights().size)
                assertEquals(Pair(true, true), expectMostRecentItem())

                assertEquals(savedFlights, repo.getAllFlights())

                //redo deletes
                repeat(3){
                    repo.redo()
                    expectedSize--
                    assertEquals(expectedSize, repo.getAllFlights().size)
                }
                assertEquals(Pair(true, false), expectMostRecentItem())

                //should not have remaining events
            }
        }
    }
}