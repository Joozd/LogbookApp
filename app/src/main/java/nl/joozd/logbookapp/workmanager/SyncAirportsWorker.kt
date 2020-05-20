/*
 *  JoozdLog Pilot's Logbook
 *  Copyright (c) 2020 Joost Welle
 *
 *      This program is free software: you can redistribute it and/or modify
 *      it under the terms of the GNU Affero General Public License as
 *      published by the Free Software Foundation, either version 3 of the
 *      License, or (at your option) any later version.
 *
 *      This program is distributed in the hope that it will be useful,
 *      but WITHOUT ANY WARRANTY; without even the implied warranty of
 *      MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 *      GNU Affero General Public License for more details.
 *
 *      You should have received a copy of the GNU Affero General Public License
 *      along with this program.  If not, see https://www.gnu.org/licenses
 *
 */

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
    private val airportsRepository = AirportRepository.getInstance()
    var progress: Int = 0
        set(p) {
            field = p
            airportsRepository.setAirportSyncProgress(p)
        }


    override suspend fun doWork(): Result = withContext(Dispatchers.IO) {
        val serverDbVersion = Cloud.getAirportDbVersion()
        Log.d("syncAirportsWorker", "server DB = $serverDbVersion, local DB = ${Preferences.airportDbVersion}")
        when (serverDbVersion){
            -1 -> return@withContext Result.failure()                               // -1 is server reported unable
            -2 -> return@withContext Result.retry()                                 // -2 means connection failure
            Preferences.airportDbVersion -> return@withContext Result.success().also{ progress = 100 }    // DB is up-to-date
        }
        progress = 5
        Cloud.getAirports{ processDownloadProgress(it) }?.map{ Airport(it) }?.let {
            AirportRepository.getInstance().replaceDbWith(it)
            progress = 99
            Preferences.airportDbVersion = serverDbVersion
            progress = 100
        }?: return@withContext Result.retry()                                       // something happened with connection?
        Result.success()
    }.also{
        progress = -1
    }

    private fun processDownloadProgress(p: Int){
        Log.d(this::class.simpleName, "processDownloadProgress($p)")
        progress = 5+p*3/4 // ends at 80
    }
}