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
import nl.joozd.logbookapp.core.UserManagement
import nl.joozd.logbookapp.data.sharedPrefs.Prefs
import nl.joozd.logbookapp.data.sharedPrefs.errors.Errors
import nl.joozd.logbookapp.data.sharedPrefs.errors.ScheduledErrors

/**
 * Set email with the server. This worker is used when client is offline when changing email so it gets changed when client gets back online
 */
class SetEmailWorker(appContext: Context, workerParams: WorkerParameters)
    : CoroutineWorker(appContext, workerParams) {
    override suspend fun doWork(): Result = withContext(Dispatchers.IO) {
        when{
            Prefs.emailAddress.isBlank() -> Result.failure()

            /*
             In case of client error (ie. internet failed before this is done), function will try to schedule work again
             which will fail because policy should be KEEP. However, as it does not add an unacceptable error to ScheduledErrors,
             this when will default to Result.retry()
            */
            UserManagement.changeEmailAddress() ->
                Result.success()

            // These would have been filled by UserManagement.confirmEmail
            ScheduledErrors.currentErrors.any{ it in unacceptableErrors } ->
                Result.failure()

            else -> Result.retry()
        }
    }

    private val unacceptableErrors = listOf(Errors.BAD_EMAIL_SAVED, Errors.LOGIN_DATA_REJECTED_BY_SERVER)
}