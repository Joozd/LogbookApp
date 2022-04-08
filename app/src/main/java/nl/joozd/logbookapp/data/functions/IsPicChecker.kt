package nl.joozd.logbookapp.data.functions

import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import nl.joozd.logbookapp.data.repository.flightRepository.FlightRepositoryWithSpecializedFunctions

// snapshot only, so not a singleton
class IsPicChecker {
    //suspended lazy
    private val ispicMutex = Mutex()
    private var isPic: Boolean? = null
    suspend fun isPic(): Boolean =
        isPic
            ?: ispicMutex.withLock {
                (FlightRepositoryWithSpecializedFunctions.instance.getMostRecentCompletedFlight()?.isPIC
                    ?: false)
                    .also {
                        isPic = it
                    }
            }
}