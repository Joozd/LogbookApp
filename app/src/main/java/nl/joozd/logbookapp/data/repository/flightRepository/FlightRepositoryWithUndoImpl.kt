package nl.joozd.logbookapp.data.repository.flightRepository

import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import nl.joozd.logbookapp.data.room.JoozdlogDatabase
import nl.joozd.logbookapp.model.dataclasses.Flight
import nl.joozd.logbookapp.utils.CastFlowToMutableFlowShortcut
import java.util.*

// TODO: Save and delete can be optimized, using singleUndoableOperation gets all flights twice while we only need the changed flights.
class FlightRepositoryWithUndoImpl(
    mockDataBase: JoozdlogDatabase?
): FlightRepositoryWithUndo {
    private constructor(): this (null)

    private val repository =
        if (mockDataBase == null)FlightRepository.instance
        else FlightRepository.mock(mockDataBase)

    // We need to perform actions in order or undo/redo will become a mess.
    private val keepOrder = Mutex()

    private val undoStack = Stack<Map<Int, FlightState>>()
    private val redoStack = Stack<Map<Int, FlightState>>()

    override val undoAvailableFlow: StateFlow<Boolean> = MutableStateFlow(false)
    override val redoAvailableFlow: StateFlow<Boolean> = MutableStateFlow(false)

    override var undoAvailable: Boolean by CastFlowToMutableFlowShortcut(undoAvailableFlow)
        private set

    override var redoAvailable: Boolean by CastFlowToMutableFlowShortcut(redoAvailableFlow)
        private set

    private fun popUndo(): Map<Int, FlightState> = undoStack.pop().also{
        undoAvailable = undoStack.isNotEmpty()
    }

    private fun pushUndo(state: Map<Int, FlightState>){
        undoStack.push(state)
        undoAvailable = true
    }

    private fun popRedo(): Map<Int, FlightState> = redoStack.pop().also{
        redoAvailable = redoStack.isNotEmpty()
    }

    private fun pushRedo(state: Map<Int, FlightState>){
        redoStack.push(state)
        redoAvailable = true
    }

    override suspend fun undo() {
        keepOrder.withLock {
            if (undoStack.isNotEmpty()) { // this could happen if user spams undo
                // Throws EmptyStackException if nothing on stack.
                // This should not be possible since we are doing this in a mutex and just checked it wasn't empty.
                val state = popUndo()
                val redoState = recoverState(state)
                pushRedo(redoState)
            }
        }
    }

    override suspend fun redo() {
        keepOrder.withLock {
            if (redoStack.isNotEmpty()) { // this could happen if user spams undo
                println("REDO STACK: ${redoStack.size} // $redoStack")
                // Throws EmptyStackException if nothing on stack.
                // This should not be possible since we are doing this in a mutex and just checked it wasn't empty.
                val state = popRedo()
                val undoState = recoverState(state)
                pushUndo(undoState)
            }
        }
    }

    // this returns the state with the before and after values reversed, so that it can be undone by just feeding the resulting state to this function again.
    private suspend fun recoverState(state: Map<Int, FlightState>): Map<Int, FlightState> {
        val idsToRemove = state.keys.filter { state[it]!!.before == null }
        val flightsToSave = state.values.filter { it.before != null }.map { it.before!! }
        repository.deleteById(idsToRemove)
        repository.save(flightsToSave)
        return state.mapValues{
            it.value.revert()
        }
    }

    override suspend fun getFlightByID(flightID: Int): Flight? =
        repository.getFlightByID(flightID)


    override suspend fun getFlightsByID(ids: Collection<Int>): List<Flight> =
        repository.getFlightsByID(ids)

    override suspend fun getFlightsStartingInEpochSecondRange(range: ClosedRange<Long>) =
        repository.getFlightsStartingInEpochSecondRange(range)

    override fun allFlightsFlow(): Flow<List<Flight>> =
        repository.allFlightsFlow()

    override suspend fun getAllFlights(): List<Flight> =
        repository.getAllFlights()

    override fun flightDataCacheFlow(): Flow<FlightDataCache> =
        repository.flightDataCacheFlow()

    override suspend fun save(flight: Flight) =
        singleUndoableOperation {
            save(flight)
        }

    override suspend fun save(flights: Collection<Flight>) =
        singleUndoableOperation {
            save(flights)
        }

    override suspend fun delete(flight: Flight)  =
        singleUndoableOperation {
            delete(flight)
        }

    override suspend fun delete(flights: Collection<Flight>) =
        singleUndoableOperation {
            delete(flights)
        }

    override suspend fun deleteById(ids: Collection<Int>) =
        singleUndoableOperation {
            deleteById(ids)
        }

    override suspend fun deleteById(id: Int) =
        singleUndoableOperation {
            deleteById(id)
        }

    override suspend fun clear() =
        singleUndoableOperation {
            clear()
        }

    override suspend fun generateAndReserveNewFlightID(highestTakenID: Int): Int =
        repository.generateAndReserveNewFlightID(highestTakenID)


    //This can get a bit expensive, O(n2) on all flights twice.
    override suspend fun <T> singleUndoableOperation(operation: suspend FlightRepository.() -> T): T {
        val before = repository.getAllFlights()
        return repository.operation().also {
            //also block adds differences between before and after to undo stack
            //If this proves too expensive a method I could optimize but I think we should be OK
            val after = repository.getAllFlights()
            val changesBefore = before.filter { it !in after }
            val changesAfter = after.filter { it !in before }
            val changedIDs = changesBefore.map { it.flightID } + changesAfter.map { it.flightID }.distinct()
            val state = changedIDs.associateWith { id ->
                FlightState(changesBefore.firstOrNull { it.flightID == id }, changesAfter.firstOrNull { it.flightID == id })
            }
            pushUndo(state)
        }
    }

    private data class FlightState(val before: Flight?, val after: Flight?){
        fun revert() = FlightState(after, before)
    }

    companion object{
        val instance by lazy { FlightRepositoryWithUndoImpl() }
        fun mock(mockDataBase: JoozdlogDatabase) = FlightRepositoryWithUndoImpl(mockDataBase)
    }
}