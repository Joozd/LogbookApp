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

package nl.joozd.logbookapp.data.comm

import android.util.Log
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import nl.joozd.joozdlogcommon.AircraftType
import nl.joozd.joozdlogcommon.ForcedTypeData
import nl.joozd.logbookapp.data.comm.protocol.Client
import nl.joozd.logbookapp.model.dataclasses.Flight

import nl.joozd.logbookapp.data.sharedPrefs.Preferences
import nl.joozd.joozdlogcommon.exceptions.NotAuthorizedException
import nl.joozd.logbookapp.data.room.model.AircraftTypeConsensusData
import nl.joozd.logbookapp.data.repository.flightRepository.FlightRepository

import java.time.Instant

/**
 * Cloud will take care of all things happening in the cloud.
 * It consists of a number of functions, that will communicate with
 * the JoozdlogServer and take care of everything that happens
 */

object Cloud {
    var syncingFlights = false
    const val TAG = "JoozdLogCloud object"
    //TODO make listeners for progress tracking+

    /**********************************************************************************************
     * Utility functions
     **********************************************************************************************/

    suspend fun getTime(): Long? = withContext(Dispatchers.IO) {
        Client().use { client ->
            ServerFunctions.getTimestamp(client)
        }
    }

    /**
     * Creates a new user
     */
    suspend fun createNewUser(name: String, key: ByteArray): Boolean = withContext(Dispatchers.IO) {
        Client().use {
            ServerFunctions.createNewAccount(it, name, key)
        }
    }

    /**********************************************************************************************
     * Airport sync functions
     **********************************************************************************************/

    suspend fun getAirportDbVersion(): Int = withContext(Dispatchers.IO) {
        Client().use { server ->
            ServerFunctions.getAirportDbVersion(server) // no need to handle errors as negative values won't be higher than available ones
        }
    }

    // returns List<BasicAirport>
    suspend fun getAirports(listener: (Int) -> Unit = {}) = withContext(Dispatchers.IO) {
        Client().use {
            ServerFunctions.getAirports(it, listener)
        }
    }

    /**********************************************************************************************
     * Aircraft sync functions
     **********************************************************************************************/

    /**
     * Gets AircraftTypeConsensus from server
     */
    suspend fun getAircraftConsensus(listener: (Int) -> Unit = {}): List<AircraftTypeConsensusData> {
        TODO("Not Implemented")
    }

    suspend fun getAircraftTypes(listener: (Int) -> Unit = {}): List<AircraftType>? = withContext(Dispatchers.IO) {
        Client().use{
            ServerFunctions.getAircraftTypes(it, listener)
        }
    }

    suspend fun getForcedTypes(listener: (Int) -> Unit = {}): List<ForcedTypeData>? = withContext(Dispatchers.IO) {
        Client().use{
            ServerFunctions.getForcedTypes(it, listener)
        }
    }




    suspend fun getAircraftTypesVersion(listener: (Int) -> Unit = {}): Int? = withContext(Dispatchers.IO) {
        Client().use{
            ServerFunctions.getAircraftTypesVersion(it, listener)
        }
    }

    suspend fun getForcedAircraftTypesVersion(listener: (Int) -> Unit = {}): Int? = withContext(Dispatchers.IO) {
        Client().use{
            ServerFunctions.getForcedAircraftTypesVersion(it, listener)
        }
    }

    /**********************************************************************************************
     * Flights sync functions
     **********************************************************************************************/

    /**
     * Gets all flights from server
     * @param f: Listener for progress
     */
    suspend fun requestAllFlights(f: (Int) -> Unit = {}): List<Flight>? =
        withContext(Dispatchers.IO) {
            Client().use {
                if (ServerFunctions.login(it) != true) {
                    f(100)
                    null
                } else
                    try {
                        val result = ServerFunctions.requestFlightsSince(it, -100L, f)

                        result
                    } catch (nae: NotAuthorizedException) {
                        Log.e(TAG, nae.stackTrace.toString())
                        null
                    }
            }
        }

    /**
     * Just send all flights to server.
     * @param f: List of flights
     * @return: Error code, 0 if all OK
     * Error codes:
     *  1: Login failed
     *  2: Sending flights failed
     *  3: time sync failed
     */
    suspend fun justSendFlights(f: List<Flight>): Int = withContext(Dispatchers.IO) {
        Client().use { client ->
            val timeStamp = ServerFunctions.getTimestamp(client) ?: return@withContext 3
            val loggedIn = ServerFunctions.login(client) == true
            when {
                !loggedIn -> 1
                !ServerFunctions.sendFlights(client, f) -> 2
                else -> {
                    ServerFunctions.sendTimeStamp(client, timeStamp)
                    ServerFunctions.save(client)
                    ServerFunctions.finish(client)
                    0
                }
            }
        }
    }


