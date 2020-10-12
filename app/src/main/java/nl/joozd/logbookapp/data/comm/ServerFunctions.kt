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

import android.util.Base64
import android.util.Log
import nl.joozd.joozdlogcommon.*
import nl.joozd.logbookapp.data.comm.protocol.Client
import nl.joozd.logbookapp.model.dataclasses.Flight
import nl.joozd.logbookapp.data.sharedPrefs.Preferences
import nl.joozd.joozdlogcommon.comms.JoozdlogCommsKeywords
import nl.joozd.joozdlogcommon.exceptions.NotAuthorizedException
import nl.joozd.joozdlogcommon.serializing.*
import nl.joozd.joozdlogcommon.serializing.longFromBytes
import nl.joozd.joozdlogcommon.serializing.unwrapInt
import nl.joozd.joozdlogcommon.serializing.wrap
import java.security.MessageDigest

object ServerFunctions {
    const val TAG = "ServerFunctions"
    /**
     * sends a REQUEST TIMESTAMP to server
     * Expects server to reply with a single Long (8 Bytes)
     * @return the Timestamp from server as a Long (epochseconds) or -1 if error
     */
    fun getTimestamp(client: Client): Long?{
        client.sendRequest(JoozdlogCommsKeywords.REQUEST_TIMESTAMP)
        client.readFromServer()?.let {
            return longFromBytes(it)
        }
        Log.e("getTimestamp", "readFromServer() returned null")
        return null
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

    fun getForcedTypes(client: Client, listener: (Int) -> Unit): List<ForcedTypeData>?{
        client.sendRequest(JoozdlogCommsKeywords.REQUEST_FORCED_TYPES)
        return client.readFromServer(listener)?.let{
            unpackSerialized(it).map {bytes -> ForcedTypeData.deserialize (bytes)}
        }
    }


    fun getAircraftTypesVersion(client: Client, listener: (Int) -> Unit): Int? {
        client.sendRequest(JoozdlogCommsKeywords.REQUEST_AIRCRAFT_TYPES_VERSION)
        return client.readFromServer(listener)?.let{
            unwrap(it)
        }
    }

    fun getForcedAircraftTypesVersion(client: Client, listener: (Int) -> Unit): Int? {
        client.sendRequest((JoozdlogCommsKeywords.REQUEST_FORCED_TYPES_VERSION))
        return client.readFromServer(listener)?.let{
            unwrap(it)
        }
    }

    /**
     * Gets consensus data from server
     * If success, will return a Map<Registration to ByteArray>. ByteArray is a serialized AircraftType.
     */
    fun getConsensus(client: Client, listener: (Int) -> Unit = {}): Map<String, ByteArray>?{
        client.sendRequest(JoozdlogCommsKeywords.REQUEST_AIRCRAFT_CONSENSUS)
        return client.readFromServer(listener)?.let{
            mapFromBytes(it)
        }
    }

    /**
     * Logs a user in. User will remain logged in until connection with [client] is lost.
     * @return true is success, false if username/pass incorrect, null if connection problem
     */
    fun login(client: Client): Boolean?{
        if (Preferences.username == null || Preferences.key == null) {
            Log.d("Serverfunctions.login", "username: ${Preferences.username}, keySize: ${Preferences.key?.size}")
            return false
        }
        //payLoad is LoginData.serialize()
        val payLoad = LoginData(Preferences.username!!, Preferences.key!!, BasicFlight.VERSION.version).serialize()

        client.sendRequest(JoozdlogCommsKeywords.LOGIN, payLoad)
        return client.readFromServer()?.contentEquals(JoozdlogCommsKeywords.OK.toByteArray(Charsets.UTF_8))
    }

    /**
     * check username and password with server
     * @param client: [Client] to use for comms
     * @param username: username to check
     * @param password: password to check
     * @return error code if send error, 1 if OK, 2 if server OK but login/pass incorrect, -999 if receive error
     */
    fun testLogin(client: Client, username: String, password: String): Int{
        val payload = LoginData(username, makeKey(password),BasicFlight.VERSION.version).serialize()
        val requestResult = client.sendRequest(JoozdlogCommsKeywords.LOGIN, payload)
        if (requestResult < 0) return requestResult
        return when (val x = client.readFromServer()?.toString(Charsets.UTF_8)){
            JoozdlogCommsKeywords.OK -> 1
            JoozdlogCommsKeywords.UNKNOWN_USER_OR_PASS -> 2
            null -> -999
            else -> {
                Log.w("testLogin", "Server responded unexpected \"$x\"")
                -998
            } // for debugging, server responded something unexpected
        }
    }

    /**
     * check username and password with server
     * @param client: [Client] to use for comms
     * @param username: username to check
     * @param password: password to check
     * @return error code if send error, 1 if OK, 2 if server OK but login/pass incorrect, -999 if receive error
     */
    fun testLoginFromLink(client: Client, username: String, password: String): Int{
        val payload = LoginData(username, Base64.decode(password, Base64.DEFAULT) ,BasicFlight.VERSION.version).serialize()
        val requestResult = client.sendRequest(JoozdlogCommsKeywords.LOGIN, payload)
        if (requestResult < 0) return requestResult
        return when (val x = client.readFromServer()?.toString(Charsets.UTF_8)){
            JoozdlogCommsKeywords.OK -> 1
            JoozdlogCommsKeywords.UNKNOWN_USER_OR_PASS -> 2
            null -> -999
            else -> {
                Log.w("testLogin", "Server responded unexpected \"$x\"")
                -998
            } // for debugging, server responded something unexpected
        }
    }


    fun createNewAccount(client: Client, name: String, key: ByteArray): Boolean?{
        val payLoad = LoginData(name, key, BasicFlight.VERSION.version).serialize()
        client.sendRequest(JoozdlogCommsKeywords.NEW_ACCOUNT, payLoad)
        val result = client.readFromServer()
        Log.d(TAG, "Result was ${result?.toString(Charsets.UTF_8)}")
        return result?.contentEquals(JoozdlogCommsKeywords.OK.toByteArray(Charsets.UTF_8))
    }

    /**
     * Change password on a logged-in account
     * @return true on success, null on connection error, false on server error (other result than "OK")
     */
    fun changePassword(client: Client, newPassword: ByteArray): Boolean? {
        if (client.sendRequest(JoozdlogCommsKeywords.UPDATE_PASSWORD, newPassword) < 0) return null
        val result = client.readFromServer()
        Log.d(TAG, "Result for changePassword() was ${result?.toString(Charsets.UTF_8)}")
        return result.contentEquals(JoozdlogCommsKeywords.OK.toByteArray(Charsets.UTF_8))
    }

    /**
     * Runs listsner f with a 0-100 percentage completed value
     */
    fun requestFlightsSince(client: Client, timeStamp: Long, f: (Int) -> Unit = {}): List<Flight>?{
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
     * @return: true if server responds OK, else false.
     */
    fun sendFlights(client: Client, flightsToSend: List<Flight>): Boolean{
        client.sendRequest(
            JoozdlogCommsKeywords.SENDING_FLIGHTS,
            packSerialized(flightsToSend.map {it.toBasicFlight().serialize() }))
        return client.readFromServer()?.contentEquals(JoozdlogCommsKeywords.OK.toByteArray(Charsets.UTF_8)) ?: false
    }

    fun sendTimeStamp(client: Client, timeStamp: Long): Boolean {
        client.sendRequest(JoozdlogCommsKeywords.ADD_TIMESTAMP,
            wrap(timeStamp)
        )
        val reply = client.readFromServer()
        return reply?.contentEquals(JoozdlogCommsKeywords.OK.toByteArray(Charsets.UTF_8)) ?: false
    }

    /**
     * Send consensus data to server
     */
    fun sendConsensus(client: Client, consensus: List<ConsensusData>): Boolean{
        client.sendRequest(JoozdlogCommsKeywords.SENDING_AIRCRAFT_CONSENSUS, packSerializable(consensus))
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


    /**
     * Make a key from password in the same way it is done in Preferences, for checking purposes
     */
    private fun makeKey(password: String): ByteArray = with (MessageDigest.getInstance("MD5")) {
        update(password.toByteArray())
        digest()
    }


}