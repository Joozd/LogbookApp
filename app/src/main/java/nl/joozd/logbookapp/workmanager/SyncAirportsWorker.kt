/*
 *  JoozdLog Pilot's Logbook
 *  Copyright (c) 2020-2022 Joost Welle
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
import nl.joozd.logbookapp.comm.OldCloud
import nl.joozd.logbookapp.comm.ServerFunctionResult
import nl.joozd.logbookapp.data.sharedPrefs.Prefs

class SyncAirportsWorker(appContext: Context, workerParams: WorkerParameters)
    : CoroutineWorker(appContext, workerParams) {
    /**
     * Try to download airport DB from server.
     */
    override suspend fun doWork(): Result = Result.failure() /* {
        try {
            val serverDbVersion = withContext(Dispatchers.IO) { OldCloud.getServerAirportDbVersion() }
            Log.d("syncAirportsWorker", "server DB = $serverDbVersion, local DB = ${Prefs.airportDbVersion}")
            when (serverDbVersion) {
                -1 -> return Result.failure()                              // -1 is server reported unable
                -2 -> return Result.retry()                                // -2 means connection failure
                Prefs.airportDbVersion -> return Result.success()    // DB is up-to-date
            }
            return when (OldCloud.downloadAirportsDatabase { processDownloadProgress(it) }){
                ServerFunctionResult.OK -> Result.success()
                else -> Result.retry() // connection went bad after checking version; retry
            }
        }
        catch(exception: Exception) {
            Log.e("SyncAirportsWorker", "exception:\n${exception.stackTraceToString()}")
            return Result.failure()
        }
    }

    private fun processDownloadProgress(p: Int){
        Log.d(this::class.simpleName, "processDownloadProgress($p)")
    }
    */
}