    /**
     * @return timestamp on success, -1 on critical fail (ie wrong credentials), null on server error (retry later)
     * Listsner will give an estimated completion percentage
     */

    suspend fun syncAllFlights(flightRepository: FlightRepository, listener: (Int) -> Unit = {}): Long? =
        try {
            withContext(Dispatchers.IO) f@{
                syncingFlights = true
                listener(0)
                Log.d("YOLO", "SWAGGGGGG11111")
                Client().use { server ->
                    Log.d("YOLO", "SWAGGGGGG22222")

                    listener(5) // Connection is made!
                    with(ServerFunctions) {
                        Log.d("YOLO", "SWAGGGGGG33333")

                        //sync time with server
                        val timeStamp: Long = getTimestamp(server) ?: -1
                        Log.d(TAG, "Got timestamp ${Instant.ofEpochSecond(timeStamp)}")
                        listener(10)
                        Preferences.serverTimeOffset = timeStamp - Instant.now().epochSecond

                        //Login and handle if that fails:
                        when (login(server)) {
                            false -> return@f -1L
                            null -> return@f null.also{Log.d("Cloud", "Login returned null")}
                        }
                        listener(15)

                        //get new flights from server
                        //listener from 15 to 40 (25 total)
                        val newFlightsFromServer = try {
                            requestFlightsSince(
                                server,
                                Preferences.lastUpdateTime
                            ) { listener(15 + it / 4) }?.map { it.copy(timeStamp = timeStamp) }
                                ?: return@f null.also{Log.d("Cloud", "requestFlightsSince returned null")}
                        } catch (e: NotAuthorizedException) {
                            return@f -1L
                        }
                        listener(40)
                        val completeFlightDB = flightRepository.requestWholeDB()
                        listener(45)

                        //fix possible flightID conflicts
                        val newLocalFlights =
                            completeFlightDB.filter { it.unknownToServer }
                        val fixedLocalFlights = mutableListOf<Flight>()

                        val takenIDs = newFlightsFromServer.map { it.flightID }
                        val lowestFixedID =
                            (takenIDs.max()
                                ?: -1) + 1 // null on empty list, but empty list means no fixes
                        val newFlights = newLocalFlights.filter { it.flightID in takenIDs }
                            .mapIndexed { index: Int, flight: Flight ->
                                flight.copy(flightID = lowestFixedID + index)
                            }
                        launch(Dispatchers.Main) {
                            flightRepository.delete(newLocalFlights.filter { it.flightID in takenIDs }, sync = false)
                            flightRepository.save(newFlights, sync = false)
                        }
                        fixedLocalFlights.addAll(newFlights)

                        listener(50)
                        //previous block added all fixed flights to a list, now add the ones that didn't need fixing:
                        fixedLocalFlights.addAll(newLocalFlights.filter { it.flightID !in takenIDs })

                        //prepare list to send to Server:
                        // -> add fixed and not-fixed flights together
                        // -> change their timestamps to now
                        // (this means that editing flights on two devices before syncing will stick to most recent sync, not most recent edit)
                        val flightsToSend =
                            (completeFlightDB.filter { it.timeStamp > Preferences.lastUpdateTime && !it.unknownToServer } + // Not including flightslist we just fixed
                                    fixedLocalFlights)
                                .map { it.copy(timeStamp = timeStamp, unknownToServer = false) }

                        //send the flights to server, retry on fail as login worked earlier
                        // Could make this incrementally increase progbar, but it would make things somewhat more inefficient. Lets see.
                        if (!sendFlights(server, flightsToSend)) return@f null.also{Log.d("Cloud", "sendFlights returned null")}
                        listener(75)
                        //add timestamp to this transaction
                        if (!sendTimeStamp(server, timeStamp)) return@f null.also{Log.d("Cloud", "sendTimeStamp returned null")}
                        listener(80)
                        //save changes on server
                        if (!save(server)) return@f null.also{Log.d("Cloud", "save returned null")}
                        listener(85)

                        //mark time of this successful sync
                        Preferences.lastUpdateTime = timeStamp

                        //Save flights with current timestamps and clear `changed` flags
                        //listsner from 85 to 100
                        launch(Dispatchers.Main) {
                            flightRepository.save(flightsToSend.map { it.copy(unknownToServer = false) } + newFlightsFromServer, sync = false) // { listener(85 + it * 15 / 100) } // TODO Listsner not implemented
                        }

                        // Profit!
                        listener(100)
                        timeStamp
                    }
                }
            }
        } finally {
            syncingFlights = false
        }
}
