package nl.joozd.logbookapp.workmanager

import android.content.Context
import android.util.Log
import androidx.work.CoroutineWorker
import androidx.work.WorkerParameters
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import nl.joozd.logbookapp.data.comm.Cloud
import nl.joozd.logbookapp.data.dataclasses.Airport
import nl.joozd.logbookapp.data.repository.AirportRepository
import nl.joozd.logbookapp.data.sharedPrefs.Preferences

class SyncAirportsWorker(appContext: Context, workerParams: WorkerParameters)
    : CoroutineWorker(appContext, workerParams) {

    override suspend fun doWork(): Result = withContext(Dispatchers.IO) {
        val serverDbVersion = Cloud.getAirportDbVersion()
        Log.d("syncAirportsWorker", "server DB = $serverDbVersion, local DB = ${Preferences.airportDbVersion}")
        when (serverDbVersion){
            -1 -> return@withContext Result.failure()                               // -1 is server reported unable
            -2 -> return@withContext Result.retry()                                 // -2 means connection failure
            Preferences.airportDbVersion -> return@withContext Result.success()     // DB is up-to-date
        }
        Cloud.getAirports()?.map{ Airport(it) }?.let {
            AirportRepository.getInstance().replaceDbWith(it)
            Preferences.airportDbVersion = serverDbVersion
        }?: return@withContext Result.retry()                                       // something happened with connection?
        Result.success()
    }
}