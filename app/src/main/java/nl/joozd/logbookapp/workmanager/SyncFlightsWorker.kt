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
import androidx.work.CoroutineWorker
import androidx.work.WorkerParameters
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import nl.joozd.logbookapp.data.comm.Cloud
import nl.joozd.logbookapp.data.repository.FlightRepository
import nl.joozd.logbookapp.data.sharedPrefs.Preferences

class SyncFlightsWorker(appContext: Context, workerParams: WorkerParameters)
    : CoroutineWorker(appContext, workerParams) {

    private val flightRepository = FlightRepository.getInstance()
    private var progress: Int = 0
        set(p) {
            field = p
            flightRepository.setSyncProgress(p)
        }


    override suspend fun doWork(): Result = try {
        withContext(Dispatchers.IO) {
            val flightsRepository = FlightRepository.getInstance()
            when(val result = Cloud.syncAllFlights(flightsRepository) { processDownloadProgress(it) }) {
                null -> Result.retry()
                -1L -> Result.failure()
                else -> {
                    Preferences.lastUpdateTime = result
                    progress = 100
                    Result.success()
                }
            }
        }
    } finally{
        progress = -1
    }

    private fun processDownloadProgress(p: Int){
        progress = p*99/100
    }
}
