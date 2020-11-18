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

import android.util.Log
import androidx.work.*
import nl.joozd.logbookapp.App
import nl.joozd.logbookapp.data.sharedPrefs.Preferences
import java.time.Duration
import java.util.concurrent.TimeUnit

object JoozdlogWorkersHub {
    /**
     * Constants for influencing behaviour
     */
    private const val MIN_DELAY_FOR_OUTBOUND_SYNC: Long = 1 // minutes
    private const val DELAY_FOR_OVERWRITE_MINUTES: Long = 1 // minutes



    /**
     * These will be set to true if a forced update job is scheduled.
     * If set to true, a periodic job will not be scheduled.
     * A forced job should set this to false after completing and schedule a periodic job.
     */
    var forcedAirportWork: Boolean = false
    var forcedAircraftWork: Boolean = false


    /**
     * Synchronizes time with server, stores offset in SharedPrefs through [Preferences]
     * If another Worker is already trying to do that, that one is canceled
     */
    fun synchronizeTime(){
        val constraints = Constraints.Builder()
            .setRequiredNetworkType(NetworkType.CONNECTED)
            .build()

        val task = OneTimeWorkRequestBuilder<SyncTimeWorker>()
            .setConstraints(constraints)
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
    fun synchronizeFlights(delay: Boolean = true){
        if (Preferences.useCloud) {
            val constraints = Constraints.Builder()
                .setRequiredNetworkType(NetworkType.CONNECTED)
                .setRequiresBatteryNotLow(true)
                .build()

            val task = OneTimeWorkRequestBuilder<SyncFlightsWorker>().apply {
                setConstraints(constraints)
                if (delay)
                    setInitialDelay(MIN_DELAY_FOR_OUTBOUND_SYNC, TimeUnit.MINUTES)
                addTag(SYNC_FLIGHTS)
            }.build()

            with(WorkManager.getInstance(App.instance)) {
                enqueueUniqueWork(SYNC_FLIGHTS, ExistingWorkPolicy.REPLACE, task)
            }
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
     *
     */
    fun periodicBackupFromServer(force: Boolean = false){
        Log.d("periodBackupFrmServer()", "added task to check for backup")
        val constraints = Constraints.Builder()
            .setRequiredNetworkType(NetworkType.CONNECTED)
            .setRequiresBatteryNotLow(true)
            .build()
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
     * Constants for use as tags
     */
    private const val SYNC_TIME = "SYNC_TIME"
    private const val SYNC_FLIGHTS = "SYNC_FLIGHTS"
    private const val GET_AIRPORTS = "GET_AIRPORTS"
    private const val SYNC_AIRCRAFT_TYPES = "SYNC_AIRCRAFT_TYPES"
    private const val GET_BACKUP_EMAIL = "GET_BACKUP_EMAIL"
    private const val TEST_EMAIL = "GET_BACKUP_EMAIL"
}