package nl.joozd.logbookapp.data.repository.flightRepository

import nl.joozd.logbookapp.data.room.JoozdlogDatabase
import nl.joozd.logbookapp.model.dataclasses.Flight


//changes from supertype only in that it does not update timestamps on saving
class FlightRepositoryForSyncPurposesImpl(
    injectedDatabase: JoozdlogDatabase? = null
): FlightRepositoryImpl(injectedDatabase), FlightRepositoryForSyncPurposes {
    override suspend fun save(flights: Collection<Flight>) {
        saveWithID(flights)
    }

    private suspend fun saveWithID(flights: Collection<Flight>) =
        saveDirectToDB(flights.updateIDsIfNeeded())

    companion object{
        val instance by lazy { FlightRepositoryForSyncPurposesImpl() }
    }
}