package nl.joozd.logbookapp.workmanager

import android.content.Context
import androidx.work.CoroutineWorker
import androidx.work.WorkerParameters
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import nl.joozd.logbookapp.data.comm.Cloud
import nl.joozd.logbookapp.data.repository.AircraftRepository
import nl.joozd.logbookapp.data.sharedPrefs.Preferences

class SyncAircraftTypesWorker(appContext: Context, workerParams: WorkerParameters)
    : CoroutineWorker(appContext, workerParams) {

    override suspend fun doWork(): Result = withContext(Dispatchers.IO) {
        val serverVersion = Cloud.getAircraftTypesVersion() ?: return@withContext Result.retry()
        if (serverVersion == Preferences.aircraftTypesVersion) return@withContext Result.success()
        Cloud.getAircraftTypes()?.let{
            AircraftRepository.getInstance().replaceAllTypesWith(it)
            Preferences.aircraftTypesVersion = serverVersion
            return@withContext Result.success()
        }?: return@withContext Result.retry()
    }
}