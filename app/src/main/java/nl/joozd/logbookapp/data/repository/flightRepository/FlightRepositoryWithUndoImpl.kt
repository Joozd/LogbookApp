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
import kotlinx.coroutines.MainScope
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.launch
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import nl.joozd.logbookapp.data.room.JoozdlogDatabase
import nl.joozd.logbookapp.model.dataclasses.Flight
import nl.joozd.logbookapp.utils.CastFlowToMutableFlowShortcut
import nl.joozd.logbookapp.utils.InsertedUndoableCommand
import nl.joozd.logbookapp.utils.UndoableCommand
import java.util.*

class FlightRepositoryWithUndoImpl(
    mockDataBase: JoozdlogDatabase?
): FlightRepositoryWithUndo {
    private constructor(): this (null)

    private val repositoryWithDirectAccess =
        if (mockDataBase == null)FlightRepositoryWithDirectAccess.instance
        else FlightRepositoryWithDirectAccess.mock(mockDataBase)

    private val undoStack = Stack<UndoableCommand>()
    private val redoStack = Stack<UndoableCommand>()

    override val undoAvailableFlow: StateFlow<Boolean> = MutableStateFlow(false)
    override val redoAvailableFlow: StateFlow<Boolean> = MutableStateFlow(false)

    override var undoAvailable: Boolean by CastFlowToMutableFlowShortcut(undoAvailableFlow)
        private set

    override var redoAvailable: Boolean by CastFlowToMutableFlowShortcut(redoAvailableFlow)
        private set


    //override val undoAvailableFlow: Flow<Boolean> = _undoAvailable
    //override val redoAvailableFlow: Flow<Boolean> = _redoAvailable

    private val undoRedoMutex = Mutex()
    /**
     * Undo last operation
     */
    override suspend fun undo() {
        undoRedoMutex.withLock {
            val command = undoStack.pop()
            undoAvailable = !undoStack.empty()

            command.undo()
            redoStack.push(command)
            redoAvailable = true
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
                redoAvailable = !redoStack.empty()

                command()
                undoStack.push(command)
                undoAvailable = true
            }
        }
    }

    override fun insertUndoAction(undoableCommand: InsertedUndoableCommand) {
        undoStack.push(undoableCommand)
        undoAvailable = true
    }

    override fun invalidateInsertedUndoCommands() {
        undoStack.removeAll { it is InsertedUndoableCommand }
        undoAvailable = undoStack.isNotEmpty()
        redoStack.removeAll { it is InsertedUndoableCommand }
        redoAvailable = redoStack.isNotEmpty()
    }



    /**
     * Get a single flight by it's ID
     */
    override suspend fun getFlightByID(flightID: Int): Flight? =
        repositoryWithDirectAccess.getFlightByID(flightID)

    override suspend fun getFlightsByID(ids: Collection<Int>): List<Flight> =
        repositoryWithDirectAccess.getFlightsByID(ids)

    override suspend fun getFlightsStartingInEpochSecondRange(range: ClosedRange<Long>) {
        repositoryWithDirectAccess.getFlightsStartingInEpochSecondRange(range)
    }

    override fun allFlightsFlow(): Flow<List<Flight>> =
        repositoryWithDirectAccess.allFlightsFlow()

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
        redoAvailable = false // new save means any previous redo can no longer be done
    }

    /**
     * Delete a flight.
     */
    override suspend fun delete(flight: Flight) {
        delete(listOf(flight))
    }

    override suspend fun delete(flights: Collection<Flight>) {
        deleteWithUndo(flights)
        redoAvailable = false // new delete means any previous redo can no longer be done
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

    private suspend fun generateSaveFlightsAction(flightsToSave: Collection<Flight>) = suspend {
        repositoryWithDirectAccess.save(flightsToSave)
    }

    private fun generateDeleteFlightsAction(flightsToSave: Collection<Flight>) = suspend {
        repositoryWithDirectAccess.delete(flightsToSave)
    }

    /*
     * The idea is that if a flight was overwritten, it can overwrite back.
     * If there was no flight overwritten (so "null" result), no flight will be saved on undo
     */
    private fun generateUndoSaveFlightsAction(
        newFlights: Collection<Flight>,
        overwrittenFlights: Collection<Flight>
    ): () -> Unit = {
        MainScope().launch {
            repositoryWithDirectAccess.deleteHard(newFlights)
            repositoryWithDirectAccess.saveDirectToDB(overwrittenFlights) // bypasses new timestamp / ID generation
        }
    }



    private suspend fun executeUndoableCommand(command: UndoableCommand){
        undoStack.push(command)
        redoStack.clear()
        undoAvailable = true
        command()
    }

    companion object{
        val instance by lazy { FlightRepositoryWithUndoImpl() }
        fun mock(mockDataBase: JoozdlogDatabase) = FlightRepositoryWithUndoImpl(mockDataBase)
    }
}