package nl.joozd.logbookapp.workmanager

import android.content.Context
import androidx.work.CoroutineWorker
import androidx.work.WorkerParameters
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import nl.joozd.logbookapp.comm.HTTPServer
import nl.joozd.logbookapp.comm.updateDataFiles
import nl.joozd.logbookapp.data.sharedPrefs.DataVersions
import java.time.Instant

class SyncDataFilesWorker(appContext: Context, workerParams: WorkerParameters, private val server: HTTPServer)
    : CoroutineWorker(appContext, workerParams) {
    constructor(appContext: Context, workerParams: WorkerParameters): this(appContext, workerParams, HTTPServer())
    override suspend fun doWork(): Result = withContext(Dispatchers.IO) {
        updateDataFiles(server).also {
            if(it.isOK())
                DataVersions.mostRecentDataFilesSyncEpochSecond(Instant.now().epochSecond)
        }.toListenableWorkerResult()

    }
}