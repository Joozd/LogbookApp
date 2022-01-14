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
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import nl.joozd.logbookapp.data.comm.Cloud
import nl.joozd.logbookapp.data.sharedPrefs.Preferences
import java.time.Instant


/**
 * Worker to sync flights to server
 * protocol:
 * - Client contacts server, saying username/password
 * - Client asks for timestamp
 * - Server checks if file `username` exists, if so, it loads flights/aircraft from files
 * - server responds "OK" or "UNKNOWN_USER"<make registration for users?> or "WRONG_PASSWORD"
 *
 * - Client asks for all changes since last sync (ie. last timestamp from server)
 * - server sends them (can be empty)
 *
 *   [if new flights on client]:
 * - client adjusts flightIDs of new flights so they won't conflict with flights on server
 * - Client responds with a list of new and changed flights, as serialized BasicFlights
 * - Server adds flights to list, sends OK
 * - client sends timestamp to go with this sync
 * - server sends OK
 * - client sends "save" command
 * - server sends OK
 * - Client marks flights as "known to server"
 * - Client saves timestamp as time of previous synch
 */
class SyncTimeWorker(appContext: Context, workerParams: WorkerParameters)
    : CoroutineWorker(appContext, workerParams) {
    override suspend fun doWork(): Result = withContext(Dispatchers.IO) {
        val serverTime = Cloud.getTime() ?: return@withContext Result.retry()
        val now = Instant.now().epochSecond
        Preferences.serverTimeOffset = serverTime - now
        Result.success()
    }
}


