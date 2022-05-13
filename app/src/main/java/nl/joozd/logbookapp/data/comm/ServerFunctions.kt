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

package nl.joozd.logbookapp.data.comm

import android.util.Base64
import android.util.Log
import nl.joozd.comms.Client
import nl.joozd.comms.isOK
import nl.joozd.joozdlogcommon.*
import nl.joozd.logbookapp.model.dataclasses.Flight
import nl.joozd.logbookapp.data.sharedPrefs.Prefs
import nl.joozd.joozdlogcommon.comms.JoozdlogCommsKeywords
import nl.joozd.logbookapp.core.UserManagement
import nl.joozd.logbookapp.data.sharedPrefs.EmailPrefs
import nl.joozd.serializing.*
import nl.joozd.logbookapp.data.sharedPrefs.errors.Errors
import nl.joozd.logbookapp.exceptions.NotAuthorizedException
import nl.joozd.logbookapp.extensions.nullIfBlank
import nl.joozd.logbookapp.extensions.toCloudFunctionResults
import java.security.MessageDigest

// NOTE: ALL THESE FUNCTIONS MUST BE CALLED FROM SUSPENDED FUNCTION DUE TO BLOCKING CALLS
// If properly used in a Client().use{} block that should always be the case as Client() has
// blocking code in its constructor.
@Deprecated("Use Cloud")
object ServerFunctions {
    private const val TAG = "ServerFunctions"
    /*
    /**
     * sends a REQUEST TIMESTAMP to server
     * Expects server to reply with a single Long (8 Bytes)
     * @return the Timestamp from server as a Long (epochSeconds) or -1 if error
     */
    suspend fun getTimestamp(client: Client): Long?{
        client.sendRequest(JoozdlogCommsKeywords.REQUEST_TIMESTAMP)
        client.readFromServer()?.let {
            return longFromBytes(it)
        }
        Log.e("getTimestamp", "readFromServer() returned null - ")
        return null
    }

    /**
     * Request Server to send a backup mail
     * @return [ServerFunctionResult]:
     *  [ServerFunctionResult.OK] if all OK
     *  [ServerFunctionResult.NO_LOGIN_DATA] if no login data saved
     *  [ServerFunctionResult.DATA_ERROR] if bad data was received
     *  [ServerFunctionResult.UNKNOWN_USER_OR_PASS] if login data rejected by server
     *  [ServerFunctionResult.NOT_A_VALID_EMAIL_ADDRESS] if [Prefs.emailAddress] is blank or server doesn't like the email address]
     *  [ServerFunctionResult.CLIENT_ERROR] if Client got an error (eg. died while receiving data)
     *  [ServerFunctionResult.CLIENT_NOT_ALIVE] if Client died
     *  [ServerFunctionResult.UNKNOWN_REPLY_FROM_SERVER] if server sent an unknown reply
     *  or, it can return any of the errors [Client.sendRequest] can return
     */
    suspend fun requestBackup(client: Client): CloudFunctionResult {
        if(checkIfLoginDataSet()) return CloudFunctionResult.SERVER_REFUSED
        val payload = LoginDataWithEmail(
            Prefs.username!!,
            Prefs.key!!,
            BasicFlight.VERSION.version,
            EmailPrefs.emailAddress.nullIfBlank() ?: return ServerFunctionResult.NOT_A_VALID_EMAIL_ADDRESS
        ).serialize()
        client.sendRequest(JoozdlogCommsKeywords.REQUEST_BACKUP_MAIL, payload).let{
            if (!it.isOK())
                return it.toCloudFunctionResults()
        }
        return when (val r = readServerResponse(client)) {
            JoozdlogCommsKeywords.OK -> ServerFunctionResult.OK
            JoozdlogCommsKeywords.BAD_DATA_RECEIVED -> ServerFunctionResult.DATA_ERROR
            JoozdlogCommsKeywords.UNKNOWN_USER_OR_PASS -> ServerFunctionResult.UNKNOWN_USER_OR_PASS
            JoozdlogCommsKeywords.NOT_A_VALID_EMAIL_ADDRESS -> ServerFunctionResult.EMAIL_DOES_NOT_MATCH
            null -> if (client.alive) ServerFunctionResult.CLIENT_ERROR else ServerFunctionResult.CLIENT_NOT_ALIVE
            else -> ServerFunctionResult.UNKNOWN_REPLY_FROM_SERVER.also{
                Log.w("requestBackup()", "Received unknown reply from server: $r")
            }
        }
    }



