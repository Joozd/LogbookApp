package nl.joozd.joozdlogcommon.utilities

import nl.joozd.joozdlogcommon.BasicFlight

/**
 * Under some circumstances, duplicate flights may appear in logbook.
 * These duplicates should be deleted hard, both locally and on server.
 * Use this function to get the flights that need deleting.
 * @return a list of BasicFlights that need deleting. These are the duplicate entries with the highest IDs.
 * NOTE this might take some time on bigger datasets, so it is suggested to run this in Dispatchers.Default
 * TODO make separate Cloud function for removing duplicates from DB as this turns out to be rather expensive to do every time.
 */
fun checkForDuplicates(flights: List<BasicFlight>): List<BasicFlight> {
    val flightsToKeep = ArrayList<BasicFlight>(flights.size)
    val duplicates = ArrayList<BasicFlight>()
    flights.sortedBy { it.flightID }.forEach{ flightToCheck ->
        if (flightsToKeep.any { it.isDuplicateOf(flightToCheck)} )
            duplicates.add(flightToCheck)
        else flightsToKeep.add(flightToCheck)
    }
    return duplicates
}

// A flight is a duplicate if everything except flightID, timestamp and changed are the same.
private fun BasicFlight.isDuplicateOf(other: BasicFlight) =
    this.copy(flightID = 0, changed = false, timeStamp = 0) == other.copy(flightID = 0, changed = false, timeStamp = 0)