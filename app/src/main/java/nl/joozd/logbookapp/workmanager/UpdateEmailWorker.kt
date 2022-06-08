package nl.joozd.logbookapp.workmanager

import android.content.Context
import androidx.work.CoroutineWorker
import androidx.work.WorkerParameters
import kotlinx.coroutines.withContext
import nl.joozd.logbookapp.comm.updateEmailAddressOnServer
import nl.joozd.logbookapp.comm.Cloud
import nl.joozd.logbookapp.utils.DispatcherProvider

/*
 * This gets called from TaskDispatcher when [TaskDispatcher.emailUpdateWantedFlow] == true,
 *  which is triggered primarily by [TaskFlags.updateEmailWithServer]
 *
 * return:
 *  - Success() if successful, should set it's flag to false
 *  - Retry() if connection error from cloud function, should not touch its flag
 *  - Failure() if server refused to perform task, should not touch its flag
 *      (server refusal handling should have set another flag which will prevent this worker from being called until it is fixed)
 */
class UpdateEmailWorker(appContext: Context, workerParams: WorkerParameters, private val cloud: Cloud = Cloud())
    : CoroutineWorker(appContext, workerParams) {
    override suspend fun doWork(): Result = withContext(DispatcherProvider.io()) {
        updateEmailAddressOnServer(cloud).toListenableWorkerResult()
    }
}