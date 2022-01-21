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
import kotlinx.coroutines.withContext
import nl.joozd.logbookapp.model.dataclasses.Flight
import nl.joozd.logbookapp.utils.DispatcherProvider
import nl.joozd.logbookapp.utils.UndoableCommand
import java.util.*

class FlightRepositoryWithUndoImpl: FlightRepositoryWithUndo, CoroutineScope by MainScope() {
    val repositoryWithDirectAccess = FlightRepositoryWithDirectAccess.instance

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
        //Locked because this can be a write operation.
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
            saveWithUndo(flight)

            // new save means any previous redo can no longer be done
            _redoAvailable.value = false
        }
    }

    /**
     * Save a collection of Flights to DB.
     */
    override fun save(flights: Collection<Flight>) =
        TODO("TODO")

    /**
     * Delete a flight.
     */
    override fun delete(flight: Flight) =
        TODO("TODO")

    private suspend fun saveWithUndo(flightToSave: Flight){
        val saveAction = generateSaveFlightAction(flightToSave)

        //If no flight with this ID in database yet, this will be null
        val overwrittenFlight: Flight? = getFlightByID(flightToSave.flightID)
        val undoAction = generateUndoSaveFlightAction(flightToSave, overwrittenFlight)

        val command = UndoableCommand(saveAction, undoAction)
        executeUndoableCommand(command)
    }

    private fun generateSaveFlightAction(flightToSave: Flight): () -> Unit = {
        launch {
            repositoryWithDirectAccess.save(flightToSave)
        }
    }

    /*
     * The idea is that if a flight was overwritten, it can overwrite back.
     * If there was no flight overwritten (so "null" result), no flight will be saved on undo
     */
    private fun generateUndoSaveFlightAction(newFlight: Flight, overwrittenFlight: Flight?): () -> Unit =
        {
            launch {
                repositoryWithDirectAccess.deleteHard(newFlight)
                overwrittenFlight?.let {
                    withContext(DispatcherProvider.io()) {
                        repositoryWithDirectAccess.saveDirectToDB(it) // bypasses new timestamp generation
                    }
                }
            }
        }

    private fun executeUndoableCommand(command: UndoableCommand){
        undoStack.push(command)
        redoStack.clear()
        _undoAvailable.value = true
        command()
    }
}