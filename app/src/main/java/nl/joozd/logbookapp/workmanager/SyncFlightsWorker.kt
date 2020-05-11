package nl.joozd.logbookapp.workmanager

import android.content.Context
import androidx.work.CoroutineWorker
import androidx.work.WorkerParameters
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import nl.joozd.logbookapp.data.comm.Cloud
import nl.joozd.logbookapp.data.repository.FlightRepository
import nl.joozd.logbookapp.data.sharedPrefs.Preferences

class SyncFlightsWorker(appContext: Context, workerParams: WorkerParameters)
    : CoroutineWorker(appContext, workerParams) {
    override suspend fun doWork(): Result = withContext(Dispatchers.IO) {
        val flightsRepository = FlightRepository.getInstance()
        when(val result = Cloud.syncAllFlights(flightsRepository)) {
            null -> Result.retry()
            -1L -> Result.failure()
            else -> {
                Preferences.lastUpdateTime = result
                Result.success()
            }
        }
    }
}
