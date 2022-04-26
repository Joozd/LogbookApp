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
import nl.joozd.logbookapp.data.comm.CloudFunctionResults
import nl.joozd.logbookapp.data.sharedPrefs.Prefs
import nl.joozd.logbookapp.data.sharedPrefs.errors.Errors
import nl.joozd.logbookapp.data.sharedPrefs.errors.ScheduledErrors

/**
 * This is used to login from a login link if connection failure when doing it right away
 */
class CloudLoginWorker(appContext: Context, workerParams: WorkerParameters)
    : CoroutineWorker(appContext, workerParams) {
    override suspend fun doWork(): Result = withContext(Dispatchers.IO) {
        if (':' !in Prefs.loginLinkStringWaiting) return@withContext Result.failure() // bad login string
        val loginPass = Prefs.loginLinkStringWaiting.replace('-', '/').split(":").let { lp ->
            lp.first() to lp.last()
        }
        when (UserManagement.loginFromLink(loginPass)) {
            CloudFunctionResults.OK -> Result.success()
            CloudFunctionResults.UNKNOWN_USER_OR_PASS -> {
                ScheduledErrors.addError(Errors.LOGIN_DATA_REJECTED_BY_SERVER)
                Result.failure()
            } // bad login data in link
            else -> Result.retry()
        }
    }
}