    /**
     * Downloads list of airports from server
     * @param client: Client to use
     * @return: List of Airport
     */
    suspend fun getAirports(client: Client, listener: (Int) -> Unit): List<BasicAirport>?{
        return if (client.sendRequest(JoozdlogCommsKeywords.REQUEST_AIRPORT_DB).isOK())
            client.readFromServer(listener)?.let {
                unpackSerialized(it).map { bytes -> BasicAirport.deserialize(bytes)}
            }
        else null
    }

    /**
     * Downloads list if AircraftTypes from server
     * @param client: Client to use
     * @return: List of AircraftType
     */

    suspend fun getAircraftTypes(client: Client, listener: (Int) -> Unit): List<AircraftType>?{
        client.sendRequest(JoozdlogCommsKeywords.REQUEST_AIRCRAFT_TYPES)
        return client.readFromServer(listener)?.let{
            unpackSerialized(it).map {bytes -> AircraftType.deserialize (bytes)}
        }
    }

    suspend fun getForcedTypes(client: Client, listener: (Int) -> Unit): List<ForcedTypeData>?{
        client.sendRequest(JoozdlogCommsKeywords.REQUEST_FORCED_TYPES)
        return client.readFromServer(listener)?.let{
            unpackSerialized(it).map {bytes -> ForcedTypeData.deserialize (bytes)}
        }
    }


    suspend fun getAircraftTypesVersion(client: Client, listener: (Int) -> Unit): Int? {
        client.sendRequest(JoozdlogCommsKeywords.REQUEST_AIRCRAFT_TYPES_VERSION)
        return client.readFromServer(listener)?.let{
            unwrap(it)
        }
    }

    suspend fun getForcedAircraftTypesVersion(client: Client, listener: (Int) -> Unit): Int? {
        client.sendRequest((JoozdlogCommsKeywords.REQUEST_FORCED_TYPES_VERSION))
        return client.readFromServer(listener)?.let{
            unwrap(it)
        }
    }


    /**
     * Logs a user in. User will remain logged in until connection with [client] is lost.
     * @return [ServerFunctionResult]:
     *  [ServerFunctionResult.OK] if logged in OK
     *  [ServerFunctionResult.NO_LOGIN_DATA] if no login data stored in [Prefs]
     *  [ServerFunctionResult.UNKNOWN_USER_OR_PASS] if server rejected login data. In this case, an error to be shown to user will be scheduled through [ScheduledErrors.addError]
     *  [ServerFunctionResult.CLIENT_ERROR] if Client got an error (eg. died while receiving data)
     *  [ServerFunctionResult.CLIENT_NOT_ALIVE] if Client died
     *  [ServerFunctionResult.UNKNOWN_REPLY_FROM_SERVER] if server sent an unknown reply
     */
    suspend fun login(client: Client): ServerFunctionResult {
        //payLoad is LoginData.serialize()
        val payLoad = LoginData(
            Prefs.username ?: return ServerFunctionResult.NO_LOGIN_DATA,
            Prefs.key ?: return ServerFunctionResult.NO_LOGIN_DATA,
            BasicFlight.VERSION.version)
            .serialize()

        client.sendRequest(JoozdlogCommsKeywords.LOGIN, payLoad)
        return when (readServerResponse(client)){
            JoozdlogCommsKeywords.OK -> ServerFunctionResult.OK
            JoozdlogCommsKeywords.UNKNOWN_USER_OR_PASS -> ServerFunctionResult.UNKNOWN_USER_OR_PASS.also{
                ScheduledErrors.addError(Errors.LOGIN_DATA_REJECTED_BY_SERVER)
            }
            null -> {
                if (client.alive) ServerFunctionResult.CLIENT_ERROR
                else ServerFunctionResult.CLIENT_NOT_ALIVE
            }
            else -> ServerFunctionResult.UNKNOWN_REPLY_FROM_SERVER.also{
                Log.w("login()", "Got unexpected reply from server: $it")
            }
        }

    }

