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
import nl.joozd.logbookapp.data.utils.Encryption

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
     * Calling function should consider storing username and pasword in [Preferences]
     */
    suspend fun createNewUser(name: String, key: ByteArray): Boolean? = withContext(Dispatchers.IO) {
        Client().use {
            ServerFunctions.createNewAccount(it, name, key)
        }
    }

    /**
     * Creates a new user
     * Calling function should consider storing username and pasword in [Preferences]
     */
    suspend fun createNewUser(name: String, password: String): Boolean? = withContext(Dispatchers.IO) {
        Client().use {
            ServerFunctions.createNewAccount(it, name, Encryption.md5Hash(password))
        }
    }

    /**
     * Changes a user's password
     * Calling function should consider storing username and pasword in [Preferences]
     */
    suspend fun changePassword(newPassword: String): Boolean? = withContext(Dispatchers.IO) {
        Client().use {client ->
            ServerFunctions.login(client)?.let{
                if (!it) return@withContext it
            }
            ServerFunctions.changePassword(client, Encryption.md5Hash(newPassword))
        }
    }




    /**
     * Check username / pass
     * ServerFunctions.testLogin returns 1 if success, 2 if failed, negative value if connection failed
     */
    suspend fun checkUser(username: String, password: String): Boolean? =  withContext(Dispatchers.IO) {
            when (Client().use{
                ServerFunctions.testLogin(it, username, password)
            }) {
                1 -> true
                2 -> false
                -998 -> {
                    Log.w("Cloud", "Server gave unexpected response")
                    null
                }
                else -> null
            }
        }

    /**
     * Check username / pass
     * ServerFunctions.testLogin returns 1 if success, 2 if failed, negative value if connection failed
     */
    suspend fun checkUserFromLink(username: String, password: String): Boolean? =  withContext(Dispatchers.IO) {
        when (Client().use{
            ServerFunctions.testLoginFromLink(it, username, password)
        }) {
            1 -> true
            2 -> false
            -998 -> {
                Log.w("Cloud", "Server gave unexpected response")
                null
            }
            else -> null
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

    suspend fun syncAllFlights(flightRepository: FlightRepository, listener: (Int) -> Unit = {}): Long? = try {
        withContext(Dispatchers.IO) f@{
            syncingFlights = true
            listener(0)
            Client().use { server ->
                listener(5) // Connection is made!
                with(ServerFunctions) {
                    //sync time with server
                    val timeStamp: Long = getTimestamp(server)?.also {
                        Log.d(TAG, "Got timestamp ${Instant.ofEpochSecond(it)}")
                        listener(10)
                        Preferences.serverTimeOffset = it - Instant.now().epochSecond
                    } ?: return@f null // if no timestamp received, server is not working so might as well quit here

                    //Login and handle if that fails:
                    when (login(server)) {
                        false -> {
                            flightRepository.setNotLoggedInFlag(true)
                            return@f -1L
                        }
                        null -> return@f null.also { Log.d("Cloud", "Login returned null") }
                    }
                    listener(15)

                    //get new flights from server
                    //listener from 15 to 40 (25 total)
                    val newFlightsFromServer = try {
                        requestFlightsSince(
                            server,
                            Preferences.lastUpdateTime
                        ) { listener(15 + it / 4) }?.filter{ !it.isPlanned }?.map { it.copy(timeStamp = timeStamp) } // don't load planned flights
                            ?: return@f null.also {
                                Log.w(
                                    "Cloud",
                                    "requestFlightsSince returned null"
                                )
                            }
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
                    val fixedNewLocalFlights = newLocalFlights.filter { it.flightID in takenIDs }
                        .mapIndexed { index: Int, flight: Flight ->
                            flight.copy(flightID = lowestFixedID + index)
                        }
                    launch(Dispatchers.Main) {
                        flightRepository.delete(
                            newLocalFlights.filter { it.flightID in takenIDs },
                            sync = false
                        )
                        flightRepository.save(fixedNewLocalFlights, sync = false)
                    }
                    fixedLocalFlights.addAll(fixedNewLocalFlights)

                    listener(50)
                    //previous block added all fixed flights to a list, now add the ones that didn't need fixing:
                    fixedLocalFlights.addAll(newLocalFlights.filter { it.flightID !in takenIDs })

                    //prepare list to send to Server:
                    // -> add fixed and not-fixed flights together
                    // -> change their timestamps to now
                    // (this means that editing flights on two devices before syncing will stick to most recent sync, not most recent edit)
                    val flightsToSend =
                        (completeFlightDB.filter { it.timeStamp > Preferences.lastUpdateTime && !it.unknownToServer && (!it.isPlanned) } + // Not including flightslist we just fixed. Don't sync planned flights.
                                fixedLocalFlights)
                            .filter { !it.isPlanned || !it.unknownToServer } // don't send planned flights unless server knows about them somehow
                            .map { it.copy(timeStamp = timeStamp, unknownToServer = false) }

                    //send the flights to server, retry on fail as login worked earlier
                    // Could make this incrementally increase progbar, but it would make things somewhat more inefficient. Lets see.
                    if (!sendFlights(server, flightsToSend)) return@f null.also {
                        Log.d(
                            "Cloud",
                            "sendFlights returned null"
                        )
                    }
                    listener(75)
                    //add timestamp to this transaction
                    if (!sendTimeStamp(server, timeStamp)) return@f null.also {
                        Log.d(
                            "Cloud",
                            "sendTimeStamp returned null"
                        )
                    }
                    listener(80)
                    //save changes on server
                    if (!save(server)) return@f null.also { Log.d("Cloud", "save returned null") }
                    listener(85)

                    //mark time of this successful sync
                    Preferences.lastUpdateTime = timeStamp

                    //Save flights with current timestamps and clear `changed` flags
                    //listsner from 85 to 100
                    launch(Dispatchers.Main) {
                        flightRepository.save(flightsToSend.map { it.copy(unknownToServer = false) } + newFlightsFromServer,
                            sync = false) // { listener(85 + it * 15 / 100) } // TODO Listsner not implemented
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
