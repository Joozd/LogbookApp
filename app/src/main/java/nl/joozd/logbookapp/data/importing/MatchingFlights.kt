package nl.joozd.logbookapp.data.importing

import nl.joozd.logbookapp.model.dataclasses.Flight

data class MatchingFlights(val knownFlight: Flight, val newFlight: Flight){
    // This assumes this is done on matching flights so only checks times, registration, type.
    fun hasChanges(): Boolean =
        knownFlight.timeOut != newFlight.timeOut
                || knownFlight.timeIn != newFlight.timeIn
                || knownFlight.registration != newFlight.registration
                || knownFlight.aircraftType != newFlight.aircraftType

}