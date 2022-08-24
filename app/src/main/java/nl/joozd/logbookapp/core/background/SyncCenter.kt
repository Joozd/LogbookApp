package nl.joozd.logbookapp.core.background

import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.launch
import nl.joozd.logbookapp.core.TaskFlags
import nl.joozd.logbookapp.data.sharedPrefs.DataVersions
import nl.joozd.logbookapp.data.sharedPrefs.ServerPrefs
import java.time.Instant

class SyncCenter private constructor(private val taskFlags: TaskFlags = TaskFlags) {
    fun syncDataFiles(){
        taskFlags.syncDataFiles(true)
    }

    fun mergeAllDataFromServer(){
        taskFlags.mergeAllDataFromServer(true)
    }

    suspend fun syncDataFilesIfNotJustDone(){
        if (Instant.now().epochSecond - DataVersions.mostRecentSyncEpochSecond() > TIME_BETWEEN_SYNC_CHECKS)
            syncDataFiles()
    }

    fun syncFlights(){
        taskFlags.syncFlights(true)
    }

    fun killDuplicates(){
        taskFlags.killDuplicates(true)
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

    // Delay next unforced sync by pretending we just synchronized
    fun delaySync(){
        ServerPrefs.mostRecentFlightsSyncEpochSecond(Instant.now().epochSecond)
    }

    companion object{
        val instance by lazy { SyncCenter() }
        private const val TIME_BETWEEN_SYNC_CHECKS: Long = 15*60L // 15 minutes * 60 seconds per minute
    }
}