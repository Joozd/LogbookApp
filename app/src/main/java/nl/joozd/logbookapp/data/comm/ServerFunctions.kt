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

package nl.joozd.logbookapp.data.comm

import android.util.Log
import nl.joozd.joozdlogcommon.AircraftType
import nl.joozd.logbookapp.data.comm.protocol.Client
import nl.joozd.logbookapp.model.dataclasses.Flight
import nl.joozd.logbookapp.data.sharedPrefs.Preferences
import nl.joozd.joozdlogcommon.BasicAirport
import nl.joozd.joozdlogcommon.BasicFlight
import nl.joozd.joozdlogcommon.comms.JoozdlogCommsKeywords
import nl.joozd.joozdlogcommon.exceptions.NotAuthorizedException
import nl.joozd.joozdlogcommon.serializing.*
import nl.joozd.joozdlogcommon.serializing.longFromBytes
import nl.joozd.joozdlogcommon.serializing.unwrapInt
import nl.joozd.joozdlogcommon.serializing.wrap

object ServerFunctions {
    const val TAG = "ServerFunctions"
    /**
     * sends a REQUEST TIMESTAMP to server
     * Expects server to reply with a single Long (8 Bytes)
     * @return the Timestamp from server as a Long (epochseconds) or -1 if error
     */
    fun getTimestamp(client: Client): Long{
        client.sendRequest(JoozdlogCommsKeywords.REQUEST_TIMESTAMP)
        client.readFromServer()?.let {
            return longFromBytes(it)
        }
        Log.e("getTimestamp", "readFromServer() returned null")
        return -1
    }

    /**
     * returns version of Airport Database
     * @param client: Client to use
     * @return version of database on server, or negative value if error:
     * -1 = server error
     * -2 = connection error
     */
    fun getAirportDbVersion(client: Client): Int{
        client.sendRequest(JoozdlogCommsKeywords.REQUEST_AIRPORT_DB_VERSION)
        return client.readFromServer()?.let {
            unwrapInt(it)
        } ?: -2
    }

    /**
     * Downloads list of airports from server
     * @param client: Client to use
     * @return: List of Airport
     */
    fun getAirports(client: Client, listener: (Int) -> Unit): List<BasicAirport>?{
        client.sendRequest(JoozdlogCommsKeywords.REQUEST_AIRPORT_DB)
        return client.readFromServer(listener)?.let {
            unpackSerialized(it).map { bytes -> BasicAirport.deserialize(bytes)}
        }
    }

    /**
     * Downloads list if AircraftTypes from server
     * @param client: Client to use
     * @return: List of AircraftType
     */

    fun getAircraftTypes(client: Client, listener: (Int) -> Unit): List<AircraftType>?{
        client.sendRequest(JoozdlogCommsKeywords.REQUEST_AIRCRAFT_TYPES)
        return client.readFromServer(listener)?.let{
            unpackSerialized(it).map {bytes -> AircraftType.deserialize (bytes)}
        }
    }

    fun login(client: Client): Boolean?{
        if (Preferences.username == null || Preferences.key == null)
            return false
        //payLoad is username as wrap, last 16 bytes are encryption key
        val payLoad = wrap(Preferences.username!!) + Preferences.key!!

        client.sendRequest(JoozdlogCommsKeywords.LOGIN, payLoad)
        return client.readFromServer()?.contentEquals(JoozdlogCommsKeywords.OK.toByteArray(Charsets.UTF_8))
    }

    fun createNewAccount(client: Client, name: String, key: ByteArray): Boolean{
        val payLoad = wrap(name) + key
        client.sendRequest(JoozdlogCommsKeywords.NEW_ACCOUNT, payLoad)
        val result = client.readFromServer()
        Log.d(TAG, "Result was ${result?.toString(Charsets.UTF_8)}")
        return result?.contentEquals(JoozdlogCommsKeywords.OK.toByteArray(Charsets.UTF_8)) ?: false
    }

    fun requestFlightsSince(client: Client, timeStamp: Long): List<Flight>?{
        client.sendRequest(JoozdlogCommsKeywords.REQUEST_FLIGHTS_SINCE_TIMESTAMP,
            wrap(timeStamp)
        )
        client.readFromServer()?.let{
            if (it.contentEquals(JoozdlogCommsKeywords.NOT_LOGGED_IN.toByteArray())) throw (NotAuthorizedException("Server responded ${JoozdlogCommsKeywords.NOT_LOGGED_IN}"))
            if (it.contentEquals(JoozdlogCommsKeywords.SERVER_ERROR.toByteArray())) return null
            return unpackSerialized(it)
                .map{ serializedBasicFlight->
                    Flight(
                        BasicFlight.deserialize(serializedBasicFlight)
                    )
                }
        }
        return null
    }

    /**
     * Runs listsner f with a 0-100 percentage completed value
     */
    fun requestFlightsSince(client: Client, timeStamp: Long, f: (Int) -> Unit): List<Flight>?{
        client.sendRequest(JoozdlogCommsKeywords.REQUEST_FLIGHTS_SINCE_TIMESTAMP,
            wrap(timeStamp)
        )
        client.readFromServer(f)?.let{
            if (it.contentEquals(JoozdlogCommsKeywords.NOT_LOGGED_IN.toByteArray())) throw (NotAuthorizedException("Server responded ${JoozdlogCommsKeywords.NOT_LOGGED_IN}"))
            if (it.contentEquals(JoozdlogCommsKeywords.SERVER_ERROR.toByteArray())) return null
            return unpackSerialized(it)
                .map{ serializedBasicFlight->
                    Flight(
                        BasicFlight.deserialize(serializedBasicFlight)
                    )
                }
        }
        return null
    }

    /**
     * Send flights to server
     * @param client: an initialized Client to send/receive data
     * @param flightsToSend: A list of Flights to send to server
     * @param compressed: Whether or not to compress the flights. Keep false for now.
     * @return: true if server responds OK, else false.
     */
    fun sendFlights(client: Client, flightsToSend: List<Flight>, compressed: Boolean = false): Boolean{
        client.sendRequest(JoozdlogCommsKeywords.SENDING_FLIGHTS,
            packSerialized(flightsToSend.map {
                it.toBasicFlight().serialize()
            }),
            compressed = compressed
        )
        return client.readFromServer()?.contentEquals(JoozdlogCommsKeywords.OK.toByteArray(Charsets.UTF_8)) ?: false
    }

    fun sendTimeStamp(client: Client, timeStamp: Long): Boolean {
        client.sendRequest(JoozdlogCommsKeywords.ADD_TIMESTAMP,
            wrap(timeStamp)
        )
        return client.readFromServer()?.contentEquals(JoozdlogCommsKeywords.OK.toByteArray(Charsets.UTF_8)) ?: false
    }

    /**
     * Run this at the end of a session to save server changes to disk and close connection
     */
    fun finish(client: Client){
        client.sendRequest(JoozdlogCommsKeywords.END_OF_SESSION)
    }

    fun save(client: Client): Boolean {
        client.sendRequest(JoozdlogCommsKeywords.SAVE_CHANGES)
        return client.readFromServer()?.contentEquals(JoozdlogCommsKeywords.OK.toByteArray(Charsets.UTF_8)) ?: false
    }


}