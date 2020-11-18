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
import kotlinx.coroutines.NonCancellable
import kotlinx.coroutines.withContext
import nl.joozd.logbookapp.data.comm.Cloud
import nl.joozd.logbookapp.data.sharedPrefs.Preferences
import nl.joozd.logbookapp.extensions.atStartOfDay
import nl.joozd.logbookapp.extensions.minus
import java.time.Duration
import java.time.Instant
import java.time.OffsetDateTime

class RequestBackupIfScheduled(appContext: Context, workerParams: WorkerParameters)
    : CoroutineWorker(appContext, workerParams) {
    /**
     * A suspending method to do your work.  This function runs on the coroutine context specified
     * by [coroutineContext].
     * <p>
     * A CoroutineWorker is given a maximum of ten minutes to finish its execution and return a
     * [ListenableWorker.Result].  After this time has expired, the worker will be signalled to
     * stop.
     *
     * @return The [ListenableWorker.Result] of the result of the background work; note that
     * dependent work will not execute if you return [ListenableWorker.Result.failure]
     */
    override suspend fun doWork(): Result = withContext(Dispatchers.IO + NonCancellable) {
        if (!Preferences.backupFromCloud || !backupNeeded()) Result.success() // If backup not needed, this (checking if it is needed) is all we do.
        else {
            when (Cloud.requestBackup()) {
                true -> Result.success().also{
                    Preferences.mostRecentBackup = Instant.now().epochSecond
                }
                null -> Result.retry()
                false -> Result.failure()
            }
        }
    }

    private fun backupNeeded(): Boolean {
        if (Preferences.backupInterval == 0) return false
        val mostRecentBackup = Instant.ofEpochSecond(Preferences.mostRecentBackup).atStartOfDay(OffsetDateTime.now().offset)
        return Instant.now() - mostRecentBackup > Duration.ofDays(Preferences.backupInterval.toLong())
    }
}