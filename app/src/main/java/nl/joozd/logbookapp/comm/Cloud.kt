/*
 * JoozdLog Pilot's Logbook
 * Copyright (C) 2020 Joost Welle
 *
 *     This program is free software: you can redistribute it and/or modify
 *     it under the terms of the GNU Affero General Public License as
 *     published by the Free Software Foundation, either version 3 of the
 *     License, or (at your option) any later version.
 *
 *     This program is distributed in the hope that it will be useful,
 *     but WITHOUT ANY WARRANTY; without even the implied warranty of
 *     MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 *     GNU Affero General Public License for more details.
 *
 *     You should have received a copy of the GNU Affero General Public License
 *     along with this program.  If not, see https://www.gnu.org/licenses
 */

package nl.joozd.logbookapp.comm

import android.util.Log
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import nl.joozd.logbookapp.comm.protocol.Client
import nl.joozd.logbookapp.data.dataclasses.Flight

import nl.joozd.logbookapp.data.room.Repository
import nl.joozd.logbookapp.data.sharedPrefs.Preferences
import nl.joozd.logbookapp.extensions.toBoolean
import nl.joozd.joozdlogcommon.exceptions.NotAuthorizedException
import nl.joozd.logbookapp.data.dataclasses.AircraftTypeConsensus
import nl.joozd.logbookapp.data.dataclasses.Airport

import java.time.Instant

/**
 * Cloud will take care of all things happening in the cloud.
 * It consists of a number of functions, that will communicate with
 * the JoozdlogServer and take care of everything that happens
 */

object Cloud {
    const val TAG = "JoozdLogCloud object"
    //TODO make listeners for progress tracking+

    suspend fun getTime(): Long = withContext(Dispatchers.IO) {
        Client().use { client ->
            ServerFunctions.getTimestamp(client)
        }
    }

