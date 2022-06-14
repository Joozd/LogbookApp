package nl.joozd.logbookapp.core.background

import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.launch
import nl.joozd.logbookapp.core.TaskFlags
import nl.joozd.logbookapp.data.sharedPrefs.DataVersions
import nl.joozd.logbookapp.data.sharedPrefs.ServerPrefs
import java.time.Instant

class SyncCenter(private val taskFlags: TaskFlags = TaskFlags) {
    fun syncDataFiles(){
        taskFlags.syncDataFiles(true)
    }

    suspend fun syncDataFilesIfNotJustDone(){
        if (Instant.now().epochSecond - DataVersions.mostRecentSyncEpochSecond() > TIME_BETWEEN_SYNC_CHECKS)
            syncDataFiles()
    }

    fun syncFlights(){
        taskFlags.syncFlights(true)
    }

    suspend fun syncFlightsIfNotJustDone(){
        if (Instant.now().epochSecond - ServerPrefs.mostRecentFlightsSyncEpochSecond() > TIME_BETWEEN_SYNC_CHECKS)
            syncFlights()
    }

    suspend fun syncAllIfNotJustDone(){
        syncDataFilesIfNotJustDone()
        syncFlightsIfNotJustDone()
    }

    fun launchSyncAllIfNotJustDone(scope: CoroutineScope){
        scope.launch {
            syncAllIfNotJustDone()
        }
    }

    companion object{
        private const val TIME_BETWEEN_SYNC_CHECKS: Long = 15*60L // 15 minutes * 60 seconds per minute
    }
}