    /**
     * check username and password with server
     * @param client: [Client] to use for comms
     * @param username: username to check
     * @param password: password to check. Password must not already be hashed.
     * @return a [ServerFunctionResult] value
     *  [ServerFunctionResult.OK] if logged in OK
     *  [ServerFunctionResult.UNKNOWN_USER_OR_PASS] if server rejected login data. In this case, an error to be shown to user will be scheduled through [ScheduledErrors.addError]
     *  [ServerFunctionResult.CLIENT_ERROR] if Client got an error (eg. died while receiving data)
     *  [ServerFunctionResult.CLIENT_NOT_ALIVE] if Client died
     *  [ServerFunctionResult.UNKNOWN_REPLY_FROM_SERVER] if server sent an unknown reply
     */
    suspend fun testLogin(client: Client, username: String, password: String): ServerFunctionResult {
        val payload = LoginData(username, makeKey(password),BasicFlight.VERSION.version).serialize()
        val requestResult = client.sendRequest(JoozdlogCommsKeywords.LOGIN, payload)
        if (!requestResult.isOK()) return requestResult.toCloudFunctionResults()
        return when (val x = readServerResponse(client)){
            JoozdlogCommsKeywords.OK -> ServerFunctionResult.OK
            JoozdlogCommsKeywords.UNKNOWN_USER_OR_PASS -> ServerFunctionResult.UNKNOWN_USER_OR_PASS
            null -> {
                if (client.alive) ServerFunctionResult.CLIENT_ERROR
                else ServerFunctionResult.CLIENT_NOT_ALIVE
            }
            else -> {
                Log.w("testLogin", "Server responded unexpected \"$x\"")
                ServerFunctionResult.UNKNOWN_REPLY_FROM_SERVER
            } // for debugging, server responded something unexpected
        }
    }

    /**
     * check username and password with server
     * @param client: [Client] to use for comms
     * @param username: username to check
     * @param password: password to check. This expects an already hashed password.
     * @return a [ServerFunctionResult] value
     *  [ServerFunctionResult.OK] if logged in OK
     *  [ServerFunctionResult.UNKNOWN_USER_OR_PASS] if server rejected login data. In this case, an error to be shown to user will be scheduled through [ScheduledErrors.addError]
     *  [ServerFunctionResult.CLIENT_ERROR] if Client got an error (eg. died while receiving data)
     *  [ServerFunctionResult.CLIENT_NOT_ALIVE] if Client died
     *  [ServerFunctionResult.UNKNOWN_REPLY_FROM_SERVER] if server sent an unknown reply
     */
    suspend fun testLoginFromLink(client: Client, username: String, password: String): ServerFunctionResult {
        val payload = LoginData(username, Base64.decode(password, Base64.DEFAULT) ,BasicFlight.VERSION.version).serialize()
        val requestResult = client.sendRequest(JoozdlogCommsKeywords.LOGIN, payload)
        if (!requestResult.isOK()) return requestResult.toCloudFunctionResults()
        return when (val x = readServerResponse(client)){
            JoozdlogCommsKeywords.OK -> ServerFunctionResult.OK
            JoozdlogCommsKeywords.UNKNOWN_USER_OR_PASS -> ServerFunctionResult.UNKNOWN_USER_OR_PASS
            null -> {
                if (client.alive) ServerFunctionResult.SERVER_ERROR
                else ServerFunctionResult.CLIENT_ERROR
            }
            else -> {
                Log.w("testLogin", "Server responded unexpected \"$x\"")
                ServerFunctionResult.UNKNOWN_REPLY_FROM_SERVER
            } // for debugging, server responded something unexpected
        }
    }

