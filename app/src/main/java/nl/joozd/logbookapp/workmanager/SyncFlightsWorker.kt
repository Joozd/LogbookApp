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
import android.widget.Toast
import androidx.work.CoroutineWorker
import androidx.work.WorkerParameters
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import nl.joozd.logbookapp.R
import nl.joozd.logbookapp.data.comm.Cloud
import nl.joozd.logbookapp.data.comm.UserManagement
import nl.joozd.logbookapp.data.comm.CloudFunctionResults
import nl.joozd.logbookapp.data.repository.flightRepository.FlightRepository
import nl.joozd.logbookapp.data.repository.flightRepository.FlightRepositoryImpl
import nl.joozd.logbookapp.data.repository.flightRepository.FlightRepositoryWithDirectAccess
import nl.joozd.logbookapp.data.sharedPrefs.Preferences
import nl.joozd.logbookapp.ui.utils.toast

class SyncFlightsWorker(appContext: Context, workerParams: WorkerParameters)
    : CoroutineWorker(appContext, workerParams) {

    private val flightRepository = FlightRepositoryWithDirectAccess.instance

    override suspend fun doWork(): Result = withContext(Dispatchers.IO) {
        if (makeNewLoginDataIfNeeded() != CloudFunctionResults.OK)
            Result.retry()
        else
            when (val result = Cloud.syncAllFlights(flightRepository)) {
                null -> Result.retry()
                -1L -> Result.failure()
                else -> {
                    Preferences.lastUpdateTime = result
                    toast(R.string.flights_synced)
                    Result.success()
                }
            }
    }

    /**
     * Make new login data if needed
     * @return [CloudFunctionResults.OK] if not needed or success, something else if failure
     */
    private suspend fun makeNewLoginDataIfNeeded(): CloudFunctionResults =
        if (Preferences.username == null) UserManagement.newLoginDataNeeded()
        else CloudFunctionResults.OK
}