    /**
     * Just send all flights to server.
     * @param f: List of flights
     * @return: Error code, 0 if all OK
     * Error codes:
     *  1: Login failed
     *  2: Sending flights failed
     */
    suspend fun justSendFlights(f: List<Flight>): Int = withContext(Dispatchers.IO) {
        Client().use { client ->
            val timeStamp = ServerFunctions.getTimestamp(client)
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
     * Creates a new user
     */
    suspend fun createNewUser(name: String, key: ByteArray): Boolean = withContext(Dispatchers.IO) {
        Client().use {
            ServerFunctions.createNewAccount(it, name, key)
        }
    }

    suspend fun requestAllFlights(): List<Flight>? = withContext(Dispatchers.IO) {
        Client().use {
            if (ServerFunctions.login(it) != true) null
            else
                try {
                    ServerFunctions.requestFlightsSince(it, -10L)
                } catch (nae: NotAuthorizedException) {
                    Log.e(TAG, nae.stackTrace.toString())
                    null
                }
        }
    }

    suspend fun requestAllFlights(f: (Int) -> Unit): List<Flight>? =
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


    suspend fun getAirportDbVersion(): Int = withContext(Dispatchers.IO) {
        Client().use { server ->
            ServerFunctions.getAirportDbVersion(server) // no need to handle errors as negative values won't be higher than available ones
        }
    }

    suspend fun getAirports(listener: (Int) -> Unit = {}) = withContext(Dispatchers.IO) {
        Client().use {
            ServerFunctions.getAirports(it, listener)
        }
    }

    /**
     * Updates airports from server to DB
     * @param listener: A listener function that will get a completion factor of 0-100
     *      Listener assumes download and saving will both be 50% of work.
     */
    suspend fun updateAirportsDb(listener: (Int) -> Unit): Boolean = withContext(Dispatchers.IO) {
        getAirports{ listener(it*99/ 100) }?.let {
            val repository = Repository.getInstance()
            repository.clearFlightDB()
            listener(99)
            repository.saveAirports(it.map{ba ->
                Airport(
                    ba
                )
            })
            listener(100)
            true
        } ?: false
    }

    /**
     * Gets AircraftTypeConsensus from server
     */
    suspend fun getAircraftConsensus(listener: (Int) -> Unit = {}): List<AircraftTypeConsensus> {
        TODO("Not Implemented")
    }


    /**
     * @return true on success, false on critical fail (ie wrong credentials), null on server error (retry later)
     * Listsner will give an estimated completion percentage
     */
    suspend fun runFullUpdate(listener: (Int) -> Unit = {}): Boolean? =
        withContext(Dispatchers.IO) f@{
            val repository = Repository.getInstance()
            listener(0)
            Client().use { server ->
                listener(5) // Connection is made, it's something!
                with(ServerFunctions) {

                    val timeStamp: Long = getTimestamp(server)
                    Log.d(TAG, "Got timestamp $timeStamp")
                    listener(10)


                    //update timeOffset for local flights
                    val currentTime = Instant.now().epochSecond
                    Preferences.serverTimeOffset = timeStamp - currentTime

                    //Login and handle if that fails:
                    when (login(server)) {
                        false -> return@f false
                        null -> return@f null
                    }
                    listener(15)

                    //get new flights from server
                    //listener from 15 to 40 (25 total)
                    val newFlightsFromServer = try {
                        requestFlightsSince(
                            server,
                            Preferences.lastUpdateTime
                        ) { listener(15 + it / 4) }?.map { it.copy(timeStamp = timeStamp) }
                            ?: return@f null
                    } catch (e: NotAuthorizedException) {
                        return@f false
                    }
                    listener(40)
                    val allFlights = repository.requestAllFlights()
                    listener(45)

                    //fix possible flightID conflicts
                    val newLocalFlights = allFlights.filter { it.changed.toBoolean() }
                    val fixedLocalFlights = mutableListOf<Flight>()

                    val takenIDs = newFlightsFromServer.map { it.flightID }
                    val lowestFixedID =
                        (takenIDs.max()
                            ?: -1) + 1 // null on empty list, but empty list means no fixes
                    val newFlights = newLocalFlights.filter { it.flightID in takenIDs }.mapIndexed { index: Int, flight: Flight ->
                         flight.copy(flightID = lowestFixedID + index) }
                    repository.deleteFlightsHard(newLocalFlights.filter { it.flightID in takenIDs })
                    repository.saveFlights(newFlights)
                    fixedLocalFlights.addAll(newFlights)

                    listener(50)
                    //previous block added all fixed flights to a list, now add the ones that didn't need fixing:
                    fixedLocalFlights.addAll(newLocalFlights.filter { it.flightID !in takenIDs })

                    //prepare list to send to Server:
                    // -> add fixed and not-fixed flights together
                    // -> change their timestamps to now
                    // (this means that editing flights on two devices before syncing will stick to most recent sync, not most recent edit)
                    val flightsToSend =
                        (allFlights.filter { it.timeStamp > Preferences.lastUpdateTime && it.changed == 0 } + // Not including flightslist we just fixed
                                fixedLocalFlights)
                            .map { it.copy(timeStamp = timeStamp, changed = 0) }

                    //send the flights to server, retry on fail as login worked earlier
                    // Could make this incrementally increase progbar, but it would make things somewhat more inefficient. Lets see.
                    if (!sendFlights(server, flightsToSend)) return@f null
                    listener(75)
                    //add timestamp to this transaction
                    if (!sendTimeStamp(server, timeStamp)) return@f null
                    listener(80)
                    //save changes on server
                    if (!save(server)) return@f null
                    listener(85)

                    //mark time of this successful sync
                    Preferences.lastUpdateTime = timeStamp

                    //Save flights with current timestamps and clear `changed` flags
                    //listsner from 85 to 100
                    repository.saveFlights(flightsToSend.map { it.copy(changed = 0) } + newFlightsFromServer) // { listener(85 + it * 15 / 100) } // TODO Listsner not implemented

                    // Profit!
                    listener(100)
                    true
                }
            }
        }
}
