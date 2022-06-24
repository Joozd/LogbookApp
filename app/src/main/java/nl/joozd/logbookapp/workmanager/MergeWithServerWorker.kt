package nl.joozd.logbookapp.workmanager

import android.content.Context
import androidx.work.CoroutineWorker
import androidx.work.WorkerParameters
import kotlinx.coroutines.withContext
import nl.joozd.logbookapp.comm.Cloud
import nl.joozd.logbookapp.comm.mergeFlightsWithServer
import nl.joozd.logbookapp.core.usermanagement.UserManagement
import nl.joozd.logbookapp.data.repository.flightRepository.FlightRepositoryWithDirectAccess
import nl.joozd.logbookapp.utils.DispatcherProvider

class MergeWithServerWorker(appContext: Context,
                            workerParams: WorkerParameters,
                            private val cloud: Cloud,
                            private val userManagement: UserManagement,
                            private val repository: FlightRepositoryWithDirectAccess
): CoroutineWorker(appContext, workerParams) {
    constructor(appContext: Context, workerParams: WorkerParameters): this(appContext, workerParams, Cloud(), UserManagement(), FlightRepositoryWithDirectAccess.instance) // constructor needed to instantiate as a Worker

    override suspend fun doWork(): Result = withContext(DispatcherProvider.io()) {
        return@withContext mergeFlightsWithServer(cloud, userManagement, repository).toListenableWorkerResult()
    }
}