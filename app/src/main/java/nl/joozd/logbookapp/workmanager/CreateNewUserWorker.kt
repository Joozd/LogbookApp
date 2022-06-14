package nl.joozd.logbookapp.workmanager

import android.content.Context
import androidx.work.CoroutineWorker
import androidx.work.WorkerParameters
import kotlinx.coroutines.withContext
import nl.joozd.logbookapp.comm.Cloud
import nl.joozd.logbookapp.utils.DispatcherProvider
import nl.joozd.logbookapp.comm.generateNewUserAndCreateOnServer

/*
 * This gets called from TaskDispatcher when [TaskDispatcher.newUserWantedFlow] == true,
 *  which is triggered primarily by [TaskFlags.createNewUser]
 * return:
 *  - Success() if successful, should set it's flag to false
 *  - Retry() if connection error from cloud function, should not touch its flag
 *  - Failure() if server refused to perform task, should not touch its flag
 *      (server refusal handling should have set another flag which will prevent this worker from being called until it is fixed)
 */
class CreateNewUserWorker(appContext: Context, workerParams: WorkerParameters, private val cloud: Cloud)
: CoroutineWorker(appContext, workerParams) {
    constructor(appContext: Context, workerParams: WorkerParameters): this(appContext, workerParams, Cloud()) // constructor needed to instantiate as a Worker
    override suspend fun doWork(): Result = withContext(DispatcherProvider.io()) {
        generateNewUserAndCreateOnServer(cloud).toListenableWorkerResult()
    }
}
