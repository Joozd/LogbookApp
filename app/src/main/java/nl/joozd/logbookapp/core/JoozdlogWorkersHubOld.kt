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

package nl.joozd.logbookapp.core

import androidx.work.*
import kotlinx.coroutines.*
import nl.joozd.logbookapp.data.repository.flightRepository.FlightRepositoryWithSpecializedFunctions
import nl.joozd.logbookapp.data.sharedPrefs.Prefs
import nl.joozd.logbookapp.workmanager.*
import java.time.Duration
import java.time.Instant
import java.util.concurrent.TimeUnit

/**
 * Central point for all things worker.
 */
@Deprecated("Use typed worker hubs")
object JoozdlogWorkersHubOld: JoozdlogWorkersHub(), CoroutineScope by MainScope() {
    /**
     * Send feedback to server.
     * [SubmitFeedbackWorker] will also reset [Prefs.feedbackWaiting] to an empty String
     */
    fun sendFeedback(contactInfo: String){
        val task = OneTimeWorkRequestBuilder<SubmitFeedbackWorker>()
            .needsNetwork()
            .setInputData(makeDataWithString(SubmitFeedbackWorker.CONTACT_INFO_TAG, contactInfo))
            .addTag(SUBMIT_FEEDBACK)
            .build()

        enqueue(task, SUBMIT_FEEDBACK, ExistingWorkPolicy.REPLACE)
    }

    private const val SUBMIT_FEEDBACK = "SUBMIT_FEEDBACK"
}