    /**
     * Request a username from server. This should get a bare string as reply, which will be the username to return.
     * Will return username, or null if client error
     */
    suspend fun requestUsername(client: Client): String?{
        client.sendRequest(JoozdlogCommsKeywords.REQUEST_NEW_USERNAME)
        return readServerResponse(client)
    }

    /**
     * Ask server to create a new account.
     * @param name: Username
     * @param key: Key
     */
    suspend fun createNewAccount(client: Client, name: String, key: ByteArray): CloudFunctionResult {
        val payLoad = LoginData(name, key, BasicFlight.VERSION.version).serialize()
        client.sendRequest(JoozdlogCommsKeywords.NEW_ACCOUNT, payLoad)
        val result = client.readFromServer()
        Log.d(TAG, "Result was ${result?.toString(Charsets.UTF_8)}")
        return handleServerResult(readServerResponse(client))
    }




    /**
     * send an email confirmation string to server
     */
    suspend fun confirmEmail(client: Client, confirmationString: String): ServerFunctionResult {
        val payload = wrap(confirmationString)
        client.sendRequest(JoozdlogCommsKeywords.CONFIRM_EMAIL, payload).let {
            if (!it.isOK()) return it.toCloudFunctionResults()
        }
        return when(readServerResponse(client)){
            null -> ServerFunctionResult.CLIENT_ERROR
            JoozdlogCommsKeywords.OK -> ServerFunctionResult.OK
            JoozdlogCommsKeywords.UNKNOWN_USER_OR_PASS -> ServerFunctionResult.UNKNOWN_USER_OR_PASS
            JoozdlogCommsKeywords.EMAIL_NOT_KNOWN_OR_VERIFIED -> ServerFunctionResult.EMAIL_DOES_NOT_MATCH
            else -> ServerFunctionResult.UNKNOWN_REPLY_FROM_SERVER
        }
    }




    /**
     * Change password on a logged-in account
     * @param client: The [Client] to use
     * @param newPassword: The new password to set. (this only works when logged in so no need for old pass)
     * @param email: Email to send new login link to.
     * @return [ServerFunctionResult]:
     *  [ServerFunctionResult.OK] if OK
     *  [ServerFunctionResult.NOT_LOGGED_IN] if not logged in before doing this
     *  [ServerFunctionResult.SERVER_ERROR] if server reported having an error
     *  [ServerFunctionResult.CLIENT_ERROR] if client died during this
     *  [ServerFunctionResult.CLIENT_NOT_ALIVE] if client died before this
     *  [ServerFunctionResult.UNKNOWN_REPLY_FROM_SERVER] if server gave an unexpected reply
     */
    suspend fun changePassword(client: Client, newPassword: ByteArray, email: String): ServerFunctionResult {
        val payload = LoginDataWithEmail("", newPassword, 0, email).serialize() // username and basicFlightVersion are unused in this function
        client.sendRequest(JoozdlogCommsKeywords.UPDATE_PASSWORD, payload).let{
            if (!it.isOK()) return it.toCloudFunctionResults()
        }
        client.readFromServer().let {
            return when(val result = it?.toString(Charsets.UTF_8)){
                JoozdlogCommsKeywords.OK -> ServerFunctionResult.OK
                JoozdlogCommsKeywords.NOT_LOGGED_IN -> ServerFunctionResult.NOT_LOGGED_IN
                JoozdlogCommsKeywords.SERVER_ERROR -> ServerFunctionResult.SERVER_ERROR
                null -> {
                    if (client.alive) ServerFunctionResult.CLIENT_ERROR
                    else ServerFunctionResult.CLIENT_NOT_ALIVE
                }
                else -> ServerFunctionResult.UNKNOWN_REPLY_FROM_SERVER.also{
                    Log.w(this::class.simpleName, "Unexpected reply from server: $result")
                } // generic server error
            }
        }
    }

