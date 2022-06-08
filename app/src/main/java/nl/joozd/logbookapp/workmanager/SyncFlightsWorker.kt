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
import androidx.work.CoroutineWorker
import androidx.work.WorkerParameters
import kotlinx.coroutines.withContext
import nl.joozd.logbookapp.comm.Cloud
import nl.joozd.logbookapp.comm.syncFlights
import nl.joozd.logbookapp.data.repository.flightRepository.FlightRepositoryWithDirectAccess
import nl.joozd.logbookapp.utils.DispatcherProvider

/*
 * This gets called from TaskDispatcher when [TaskDispatcher.syncNeededFlow] == true,
 *  which is triggered primarily by [TaskFlags.createNewUser]
 * return:
 *  - Success() if successful, should set it's flag to false
 *  - Retry() if connection error from cloud function, should not touch its flag
 *  - Failure() if server refused to perform task, should not touch its flag
 *      (server refusal handling should have set another flag which will prevent this worker from being called until it is fixed)
 */
class SyncFlightsWorker(appContext: Context, workerParams: WorkerParameters,
                        private val repository: FlightRepositoryWithDirectAccess = FlightRepositoryWithDirectAccess.instance,
                        private val cloud: Cloud = Cloud())
    : CoroutineWorker(appContext, workerParams) {

    override suspend fun doWork(): Result = withContext(DispatcherProvider.io()) {
        return@withContext syncFlights(cloud, repository).toListenableWorkerResult()
    }
}


