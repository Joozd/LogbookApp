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
import nl.joozd.joozdlogcommon.BasicAirport
import nl.joozd.serializing.SIZE_WRAPPED_INT
import nl.joozd.serializing.unpackSerialized
import nl.joozd.serializing.unwrapInt
import nl.joozd.logbookapp.data.comm.Cloud
import nl.joozd.logbookapp.data.dataclasses.Airport
import nl.joozd.logbookapp.data.repository.AirportRepository
import nl.joozd.logbookapp.data.sharedPrefs.Preferences
import nl.joozd.logbookapp.extensions.readUntilEOF
import java.net.URL

class SyncAirportsWorker(appContext: Context, workerParams: WorkerParameters)
    : CoroutineWorker(appContext, workerParams) {
    private val airportsRepository = AirportRepository.getInstance()
    var progress: Int = 0
        set(p) {
            field = p
            airportsRepository.setAirportSyncProgress(p)
        }


    /**
     * Try to downlaod airport DB from server. If that fails, try to get it from WWW. If that fails, retry or fail.
     */
    override suspend fun doWork(): Result = withContext(Dispatchers.IO) {
        airportsRepository.acquireLock()
        try {
            val serverDbVersion = Cloud.getAirportDbVersion()
            Log.d("syncAirportsWorker", "server DB = $serverDbVersion, local DB = ${Preferences.airportDbVersion}")
            when (serverDbVersion) {
                -1 -> return@withContext (if (getFromWWWIfNeeded()) Result.success() else Result.failure()).also { progress = 100 }                               // -1 is server reported unable
                -2 -> return@withContext (if (getFromWWWIfNeeded()) Result.success() else Result.retry()).also { progress = 100 }                                 // -2 means connection failure
                Preferences.airportDbVersion -> return@withContext Result.success().also { progress = 100 }    // DB is up-to-date
            }
            progress = 5
            Cloud.getAirports { processDownloadProgress(it) }?.map { Airport(it) }?.let {
                airportsRepository.replaceDbWith(it)
                progress = 99
                Preferences.airportDbVersion = serverDbVersion
                progress = 100
            } ?: return@withContext Result.retry()                                       // something happened with connection?
            Result.success()
        }
        catch(exception: Exception) {
            Log.e("SyncAirportsWorker", "exception:\n${exception.stackTraceToString()}")
            Result.failure()
        }
        finally {
            //in finally so lock always gets released
            airportsRepository.releaseLock()
        }

    }.also{
        progress = -1
    }

    private fun processDownloadProgress(p: Int){
        Log.d(this::class.simpleName, "processDownloadProgress($p)")
        progress = 5+p*3/4 // ends at 80
    }

    /**
     * Download airport DB over http. Return true if success, false if failed.
     */
    private fun getFromWWWIfNeeded(): Boolean{
        Log.d("XXXXXXXXXXXXX",  "started getfromWWW")
        if (Preferences.airportDbVersion == 0){ // only do this if no airport DB loaded yet
            val inputStream = try{
                URL(AIRPORT_DATABASE_URL).openConnection().getInputStream()
            } catch (e: Exception) {
                Log.w("SyncAirportsWorker", "Could not get Airport DB from http source")
                return false
            }
            inputStream.use{
                progress = 5
                val rawAirports = it.readUntilEOF()
                val version = unwrapInt(rawAirports.take(SIZE_WRAPPED_INT).toByteArray())
                val airports = unpackSerialized(rawAirports.drop(SIZE_WRAPPED_INT).toByteArray()).map { bytes -> BasicAirport.deserialize(bytes)}.map{ba -> Airport(ba)}
                progress = 50
                airportsRepository.replaceDbWith(airports)
                Preferences.airportDbVersion = version
                return true
            }

        }
        return false // if airport DB is not null, not grabbing it from www
    }
    companion object{
        const val AIRPORT_DATABASE_URL = "https://joozd.nl/joozdlog/airports"
    }
}