    /**
     * Runs listsner f with a 0-100 percentage completed value
     */
    suspend fun requestFlightsSince(client: Client, timeStamp: Long, f: (Int) -> Unit = {}): List<Flight>?{
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
     * @return [ServerFunctionResult]:
     *  [ServerFunctionResult.OK] if OK
     *  [ServerFunctionResult.NOT_LOGGED_IN] if not logged in before doing this
     *  [ServerFunctionResult.SERVER_ERROR] if server reported having an error
     *  [ServerFunctionResult.CLIENT_ERROR] if client died during this
     *  [ServerFunctionResult.CLIENT_NOT_ALIVE] if client died before this
     *  [ServerFunctionResult.UNKNOWN_REPLY_FROM_SERVER] if server gave an unexpected reply
     */
    suspend fun sendFlights(client: Client, flightsToSend: List<Flight>): ServerFunctionResult {
        client.sendRequest(
            JoozdlogCommsKeywords.SENDING_FLIGHTS,
            packSerialized(flightsToSend.map {it.toBasicFlight().serialize() }))
        return when (val result = readServerResponse(client)){
            JoozdlogCommsKeywords.OK -> ServerFunctionResult.OK
            JoozdlogCommsKeywords.SERVER_ERROR -> ServerFunctionResult.SERVER_ERROR
            JoozdlogCommsKeywords.NOT_LOGGED_IN -> ServerFunctionResult.NOT_LOGGED_IN
            null -> {
                if (client.alive) ServerFunctionResult.CLIENT_ERROR
                else ServerFunctionResult.CLIENT_NOT_ALIVE
            }
            else -> ServerFunctionResult.UNKNOWN_REPLY_FROM_SERVER.also{
                Log.w(this::class.simpleName, "Unexpected reply from server: $result")
            } // generic server error
        }
    }

    /**
     * Send a timestamp to server
     */
    suspend fun sendTimeStamp(client: Client, timeStamp: Long): ServerFunctionResult =
        client.sendRequest(JoozdlogCommsKeywords.ADD_TIMESTAMP, wrap(timeStamp)).toCloudFunctionResults()

    /**
     * Send feedback to server
     * @return [ServerFunctionResult]:
     *  [ServerFunctionResult.OK] if OK
     *  [ServerFunctionResult.SERVER_ERROR] if server reported having an error
     *  [ServerFunctionResult.CLIENT_ERROR] if client died during this
     *  [ServerFunctionResult.CLIENT_NOT_ALIVE] if client died before this
     *  [ServerFunctionResult.UNKNOWN_REPLY_FROM_SERVER] if server gave an unexpected reply
     */
    suspend fun sendFeedback(client: Client, feedbackData: FeedbackData): ServerFunctionResult {
        Log.d(this::class.simpleName, "SendFeedback $client / $feedbackData")
        client.sendRequest(JoozdlogCommsKeywords.SENDING_FEEDBACK, feedbackData.serialize())
        return when (val result = readServerResponse(client)){
            JoozdlogCommsKeywords.OK -> ServerFunctionResult.OK
            JoozdlogCommsKeywords.SERVER_ERROR -> ServerFunctionResult.SERVER_ERROR
            null -> {
                if (client.alive) ServerFunctionResult.CLIENT_ERROR
                else ServerFunctionResult.CLIENT_NOT_ALIVE
            }
            else -> ServerFunctionResult.UNKNOWN_REPLY_FROM_SERVER.also{
                Log.w(this::class.simpleName, "Unexpected reply from server: $result")
            } // generic server error
        }
    }

    private suspend fun readServerResponse(client: Client) =
        client.readFromServer()?.toString(Charsets.UTF_8)

    suspend fun save(client: Client): ServerFunctionResult =
        client.sendRequest(JoozdlogCommsKeywords.SAVE_CHANGES).toCloudFunctionResults()

    /**
     * Make a key from password in the same way it is done in Preferences, for checking purposes
     */
    private fun makeKey(password: String): ByteArray = with (MessageDigest.getInstance("MD5")) {
        update(password.toByteArray())
        digest()
    }



     */

}