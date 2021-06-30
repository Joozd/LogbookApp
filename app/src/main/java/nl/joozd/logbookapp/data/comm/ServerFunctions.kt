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
import nl.joozd.logbookapp.data.comm.protocol.CloudFunctionResults
import nl.joozd.logbookapp.data.sharedPrefs.errors.Errors
import nl.joozd.logbookapp.data.sharedPrefs.errors.ScheduledErrors
import java.nio.charset.Charset
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

    fun requestBackup(client: Client): CloudFunctionResults {
        val n = Preferences.username.also{Log.d("DEBUG1","key: $it")}
        val k = Preferences.key.also{Log.d("DEBUG1","key: $it")}
        if (n == null || k == null) return CloudFunctionResults.NO_LOGIN_DATA
        val payload = LoginDataWithEmail(n, k, BasicFlight.VERSION.version, Preferences.emailAddress).serialize()
        client.sendRequest(JoozdlogCommsKeywords.REQUEST_BACKUP_MAIL, payload)
        return when (client.readFromServer()?.toString(Charsets.UTF_8)) {
            JoozdlogCommsKeywords.OK -> CloudFunctionResults.OK
            JoozdlogCommsKeywords.BAD_DATA_RECEIVED -> CloudFunctionResults.DATA_ERROR
            JoozdlogCommsKeywords.UNKNOWN_USER_OR_PASS -> CloudFunctionResults.UNKNOWN_USER_OR_PASS
            JoozdlogCommsKeywords.NOT_A_VALID_EMAIL_ADDRESS -> CloudFunctionResults.EMAIL_DOES_NOT_MATCH
            null -> CloudFunctionResults.CLIENT_ERROR
            else -> CloudFunctionResults.UNKNOWN_REPLY_FROM_SERVER
        }
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
            return false
        }
        //payLoad is LoginData.serialize()
        val payLoad = LoginData(Preferences.username!!, Preferences.key!!, BasicFlight.VERSION.version).serialize()

        client.sendRequest(JoozdlogCommsKeywords.LOGIN, payLoad)
        return when (client.readFromServer()?.toString(Charsets.UTF_8)){
            JoozdlogCommsKeywords.OK -> true
            JoozdlogCommsKeywords.UNKNOWN_USER_OR_PASS -> false.also{
                ScheduledErrors.addError(Errors.LOGIN_DATA_REJECTED_BY_SERVER)
            }
            else -> null
        }

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

    /**
     * Request a username from server. This should get a bare string as reply, which will be the username to return.
     * Will return username, or null if client error
     */
    fun requestUsername(client: Client): String?{
        client.sendRequest(JoozdlogCommsKeywords.REQUEST_NEW_USERNAME)
        return client.readFromServer()?.toString(Charsets.UTF_8)
    }

    /**
     * Ask server to create a new account.
     * @param name: Username
     * @param key: Key
     */
    fun createNewAccount(client: Client, name: String, key: ByteArray): CloudFunctionResults{
        val payLoad = LoginData(name, key, BasicFlight.VERSION.version).serialize()
        client.sendRequest(JoozdlogCommsKeywords.NEW_ACCOUNT, payLoad)
        val result = client.readFromServer()
        Log.d(TAG, "Result was ${result?.toString(Charsets.UTF_8)}")
        return when (result?.toString(Charsets.UTF_8)){
            JoozdlogCommsKeywords.OK -> CloudFunctionResults.OK
            JoozdlogCommsKeywords.NOT_A_VALID_EMAIL_ADDRESS -> CloudFunctionResults.NOT_A_VALID_EMAIL_ADDRESS
            JoozdlogCommsKeywords.SERVER_ERROR -> CloudFunctionResults.SERVER_ERROR
            null -> CloudFunctionResults.CLIENT_ERROR
            else -> CloudFunctionResults.UNKNOWN_REPLY_FROM_SERVER
        }
    }

    /**
     * Send new email address to server
     * Server will send a confirmation mail if it worked.
     */
    fun sendNewEmailData(client: Client, emailToSend: String): CloudFunctionResults =
        generateLoginDataWithEmail(email = emailToSend)?.let{loginData ->
            if (client.sendRequest(JoozdlogCommsKeywords.SET_EMAIL, loginData.serialize()) < 0) CloudFunctionResults.CLIENT_ERROR
            else when(client.readFromServer()?.toString(Charsets.UTF_8)) {
                JoozdlogCommsKeywords.OK -> CloudFunctionResults.OK
                JoozdlogCommsKeywords.UNKNOWN_USER_OR_PASS -> CloudFunctionResults.UNKNOWN_USER_OR_PASS
                JoozdlogCommsKeywords.NOT_A_VALID_EMAIL_ADDRESS -> CloudFunctionResults.NOT_A_VALID_EMAIL_ADDRESS
                JoozdlogCommsKeywords.SERVER_ERROR -> CloudFunctionResults.SERVER_ERROR
                null -> CloudFunctionResults.CLIENT_ERROR
                else -> CloudFunctionResults.UNKNOWN_REPLY_FROM_SERVER
            }
        } ?: CloudFunctionResults.NO_LOGIN_DATA


    /**
     * send an email confirmation string to server
     */
    fun confirmEmail(client: Client, confirmationString: String): CloudFunctionResults{
        val payload = wrap(confirmationString)
        if (client.sendRequest(JoozdlogCommsKeywords.CONFIRM_EMAIL, payload) < 0) return CloudFunctionResults.CLIENT_ERROR
        return when(client.readFromServer()?.toString(Charsets.UTF_8)){
            null -> CloudFunctionResults.CLIENT_ERROR
            JoozdlogCommsKeywords.OK -> CloudFunctionResults.OK
            JoozdlogCommsKeywords.UNKNOWN_USER_OR_PASS -> CloudFunctionResults.UNKNOWN_USER_OR_PASS
            JoozdlogCommsKeywords.EMAIL_NOT_KNOWN_OR_VERIFIED -> CloudFunctionResults.EMAIL_DOES_NOT_MATCH
            else -> CloudFunctionResults.UNKNOWN_REPLY_FROM_SERVER
        }
    }

    /**
     * Request an email with a login link from the server
     */
    fun requestLoginLinkMail(client: Client): CloudFunctionResults {
        val n = Preferences.username
        val k = Preferences.key
        if (n == null || k == null) return CloudFunctionResults.NO_LOGIN_DATA
        val payload = LoginDataWithEmail(n, k, BasicFlight.VERSION.version, Preferences.emailAddress).serialize()
        println("XXXXX LALALALALA BANAAN 1")
        client.sendRequest(JoozdlogCommsKeywords.REQUEST_LOGIN_LINK_MAIL, payload)
        println("XXXXX LALALALALA BANAAN 2")
        return when (client.readFromServer()?.toString(Charsets.UTF_8)) { // <--- Somehow this doesn't return
            null -> CloudFunctionResults.CLIENT_ERROR
            JoozdlogCommsKeywords.OK -> CloudFunctionResults.OK
            JoozdlogCommsKeywords.UNKNOWN_USER_OR_PASS -> CloudFunctionResults.UNKNOWN_USER_OR_PASS
            JoozdlogCommsKeywords.EMAIL_NOT_KNOWN_OR_VERIFIED -> CloudFunctionResults.EMAIL_DOES_NOT_MATCH
            else -> CloudFunctionResults.UNKNOWN_REPLY_FROM_SERVER
        }.also{
            println(it)
        }
    }


    /**
     * Change password on a logged-in account
     * @param client: The [Client] to use
     * @param newPassword: The new password to set. (this only works when logged in so no need for old pass)
     * @param email: Email to send new login link to.
     * @return true on success, null on connection error, false on server error (other result than "OK")
     */
    fun changePassword(client: Client, newPassword: ByteArray, email: String): Boolean? {
        val payload = LoginDataWithEmail("", newPassword, 0, email).serialize() // username and basicFlightVersion are unused in this function
        if (client.sendRequest(JoozdlogCommsKeywords.UPDATE_PASSWORD, payload) < 0) return null
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
     * Send feedback to server
     */

    fun sendFeedback(client: Client, feedbackData: FeedbackData): Boolean{
        client.sendRequest(JoozdlogCommsKeywords.SENDING_FEEDBACK, feedbackData.serialize())
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

    private fun generateLoginDataWithEmail(username: String? = null, key: ByteArray? = null, email: String? = null): LoginDataWithEmail?{
        val n = username ?: Preferences.username
        val k = key ?: Preferences.key
        return if (n == null || k == null)  null else
        LoginDataWithEmail(n, k, BasicFlight.VERSION.version, email ?: Preferences.emailAddress)
    }
/*
    fun sendTestMail(client: Client): Boolean{
        client.sendRequest(JoozdlogCommsKeywords.DEBUG_SEND_TEST_MAIL)
        return client.readFromServer()?.contentEquals(JoozdlogCommsKeywords.OK.toByteArray(Charsets.UTF_8)) ?: false
    }

 */
}