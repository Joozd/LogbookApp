package nl.joozd.logbookapp.data.repository.helpers

import kotlinx.coroutines.*
import nl.joozd.joozdlogcommon.AircraftType
import nl.joozd.logbookapp.data.room.model.AircraftRegistrationWithTypeData

/**
 * This is one specific Aircraft. It has a [registration], a [type], and a [source] for that type
 */
data class Aircraft(val registration: String, val type: AircraftType? = null, val source: Int = NONE): CoroutineScope by MainScope() {
    var aircraftRegistrationWithTypeData: AircraftRegistrationWithTypeData? = null

    /**
     * Finds aircraft from [registration] or from [flightData]
     * @return AircraftType
     * priority:
     * 1: Saved aircraft
     * 2: Type in flight
     * 3: Consensus
     * 4: null
     */

    companion object{
        const val NONE = 0
        const val KNOWN = 1
        const val FLIGHT= 2 // works only if setFromFlight
        const val CONSENSUS = 3
        const val PREVIOUS = 4
        const val MANUAL = 5
    }
}