package nl.joozd.logbookapp.workmanager

import android.content.Context
import androidx.work.CoroutineWorker
import androidx.work.WorkerParameters
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import nl.joozd.logbookapp.comm.HTTPServer
import nl.joozd.logbookapp.comm.updateDataFiles
import nl.joozd.logbookapp.data.sharedPrefs.DataVersions
import nl.joozd.logbookapp.utils.TimestampMaker

class SyncDataFilesWorker(appContext: Context, workerParams: WorkerParameters, private val server: HTTPServer = HTTPServer())
    : CoroutineWorker(appContext, workerParams) {
    override suspend fun doWork(): Result = withContext(Dispatchers.IO) {
        updateDataFiles(server).also {
            if(it.isOK())
                DataVersions.mostRecentSyncEpochSecond(TimestampMaker().now)
        }.toListenableWorkerResult()

    }
}