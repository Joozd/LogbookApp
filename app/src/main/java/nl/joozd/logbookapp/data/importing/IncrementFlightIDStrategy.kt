package nl.joozd.logbookapp.data.importing

import nl.joozd.listmerger.IDUpdatingStrategy
import nl.joozd.logbookapp.model.dataclasses.Flight

class IncrementFlightIDStrategy(private var highestTakenID: Int): IDUpdatingStrategy<Flight> {
    override fun updateIDForItem(item: Flight): Flight = item.copy(
        flightID = ++highestTakenID
    )

    override fun idNeedsUpdating(item: Flight, masterList: List<Flight>): Boolean =
        item.flightID == Flight.FLIGHT_ID_NOT_INITIALIZED
                || item.flightID in masterList.map { it.flightID }

}