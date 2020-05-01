package nl.joozd.logbookapp.data.repository.helpers

import nl.joozd.logbookapp.model.dataclasses.Flight

/**
 * will return most recent flight that is not isPlanned
 * In case no most recent not-planned flight found, it will return an empty flight with flightID -1 (that needs to be adjusted before saving)
 */
fun mostRecentCompleteFlight(flights: List<Flight>?): Flight {
    return flights?.filter{it.isSim == 0}?.maxBy { if (it.isPlanned == 0 && it.DELETEFLAG == 0) it.timeOut else 0 } ?: Flight.createEmpty()
}

