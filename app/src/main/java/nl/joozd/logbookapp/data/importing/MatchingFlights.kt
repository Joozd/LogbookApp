package nl.joozd.logbookapp.data.importing

import nl.joozd.logbookapp.model.dataclasses.Flight

data class MatchingFlights(val knownFlight: Flight, val newFlight: Flight){
    // This assumes this is done on matching flights so only checks times, registration, type.
    fun hasChangesforCompletedFlights(): Boolean =
        knownFlight.timeOut != newFlight.timeOut
                || knownFlight.timeIn != newFlight.timeIn
                || knownFlight.registration != newFlight.registration
                || knownFlight.aircraftType != newFlight.aircraftType

    /**
     * Check if these two flights are exactly the same, apart from ID and timestamp.
     */
    fun isExactMatch(): Boolean =
        knownFlight.copy (flightID = 0, timeStamp = 0) == newFlight.copy(flightID = 0, timeStamp = 0)
}