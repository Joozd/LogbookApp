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
import kotlinx.coroutines.MainScope
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.launch
import nl.joozd.logbookapp.model.dataclasses.Flight
import nl.joozd.logbookapp.utils.UndoableCommand
import java.util.*

class FlightRepositoryWithUndoImpl: FlightRepositoryWithUndo, CoroutineScope by MainScope() {
    private val repositoryWithDirectAccess = FlightRepositoryWithDirectAccess.instance

    private val undoStack = Stack<UndoableCommand>()
    private val redoStack = Stack<UndoableCommand>()

    private val _undoAvailable = MutableStateFlow(false)
    private val _redoAvailable = MutableStateFlow(false)

    override val undoAvailable: Flow<Boolean> = _undoAvailable
    override val redoAvailable: Flow<Boolean> = _redoAvailable

    /**
     * Undo last operation
     */
    override fun undo() {
        launch {
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
    override fun redo() {
        launch {
            if (redoStack.empty())
                Log.e(this::class.simpleName,"Trying to redo but redo stack is empty")
            else{
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


    /**
     * Get all flights (including deleted ones)
     * For only usable flights, use [FlightDataCache.flights]
     */
    override suspend fun getAllFlightsInDB(): List<Flight> =
        repositoryWithDirectAccess.getAllFlightsInDB()

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
    override fun save(flight: Flight) {
        launch {
            saveWithUndo(listOf(flight))
            _redoAvailable.value = false // new save means any previous redo can no longer be done
        }
    }

    /**
     * Save a collection of Flights to DB.
     */
    override fun save(flights: Collection<Flight>) {
        launch {
            saveWithUndo(flights)
            _redoAvailable.value = false // new save means any previous redo can no longer be done
        }
    }

    /**
     * Delete a flight.
     */
    override fun delete(flight: Flight) {
        launch {
            deleteWithUndo(listOf(flight))
            _redoAvailable.value = false // new delete means any previous redo can no longer be done
        }
    }

    override fun delete(flights: Collection<Flight>) {
        launch {
            deleteWithUndo(flights)
            _redoAvailable.value = false // new delete means any previous redo can no longer be done
        }
    }

    private suspend fun saveWithUndo(flightsToSave: Collection<Flight>){
        val saveAction = generateSaveFlightsAction(flightsToSave)

        val ids = flightsToSave.map { it.flightID }
        val overwrittenFlights: List<Flight> = getFlightsByID(ids)
        val undoAction = generateUndoSaveFlightsAction(flightsToSave, overwrittenFlights)

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
            repositoryWithDirectAccess.saveDirectToDB(overwrittenFlights) // bypasses new timestamp generation
        }
    }



    private fun executeUndoableCommand(command: UndoableCommand){
        undoStack.push(command)
        redoStack.clear()
        _undoAvailable.value = true
        command()
    }
}