package nl.joozd.logbookapp.data.repository

import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.MainScope
import nl.joozd.logbookapp.workmanager.JoozdlogWorkersHub

/**
 * For doing I/O things that are not related to flights, airports, aircraft or balances forward
 */
object GeneralRepository: CoroutineScope by MainScope() {
    fun synchTimeWithServer(){
        JoozdlogWorkersHub.synchronizeTime()
    }
}