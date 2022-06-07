package nl.joozd.logbookapp.data.repository.flightRepository

import nl.joozd.logbookapp.data.room.JoozdlogDatabase

interface FlightRepositoryForSyncPurposes: FlightRepository {
    companion object{
        //If changing concrete class, change it in mock() as well!
        val instance: FlightRepository get() = FlightRepositoryWithDirectAccess.instance

        fun mock(mockDataBase: JoozdlogDatabase): FlightRepository =
            FlightRepositoryForSyncPurposesImpl(mockDataBase)
    }
}