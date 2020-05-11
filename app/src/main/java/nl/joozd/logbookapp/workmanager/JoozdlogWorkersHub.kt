package nl.joozd.logbookapp.workmanager

import androidx.work.*
import nl.joozd.logbookapp.App
import nl.joozd.logbookapp.data.comm.Cloud
import nl.joozd.logbookapp.data.sharedPrefs.Preferences
import java.time.Duration

object JoozdlogWorkersHub {
    /**
     * Constants for influencing behaviour
     */

    private const val MIN_DELAY_FOR_OUTBOUND_SYNC: Long = 1 // minutes

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
            cancelAllWorkByTag(SYNC_TIME)
            enqueue(task)
        }
    }

    /**
     * Synchronizes all flights with server (worker uses FlightRepository)
     * If another Worker is already trying to do that, that one is canceled
     */
    fun synchronizeFlights(delay: Boolean = true){
        val constraints = Constraints.Builder()
            .setRequiredNetworkType(NetworkType.CONNECTED)
            .build()

        val task = OneTimeWorkRequestBuilder<SyncFlightsWorker>().apply {
            setConstraints(constraints)
            if (delay)
                setInitialDelay(Duration.ofMinutes(MIN_DELAY_FOR_OUTBOUND_SYNC))
            addTag(SYNC_FLIGHTS)
        }.build()

        with (WorkManager.getInstance(App.instance)){
            cancelAllWorkByTag(SYNC_FLIGHTS)
            enqueue(task)
        }


    }

    /**
     * Gets airports from server and overwrites airportsDB if different.
     * If this work already exists, do nothing ( [ExistingWorkPolicy.KEEP] )
     */
    fun getAirportsFromServer(onlyUnmetered: Boolean = true){
        val constraints = Constraints.Builder()
            .setRequiredNetworkType(if (onlyUnmetered) NetworkType.UNMETERED else NetworkType.CONNECTED)
            .build()

        val task = OneTimeWorkRequestBuilder<SyncAirportsWorker>()
            .setConstraints(constraints)
            .addTag(GET_AIRPORTS)
            .build()

        with (WorkManager.getInstance(App.instance)){
            enqueueUniqueWork(GET_AIRPORTS, ExistingWorkPolicy.KEEP, task)
        }
    }

    fun synchronizeAircraftTypes(){
        val constraints = Constraints.Builder()
            .setRequiredNetworkType(NetworkType.CONNECTED)
            .build()

        val task = OneTimeWorkRequestBuilder<SyncAircraftTypesWorker>()
            .setConstraints(constraints)
            .addTag(SYNC_AIRCRAFT_TYPES)
            .build()

        with (WorkManager.getInstance(App.instance)){
            enqueueUniqueWork(SYNC_AIRCRAFT_TYPES, ExistingWorkPolicy.KEEP, task)
        }
    }

    /**
     * Constants for use as tags
     */
    private const val SYNC_TIME = "syncTime"
    private const val SYNC_FLIGHTS = "syncFlights"
    private const val GET_AIRPORTS = "getAirports"
    private const val SYNC_AIRCRAFT_TYPES = "syncAircraftTypes"
}