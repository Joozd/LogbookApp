package nl.joozd.logbookapp.workmanager

import android.content.Context
import androidx.work.CoroutineWorker
import androidx.work.WorkerParameters
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import nl.joozd.logbookapp.comm.Cloud
import nl.joozd.logbookapp.comm.requestBackupMail

class SendBackupEmailWorker(appContext: Context, workerParams: WorkerParameters, private val cloud: Cloud = Cloud())
    : CoroutineWorker(appContext, workerParams) {
    constructor(appContext: Context, workerParams: WorkerParameters): this(appContext, workerParams, Cloud()) // constructor needed to instantiate as a Worker
    override suspend fun doWork(): Result = withContext(Dispatchers.IO) {
        requestBackupMail(cloud).toListenableWorkerResult()
    }
}
