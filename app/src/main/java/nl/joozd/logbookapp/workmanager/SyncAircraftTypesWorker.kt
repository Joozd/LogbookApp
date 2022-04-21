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
import kotlinx.coroutines.*
import nl.joozd.logbookapp.data.comm.Cloud
import nl.joozd.logbookapp.data.repository.aircraftrepository.AircraftRepository
import nl.joozd.logbookapp.data.room.model.PreloadedRegistration
import nl.joozd.logbookapp.data.sharedPrefs.Prefs

/**
 * Sync aircraftTypes with server
 * - First, check version of forcedTypes and update that list if needed
 */
class SyncAircraftTypesWorker(appContext: Context, workerParams: WorkerParameters)
    : CoroutineWorker(appContext, workerParams) {


    override suspend fun doWork(): Result = withContext(Dispatchers.IO + NonCancellable) {
        var saveForced: Job? = null
        var saveTypes: Job? = null
        // var sendConsensus: Job? = null // this one is called straight away
        // var receiveConsensus: Job? = null // same

        Log.d(this::class.simpleName,"Started doWork()")
        val aircraftRepository = AircraftRepository.instance

        val serverTypesVersion = Cloud.getAircraftTypesVersion() ?: return@withContext Result.retry()
        val serverForcedVersion = Cloud.getForcedAircraftTypesVersion() ?: return@withContext Result.retry()
        Log.d(this::class.simpleName, "serverVersions $serverTypesVersion / $serverForcedVersion")


        if (serverTypesVersion != Prefs.aircraftTypesVersion) {
            Cloud.getAircraftTypes()?.let {
                Log.d(this::class.simpleName, "Downlaoded ${it.size} types")
                saveTypes = launch {
                    aircraftRepository.replaceAllTypesWith(it)
                    Prefs.aircraftTypesVersion = serverTypesVersion
                }

            } ?: return@withContext Result.retry()
        }


        if (serverForcedVersion != Prefs.aircraftForcedVersion) {
            Cloud.getForcedTypes()?.let {
                Log.d(this::class.simpleName, "Downlaoded ${it.size} forcedTypes")
                saveForced = launch {
                    aircraftRepository.replaceAllPreloadedWith(it.map {
                        PreloadedRegistration(registration = it.registration, type = it.type)
                    })
                    Prefs.aircraftForcedVersion = serverForcedVersion
                }
            } ?: return@withContext Result.retry()
        }


        saveTypes?.join()
        saveForced?.join()

        return@withContext Result.success()
    }
}