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

package nl.joozd.logbookapp.data.repository.flightRepository

import android.util.Log
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.launch
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import nl.joozd.logbookapp.data.room.JoozdlogDatabase
import nl.joozd.logbookapp.model.dataclasses.Flight
import nl.joozd.logbookapp.utils.UndoableCommand
import nl.joozd.logbookapp.utils.delegates.dispatchersProviderMainScope
import java.util.*

class FlightRepositoryWithUndoImpl(mockDataBase: JoozdlogDatabase?
): FlightRepositoryWithUndo, CoroutineScope by dispatchersProviderMainScope() {
    private constructor(): this (null)

    private val repositoryWithDirectAccess =
        if (mockDataBase == null)FlightRepositoryWithDirectAccess.instance
        else FlightRepositoryWithDirectAccess.mock(mockDataBase)

    private val undoStack = Stack<UndoableCommand>()
    private val redoStack = Stack<UndoableCommand>()

    private val _undoAvailable = MutableStateFlow(false)
    private val _redoAvailable = MutableStateFlow(false)

    override val undoAvailable: Flow<Boolean> = _undoAvailable
    override val redoAvailable: Flow<Boolean> = _redoAvailable

    private val undoRedoMutex = Mutex()
    /**
     * Undo last operation
     */
    override suspend fun undo() {
        undoRedoMutex.withLock {
            val command = undoStack.pop()
            _undoAvailable.value = !undoStack.empty()

            command.undo()
            redoStack.push(command)
            _redoAvailable.value = true
        }
    }


    /**
     * Redo last operation
     */
    override suspend fun redo() {
        undoRedoMutex.withLock {
            if (redoStack.empty())
                Log.e(this::class.simpleName, "Trying to redo but redo stack is empty")
            else {
                val command = redoStack.pop()
                _redoAvailable.value = !redoStack.empty()

                command()
                undoStack.push(command)
                _undoAvailable.value = true
            }
        }
    }



    /**
     * Get a single flight by it's ID
     */
    override suspend fun getFlightByID(flightID: Int): Flight? =
        repositoryWithDirectAccess.getFlightByID(flightID)

    override suspend fun getFlightsByID(ids: Collection<Int>): List<Flight> =
        repositoryWithDirectAccess.getFlightsByID(ids)

    override fun getAllFlightsFlow(): Flow<List<Flight>> =
        repositoryWithDirectAccess.getAllFlightsFlow()

    override suspend fun getAllFlights(): List<Flight> =
        repositoryWithDirectAccess.getAllFlights()

    /**
     * make a [FlightDataCache] with snapshot flight data
     */
    override suspend fun getFLightDataCache(): FlightDataCache =
        repositoryWithDirectAccess.getFLightDataCache()

    /**
     * Get a flow of updated FlightDataCaches
     */
    override fun flightDataCacheFlow(): Flow<FlightDataCache> =
        repositoryWithDirectAccess.flightDataCacheFlow()

    /**
     * Save a flight to DB.
     */
    override suspend fun save(flight: Flight) {
        save(listOf(flight))
    }

    /**
     * Save a collection of Flights to DB.
     */
    override suspend fun save(flights: Collection<Flight>) {
        saveWithUndo(flights)
        _redoAvailable.value = false // new save means any previous redo can no longer be done
    }

    /**
     * Delete a flight.
     */
    override suspend fun delete(flight: Flight) {
        delete(listOf(flight))
    }

    override suspend fun delete(flights: Collection<Flight>) {
        deleteWithUndo(flights)
        _redoAvailable.value = false // new delete means any previous redo can no longer be done
    }

    override suspend fun generateAndReserveNewFlightID(highestTakenID: Int): Int =
        repositoryWithDirectAccess.generateAndReserveNewFlightID(highestTakenID)


    private suspend fun saveWithUndo(flightsToSave: Collection<Flight>){
        val flightsWithIDs = updateFlightIDs(flightsToSave)
        val saveAction = generateSaveFlightsAction(flightsWithIDs)

        val ids = flightsToSave.map { it.flightID }
        val overwrittenFlights: List<Flight> = getFlightsByID(ids)
        val undoAction = generateUndoSaveFlightsAction(flightsWithIDs, overwrittenFlights)

        val command = UndoableCommand(saveAction, undoAction)
        executeUndoableCommand(command)
    }

    private suspend fun deleteWithUndo(flightsToDelete: Collection<Flight>){
        val deleteAction = generateDeleteFlightsAction(flightsToDelete)

        // get flights from DB so we will restore original flights,
        // as delete will only look at flightID
        val ids = flightsToDelete.map { it.flightID }
        val originalFlights = getFlightsByID(ids)
        val undoAction = generateSaveFlightsAction(originalFlights)

        val command = UndoableCommand(deleteAction, undoAction)
        executeUndoableCommand(command)
    }

    private suspend fun updateFlightIDs(
        flightsToSave: Collection<Flight>
    ): List<Flight> {
        val highestIdInCollection = flightsToSave.maxOfOrNull { it.flightID } ?: 0
        return flightsToSave.map {
            if (it.flightID == Flight.FLIGHT_ID_NOT_INITIALIZED)
                it.copy(flightID = generateAndReserveNewFlightID(highestIdInCollection))
            else it
        }
    }

    private fun generateSaveFlightsAction(flightsToSave: Collection<Flight>): () -> Unit = {
        launch {
            repositoryWithDirectAccess.save(flightsToSave)
        }
    }

    private fun generateDeleteFlightsAction(flightsToSave: Collection<Flight>): () -> Unit = {
        launch {
            repositoryWithDirectAccess.delete(flightsToSave)
        }
    }

    /*
     * The idea is that if a flight was overwritten, it can overwrite back.
     * If there was no flight overwritten (so "null" result), no flight will be saved on undo
     */
    private fun generateUndoSaveFlightsAction(
        newFlights: Collection<Flight>,
        overwrittenFlights: Collection<Flight>
    ): () -> Unit = {
        launch {
            repositoryWithDirectAccess.deleteHard(newFlights)
            repositoryWithDirectAccess.saveDirectToDB(overwrittenFlights) // bypasses new timestamp / ID generation
        }
    }



    private fun executeUndoableCommand(command: UndoableCommand){
        undoStack.push(command).also{ println("pushed $it, size now ${undoStack.size}")}
        redoStack.clear()
        _undoAvailable.value = true
        command()
    }

    companion object{
        val instance by lazy { FlightRepositoryWithUndoImpl() }
        fun mock(mockDataBase: JoozdlogDatabase) = FlightRepositoryWithUndoImpl(mockDataBase)
    }
}