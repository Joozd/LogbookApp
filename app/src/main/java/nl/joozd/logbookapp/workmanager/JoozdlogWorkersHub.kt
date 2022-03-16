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

import android.util.Log
import androidx.work.*
import kotlinx.coroutines.*
import nl.joozd.logbookapp.App
import nl.joozd.logbookapp.data.repository.flightRepository.FlightRepositoryWithSpecializedFunctions
import nl.joozd.logbookapp.data.sharedPrefs.Preferences
import nl.joozd.logbookapp.utils.DispatcherProvider
import java.time.Duration
import java.time.Instant
import java.util.concurrent.TimeUnit
import kotlin.coroutines.CoroutineContext

/**
 * Central point for all things worker.
 */
object JoozdlogWorkersHub: CoroutineScope {
    override val coroutineContext: CoroutineContext
        get() = DispatcherProvider.default() + SupervisorJob()
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
        println("syncTimeAndFlightsIfEnoughTimePassed")
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
     * Synchronizes time with server, stores offset in SharedPrefs through [Preferences]
     * If another Worker is already trying to do that, that one is canceled
     */
    fun synchronizeTime(){
        val task = OneTimeWorkRequestBuilder<SyncTimeWorker>()
            .setConstraints(makeConstraintsNeedNetwork())
            .addTag(SYNC_TIME)
            .build()

        with (WorkManager.getInstance(App.instance)){
            enqueueUniqueWork(SYNC_TIME, ExistingWorkPolicy.KEEP, task)
        }
    }


    /**
     * Synchronizes all flights with server (worker uses FlightRepository)
     * If another Worker is already trying to do that, that one is replaced
     */
    private fun synchronizeTimeAndFlights(){
        println("synchronizeTimeAndFlights CP 1")
        if (Preferences.useCloud) {
            println("synchronizeTimeAndFlights CP 2")
            val task = OneTimeWorkRequestBuilder<SyncFlightsWorker>().apply {
                setConstraints(makeConstraintsNeedNetwork())
                addTag(SYNC_FLIGHTS)
            }.build()

            with(WorkManager.getInstance(App.instance)) {
                enqueueUniqueWork(SYNC_FLIGHTS, ExistingWorkPolicy.REPLACE, task)
            }
        }
    }

    /**
     * Schedule an email confirmation
     * If another worker is already doing this, that one will be kept and this will be ignored.
     */
    fun scheduleEmailConfirmation(){
        Log.d("scheduleEmailConf...", "Scheduling an email confirmation")
        val task = OneTimeWorkRequestBuilder<ConfirmEmailWorker>().apply{
            setConstraints(makeConstraintsNeedNetwork())
            addTag(CONFIRM_EMAIL)
        }.build()
        with(WorkManager.getInstance(App.instance)) {
            enqueueUniqueWork(CONFIRM_EMAIL, ExistingWorkPolicy.KEEP, task)
        }
    }

    /**
     * Schedule an email confirmation
     * If another worker is already doing this, that one will be kept and this will be ignored.
     */
    fun scheduleSetEmail(){
        Log.d("scheduleEmailConf...", "Scheduling an email confirmation")
        val task = OneTimeWorkRequestBuilder<SetEmailWorker>().apply{
            setConstraints(makeConstraintsNeedNetwork())
            addTag(CONFIRM_EMAIL)
        }.build()
        with(WorkManager.getInstance(App.instance)) {
            enqueueUniqueWork(CONFIRM_EMAIL, ExistingWorkPolicy.KEEP, task)
        }
    }


    /**
     * Schedule server login attempt
     * If another worker is already doing this, that one will be kept and this will be ignored.
     */
    fun scheduleLoginAttempt(){
        Log.d("scheduleLoginAttempt", "Scheduling a login attempt")
        val task = OneTimeWorkRequestBuilder<CloudLoginWorker>().apply{
            setConstraints(makeConstraintsNeedNetwork())
            addTag(LOGIN_TO_CLOUD)
        }.build()
        with(WorkManager.getInstance(App.instance)) {
            enqueueUniqueWork(LOGIN_TO_CLOUD, ExistingWorkPolicy.KEEP, task)
        }
    }

    /**
     * Send feedback to server.
     * [SubmitFeedbackWorker] will also reset [Preferences.feedbackWaiting] to an empty String
     */
    fun sendFeedback(contactInfo: String){
        val task = OneTimeWorkRequestBuilder<SubmitFeedbackWorker>()
            .setConstraints(makeConstraintsNeedNetwork())
            .setInputData(makeDataWithString(SubmitFeedbackWorker.CONTACT_INFO_TAG, contactInfo))
            .addTag(SUBMIT_FEEDBACK)
            .build()

        with(WorkManager.getInstance(App.instance)) {
            enqueueUniqueWork(SUBMIT_FEEDBACK, ExistingWorkPolicy.REPLACE, task)
        }
    }

