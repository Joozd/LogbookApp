package nl.joozd.logbookapp.ui.activities.helpers

import nl.joozd.klcrosterparser.KlcRosterEvent
import nl.joozd.logbookapp.data.sharedPrefs.Preferences
import nl.joozd.logbookapp.model.dataclasses.Flight
import java.time.Instant

object PdfRosterFunctions{
    fun makeFlightsList(events: List<KlcRosterEvent>, startingID: Int, airportsMap: Map<String, String>): List<Flight> =
        events.mapIndexed { index: Int, rf: KlcRosterEvent ->
            val flightNumOrigArrowDest = rf.description.split(" ").map { it.trim() }
            val flightNumber = flightNumOrigArrowDest[0]
            val orig = flightNumOrigArrowDest[1]
            val dest = flightNumOrigArrowDest[3]

            Flight(
                flightID = startingID + index,
                orig = airportsMap[orig] ?: "XXXX",
                dest = airportsMap[dest] ?: "XXXX",
                timeOut = rf.startEpochSecond,
                timeIn = rf.endEpochSecond,
                flightNumber = flightNumber,
                timeStamp = Instant.now().epochSecond + Preferences.serverTimeOffset,
                unknownToServer = 1
            )
        }
}
