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
     * Constants for influencing behaviour
     */
    private const val DELAY_FOR_OVERWRITE_MINUTES: Long = 1 // minutes
    private const val INBOUND_SYNC_MINIMUM_INTERVAL: Long = 15*60 // seconds

    private var lastSyncInstantEpochSeconds = 0 // on cold app restart, always sync.

    /**
     * Sync Flights when needed.
     * Needed means: Last sync not inside last INBOUND_SYNC_MINIMUM_INTERVAL minutes.
     * This should be called from MainActivity.onResume() so it is checked when app is opened.
     * Returns true if a sync is scheduled else false
     */
    fun syncTimeAndFlightsIfEnoughTimePassed(): Boolean{
        val elapsedSinceLastSync = Instant.now().epochSecond - lastSyncInstantEpochSeconds
        if (elapsedSinceLastSync < INBOUND_SYNC_MINIMUM_INTERVAL) return false
        synchronizeTimeAndFlights()
        return true
    }

    /**
     * Sync flights when updated flights found.
     * Updated means: Timestamp more recent than [lastSyncInstantEpochSeconds] and !isPlanned
     * This should be called from MainActivity.onPause() so it updates when app moves to background.
     */
    fun syncTimeAndFlightsIfFlightsUpdated(){
        launch {
            if (lastSyncInstantEpochSeconds < FlightRepositoryWithSpecializedFunctions.instance.getMostRecentTimestampOfACompletedFlight() ?: Long.MIN_VALUE)
                synchronizeTimeAndFlights()
        }
    }

    /**
     * Synchronizes time with server, stores offset in SharedPrefs through [Prefs]
     * If another Worker is already trying to do that, that one is canceled
     */
    fun synchronizeTime(){
        val task = OneTimeWorkRequestBuilder<SyncTimeWorker>()
            .needsNetwork()
            .addTag(SYNC_TIME)
            .build()

        enqueue(task, SYNC_TIME, ExistingWorkPolicy.KEEP)
    }


    /**
     * Synchronizes all flights with server (worker uses FlightRepository)
     * If another Worker is already trying to do that, that one is replaced
     */
    private fun synchronizeTimeAndFlights(){
        if (Prefs.useCloud) {
            val task = OneTimeWorkRequestBuilder<SyncFlightsWorker>()
                .needsNetwork()
                .addTag(SYNC_FLIGHTS)
                .build()

            enqueue(task, SYNC_FLIGHTS, ExistingWorkPolicy.REPLACE)
        }
    }

    /**
     * Schedule an email confirmation
     * If another worker is already doing this, that one will be kept and this will be ignored.
     */
    fun scheduleEmailConfirmation(){
        val task = OneTimeWorkRequestBuilder<ConfirmEmailWorker>()
            .needsNetwork()
            .addTag(CONFIRM_EMAIL)
            .build()

        enqueue(task, CONFIRM_EMAIL, ExistingWorkPolicy.KEEP)
    }

    /**
     * Schedule an email confirmation
     * If another worker is already doing this, that one will be kept and this will be ignored.
     */
    fun scheduleSetEmail(){
        val task = OneTimeWorkRequestBuilder<SetEmailWorker>()
            .needsNetwork()
            .addTag(CONFIRM_EMAIL)
            .build()
        enqueue(task, CONFIRM_EMAIL, ExistingWorkPolicy.KEEP)
    }

    /**
     * Schedule an email backup
     */
    fun scheduleLoginLinkEmail(){
        val task = OneTimeWorkRequestBuilder<SendLoginLinkEmailWorker>()
            .needsNetwork()
            .addTag(GET_BACKUP_EMAIL)
            .build()
        enqueue(task, GET_BACKUP_EMAIL, ExistingWorkPolicy.REPLACE)
    }



    /**
     * Schedule server login attempt
     * If another worker is already doing this, that one will be kept and this will be ignored.
     */
    fun scheduleLoginAttempt(){
        val task = OneTimeWorkRequestBuilder<CloudLoginWorker>()
            .needsNetwork()
            .addTag(LOGIN_TO_CLOUD)
            .build()
        enqueue(task, LOGIN_TO_CLOUD, ExistingWorkPolicy.KEEP)
    }

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

    /**
     * Gets airports from server and overwrites airportsDB if different.
     * If this work already exists, do nothing ( [ExistingWorkPolicy.KEEP] )
     * Runs once per day.
     * @param overwrite: If set to true, will replace previously enqueued work (use this when changing onlyUnmetered preference), with a DELAY_FOR_OVERWRITE_MINUTES minute delay.
     *
     */
    fun periodicGetAirportsFromServer(overwrite: Boolean = false){
        val constraints = Constraints.Builder()
            .setRequiresBatteryNotLow(true)
            .build()

        val task = PeriodicWorkRequestBuilder<SyncAirportsWorker>(Duration.ofDays(1)).apply {
            setConstraints(constraints)
            addTag(GET_AIRPORTS)
            if (overwrite)
                setInitialDelay(DELAY_FOR_OVERWRITE_MINUTES, TimeUnit.MINUTES)
        }.build()

        enqueue(task, GET_AIRPORTS, if (overwrite) ExistingPeriodicWorkPolicy.REPLACE else ExistingPeriodicWorkPolicy.KEEP)
    }


    /**
     * Gets aircraft types from server, and sends consensus data to server
     * @param overwrite: If set to true, will replace previously enqueued work (use this when changing onlyUnmetered preference)
     */
    fun periodicSynchronizeAircraftTypes(overwrite: Boolean = false){
        val constraints = Constraints.Builder()
            .setRequiresBatteryNotLow(true)
            .build()
        val task = PeriodicWorkRequestBuilder<SyncAircraftTypesWorker>(Duration.ofDays(1)).apply {
            setConstraints(constraints)
            addTag(SYNC_AIRCRAFT_TYPES)
            if (overwrite)
                setInitialDelay(DELAY_FOR_OVERWRITE_MINUTES, TimeUnit.MINUTES)
        }.build()

        enqueue(task, SYNC_AIRCRAFT_TYPES, if (overwrite) ExistingPeriodicWorkPolicy.REPLACE else ExistingPeriodicWorkPolicy.KEEP)
    }




    /**
     * Constants for use as tags
     */
    private const val SYNC_TIME = "SYNC_TIME"
    private const val SYNC_FLIGHTS = "SYNC_FLIGHTS"
    private const val CONFIRM_EMAIL = "CONFIRM_EMAIL"
    private const val LOGIN_TO_CLOUD = "LOGIN_TO_CLOUD"
    private const val GET_AIRPORTS = "GET_AIRPORTS"
    private const val SYNC_AIRCRAFT_TYPES = "SYNC_AIRCRAFT_TYPES"
    private const val GET_BACKUP_EMAIL = "GET_BACKUP_EMAIL"
    private const val TEST_EMAIL = "GET_BACKUP_EMAIL"
    private const val SUBMIT_FEEDBACK = "SUBMIT_FEEDBACK"
}