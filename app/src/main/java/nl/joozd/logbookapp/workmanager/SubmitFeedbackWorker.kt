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
import nl.joozd.logbookapp.data.comm.Cloud
import nl.joozd.logbookapp.data.sharedPrefs.Prefs

class SubmitFeedbackWorker(appContext: Context, workerParams: WorkerParameters)
    : CoroutineWorker(appContext, workerParams) {
    override suspend fun doWork(): Result {
        if (Prefs.feedbackWaiting.isBlank()) return Result.success()
        val contactInfo = inputData.getString(CONTACT_INFO_TAG) ?: ""
        return if (Cloud.sendFeedback(Prefs.feedbackWaiting, contactInfo).isOK()) {
            Prefs.feedbackWaiting = ""
            Result.success()
        } else Result.retry()
    }

    companion object{
        const val CONTACT_INFO_TAG = "CONTACT_INFO_TAG"
    }

}