    /**
     * Gets airports from server and overwrites airportsDB if different.
     * If this work already exists, do nothing ( [ExistingWorkPolicy.KEEP] )
     * Runs once per day.
     * @param onlyUnmetered: If set to true, runs only on Networktype.UNMETERED
     * @param overwrite: If set to true, will replace previously enqueued work (use this when changing onlyUnmetered preference), with a DELAY_FOR_OVERWRITE_MINUTES minute delay.
     *
     */
    fun periodicGetAirportsFromServer(onlyUnmetered: Boolean = false, overwrite: Boolean = false){
        val constraints = Constraints.Builder()
            .setRequiredNetworkType(if (onlyUnmetered) NetworkType.UNMETERED else NetworkType.CONNECTED)
            .setRequiresBatteryNotLow(true)
            .build()

        val task = PeriodicWorkRequestBuilder<SyncAirportsWorker>(Duration.ofDays(1)).apply {
            setConstraints(constraints)
            addTag(GET_AIRPORTS)
            if (overwrite)
                setInitialDelay(DELAY_FOR_OVERWRITE_MINUTES, TimeUnit.MINUTES)
        }.build()

        with (WorkManager.getInstance(App.instance)){
            enqueueUniquePeriodicWork(GET_AIRPORTS, if (overwrite) ExistingPeriodicWorkPolicy.REPLACE else ExistingPeriodicWorkPolicy.KEEP, task)
        }
    }


    /**
     * Gets aircraft types from server, and sends consensus data to server
     * @param onlyUnmetered: If set to true, runs only on Networktype.UNMETERED
     * @param overwrite: If set to true, will replace previously enqueued work (use this when changing onlyUnmetered preference)
     */
    fun periodicSynchronizeAircraftTypes(onlyUnmetered: Boolean = false, overwrite: Boolean = false){
        val constraints = Constraints.Builder()
            .setRequiredNetworkType(if (onlyUnmetered) NetworkType.UNMETERED else NetworkType.CONNECTED)
            .setRequiresBatteryNotLow(true)
            .build()
        val task = PeriodicWorkRequestBuilder<SyncAircraftTypesWorker>(Duration.ofDays(1)).apply {
            setConstraints(constraints)
            addTag(SYNC_AIRCRAFT_TYPES)
            if (overwrite)
                setInitialDelay(DELAY_FOR_OVERWRITE_MINUTES, TimeUnit.MINUTES)
        }.build()

        with (WorkManager.getInstance(App.instance)){
            enqueueUniquePeriodicWork(SYNC_AIRCRAFT_TYPES, if (overwrite) ExistingPeriodicWorkPolicy.REPLACE else ExistingPeriodicWorkPolicy.KEEP, task)
        }
    }

    /**
     * Schedule a check whether server should send a backup email
     */
    fun periodicBackupFromServer(force: Boolean = false){
        Log.d("periodBackupFrmServer()", "added task to check for backup")
        val constraints = makeConstraintsNeedNetwork()
        val task = PeriodicWorkRequestBuilder<RequestBackupIfScheduled>(Duration.ofDays(1)).apply {
            setConstraints(constraints)
            addTag(GET_BACKUP_EMAIL)
        }.build()
        with (WorkManager.getInstance(App.instance)){
            enqueueUniquePeriodicWork(GET_BACKUP_EMAIL, if (force) ExistingPeriodicWorkPolicy.REPLACE else ExistingPeriodicWorkPolicy.KEEP, task)
        }
    }



    /**
     * Reschedule Aircraft and Airport updates
     */
    fun rescheduleAircraftAndAirports(onlyUnmetered: Boolean){
        periodicGetAirportsFromServer(onlyUnmetered, true)
        periodicSynchronizeAircraftTypes(onlyUnmetered, true)
    }

    /**
     * Build Constraints with requiredNetworkType = NetworkType.CONNECTED
     */
    private fun makeConstraintsNeedNetwork() = Constraints.Builder()
        .setRequiredNetworkType(NetworkType.CONNECTED)
        .build()

    /**
     * Make an input data object with a single string
     */
    private fun makeDataWithString(tag: String, value: String) =
        Data.Builder().putString(tag, value).build()

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