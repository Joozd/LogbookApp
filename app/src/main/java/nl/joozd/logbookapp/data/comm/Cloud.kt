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
import kotlinx.coroutines.withContext
import nl.joozd.joozdlogcommon.AircraftType
import nl.joozd.joozdlogcommon.ConsensusData
import nl.joozd.joozdlogcommon.FeedbackData
import nl.joozd.joozdlogcommon.ForcedTypeData
import nl.joozd.logbookapp.data.comm.protocol.Client
import nl.joozd.logbookapp.model.dataclasses.Flight

import nl.joozd.logbookapp.data.sharedPrefs.Preferences
import nl.joozd.joozdlogcommon.exceptions.NotAuthorizedException
import nl.joozd.logbookapp.data.comm.protocol.CloudFunctionResults
import nl.joozd.logbookapp.data.repository.flightRepository.FlightRepository
import nl.joozd.logbookapp.data.sharedPrefs.errors.ScheduledErrors
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
        Client.getInstance().use { client ->
            ServerFunctions.getTimestamp(client)
        }
    }

    /**
     * Creates a new user
     * Calling function should consider storing username and pasword in [Preferences]
     * @return @see [ServerFunctions.createNewAccount]
     */
    suspend fun createNewUser(name: String, key: ByteArray): CloudFunctionResults = withContext(Dispatchers.IO) {
        Client.getInstance().use {
            ServerFunctions.createNewAccount(it, name, key)
        }
    }

    /**
     * Creates a new user
     * Calling function should consider storing username and pasword in [Preferences]
     */
    suspend fun createNewUser(name: String, password: String): CloudFunctionResults = withContext(Dispatchers.IO) {
        Client.getInstance().use {
            ServerFunctions.createNewAccount(it, name, Encryption.md5Hash(password))
        }
    }

    /**
     * Requests a new username from server
     */
    suspend fun requestUsername(): String? =
        Client.getInstance().use { ServerFunctions.requestUsername(it) }

    /**
     * Send new email address to server.
     * Function will return [CloudFunctionResults.NO_LOGIN_DATA] if no login data present.
     * Server will:
     * - check if user is logged in, otherwise [CloudFunctionResults.UNKNOWN_USER_OR_PASS]
     * - Save a hash of that email and send a confirmation mail. If that fails due bad email address we get [CloudFunctionResults.NOT_A_VALID_EMAIL_ADDRESS]
     * - Generic server errors will be [CloudFunctionResults.SERVER_ERROR] or [CloudFunctionResults.UNKNOWN_REPLY_FROM_SERVER]
     * - Client errors will be [CloudFunctionResults.CLIENT_ERROR]
     */
    suspend fun sendNewEmailAddress(): CloudFunctionResults = withContext(Dispatchers.IO) {
        Client.getInstance().use { client ->
            ServerFunctions.sendNewEmailData(client, Preferences.emailAddress)
        }
    }


    /**
     * Confirm email address by sending hash to server
     */
    suspend fun confirmEmail(confirmationString: String): CloudFunctionResults = withContext(Dispatchers.IO) {
        require (":" in confirmationString) { "A confirmationString must have a \':\' in it"}
        Client.getInstance().use{ client ->
            ServerFunctions.confirmEmail(client, confirmationString)
        }
    }

    /**
     * Request a login link.
     * @return true if login link is emailed, false if someting else happened.
     */
    suspend fun requestLoginLinkMail(): CloudFunctionResults = withContext(Dispatchers.IO) {
        Client.getInstance().use { client ->
            ServerFunctions.requestLoginLinkMail(client).also{
                when(it){
                    CloudFunctionResults.OK -> {
                        Preferences.emailJobsWaiting.sendLoginLink = false
                    }
                    CloudFunctionResults.EMAIL_DOES_NOT_MATCH -> {
                        Preferences.emailVerified = false // error dialogs etc will be handled by calling function
                        Preferences.emailJobsWaiting.sendLoginLink = true
                    }
                    else -> Log.w("requestLoginLinkMail()", "unhandled result $it")
                }
            }
        }
    }

    /**
     * Send pending email jobs to server, remove them from pending jobs when successful
     * TODO make this an Iteration from EmailJobsWaiting
     */
    suspend fun sendPendingEmailJobs(){
        Preferences.emailJobsWaiting.forEach {
            it()
        }
    }


    /**
     * Changes a user's password
     * Calling function should consider storing username and password in [Preferences]
     * For returns see [ServerFunctions.login] and [ServerFunctions.changePassword]
     */
    suspend fun changePassword(newPassword: String, email: String?): CloudFunctionResults = withContext(Dispatchers.IO) {
        Client.getInstance().use {client ->
            ServerFunctions.login(client).let{
                if (!it.isOK()) return@withContext it.also{
                    Log.w("changePassword","Incorrect login credentials given")
                }
            }
            ServerFunctions.changePassword(client, Encryption.md5Hash(newPassword), email ?: "")
        }
    }




    /**
     * Check username / pass
     * ServerFunctions.testLogin returns 1 if success, 2 if failed, negative value if connection failed
     * @return true if correct, false if incorrect, null if unexpected response, server error or no connection
     */
    suspend fun checkUser(username: String, password: String): CloudFunctionResults =  withContext(Dispatchers.IO) {
        Client.getInstance().use {
            ServerFunctions.testLogin(it, username, password)
        }
    }

    suspend fun checkUser(): CloudFunctionResults =  withContext(Dispatchers.IO) {
        Client.getInstance().use {
            ServerFunctions.login(it)
        }
    }

    /**
     * Check username / pass
     * @return [CloudFunctionResults]:
     *  [CloudFunctionResults.OK] if logged in OK
     *  [CloudFunctionResults.UNKNOWN_USER_OR_PASS] if server rejected login data. In this case, an error to be shown to user will be scheduled through [ScheduledErrors.addError]
     *  [CloudFunctionResults.CLIENT_ERROR] if Client got an error (eg. died while receiving data)
     *  [CloudFunctionResults.CLIENT_NOT_ALIVE] if Client died
     *  [CloudFunctionResults.UNKNOWN_REPLY_FROM_SERVER] if server sent an unknown reply
     */
    suspend fun checkUserFromLink(username: String, password: String): CloudFunctionResults =  withContext(Dispatchers.IO) {
        Client.getInstance().use {
            ServerFunctions.testLoginFromLink(it, username, password)
        }
    }

    suspend fun requestBackup(): CloudFunctionResults = withContext(Dispatchers.IO) {
        Client.getInstance().use {client ->
            ServerFunctions.login(client)
            ServerFunctions.requestBackup(client).also{
                when (it){
                    CloudFunctionResults.OK -> {
                        Preferences.emailJobsWaiting.sendBackupCsv = false
                    }
                    CloudFunctionResults.EMAIL_DOES_NOT_MATCH -> {
                        Preferences.emailVerified = false
                        Preferences.emailJobsWaiting.sendBackupCsv = true
                    }
                    else -> Log.w("requestBackup()", "unhandled result $it")
                }
            }
        }
    }



    /**********************************************************************************************
     * Airport sync functions
     **********************************************************************************************/

    suspend fun getAirportDbVersion(): Int = withContext(Dispatchers.IO) {
        Client.getInstance().use { server ->
            ServerFunctions.getAirportDbVersion(server) // no need to handle errors as negative values won't be higher than available ones
        }
    }

    // returns List<BasicAirport>
    suspend fun getAirports(listener: (Int) -> Unit = {}) = withContext(Dispatchers.IO) {
        Client.getInstance().use {
            ServerFunctions.getAirports(it, listener)
        }
    }

    /**********************************************************************************************
     * Aircraft sync functions
     **********************************************************************************************/

    /**
     * Gets AircraftTypeConsensus from server
     */
    /*
    suspend fun getAircraftConsensus(listener: (Int) -> Unit = {}): List<AircraftTypeConsensusData> {
        TODO("Not Implemented")
    }
    */

    /**
     * Send AircraftConsensus to server
     * Needs a list of [ConsensusData] which will hold all aircraft to be added to consensus as well as all aircraft to be removed from it.
     */
    suspend fun sendAircraftConsensus(consensus: List<ConsensusData>, listener: (Int) -> Unit = {}): Boolean = withContext(Dispatchers.IO) {
        listener(0)
        Client.getInstance().use{
            listener(20)
            ServerFunctions.sendConsensus(it, consensus).also{
                listener(100)
            }
        }
    }

    suspend fun getAircraftTypes(listener: (Int) -> Unit = {}): List<AircraftType>? = withContext(Dispatchers.IO) {
        Client.getInstance().use{
            ServerFunctions.getAircraftTypes(it, listener)
        }
    }

    suspend fun getForcedTypes(listener: (Int) -> Unit = {}): List<ForcedTypeData>? = withContext(Dispatchers.IO) {
        Client.getInstance().use{
            ServerFunctions.getForcedTypes(it, listener)
        }
    }

    suspend fun getAircraftTypesVersion(listener: (Int) -> Unit = {}): Int? = withContext(Dispatchers.IO) {
        Client.getInstance().use{
            ServerFunctions.getAircraftTypesVersion(it, listener).also{
                Log.d("aircraftTypes", "version: $it")
            }
        }
    }

    suspend fun getForcedAircraftTypesVersion(listener: (Int) -> Unit = {}): Int? = withContext(Dispatchers.IO) {
        Client.getInstance().use{
            ServerFunctions.getForcedAircraftTypesVersion(it, listener)
        }
    }

    /**
     * Get a consensus Map (map of Registration to serialized type)
     */
    suspend fun getConsensus(listener: (Int) -> Unit = {}): Map<String, ByteArray>? = withContext(Dispatchers.IO) {
        Client.getInstance().use{
            ServerFunctions.getConsensus(it, listener)
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
            Client.getInstance().use {
                if (!ServerFunctions.login(it).isOK()) {
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
     * @return timestamp on success, -1 on critical fail (ie wrong credentials), null on server error (retry later)
     * Listsner will give an estimated completion percentage
     */
    suspend fun syncAllFlights(flightRepository: FlightRepository, listener: (Int) -> Unit = {}): Long? = try {
        withContext(Dispatchers.IO) f@{
            syncingFlights = true
            listener(0)
            Client.getInstance().use { server ->
                listener(5) // Connection is made!
                with(ServerFunctions) {
                    //sync time with server
                    val timeStamp: Long = getTimestamp(server)?.also {
                        Log.d(TAG, "Got timestamp ${Instant.ofEpochSecond(it)}")
                        listener(10)
                        Preferences.serverTimeOffset = it - Instant.now().epochSecond
                    } ?: return@f null.also { Log.d("Cloud", "getTimeStamp() returned null") } // if no timestamp received, server is not working so might as well quit here

                    //Login and handle if that fails:
                    login(server).let { result ->
                        when (result) {
                            CloudFunctionResults.OK -> Unit
                            CloudFunctionResults.UNKNOWN_USER_OR_PASS, CloudFunctionResults.NOT_LOGGED_IN -> {
                                flightRepository.setNotLoggedInFlag(true)
                                return@f -1L
                            }
                            else -> {
                                Log.w("syncAllFlights", "Failed, result was $result")
                                return@f null
                            }
                        }
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
                        (takenIDs.maxOrNull() ?: -1) + 1 // null on empty list, but empty list means no fixes
                    val fixedNewLocalFlights = newLocalFlights.filter { it.flightID in takenIDs }
                        .mapIndexed { index: Int, flight: Flight ->
                            flight.copy(flightID = lowestFixedID + index)
                        }

                    flightRepository.delete(
                        newLocalFlights.filter { it.flightID in takenIDs },
                        sync = false
                    )
                    flightRepository.save(fixedNewLocalFlights, sync = false, addToUndo = false)

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
                    if (!sendFlights(server, flightsToSend).isOK()) return@f null
                    listener(75)
                    //add timestamp to this transaction
                    if (!sendTimeStamp(server, timeStamp).isOK()) return@f null
                    listener(80)
                    //save changes on server
                    if (!save(server).isOK()) return@f null.also { Log.d("Cloud", "save returned null") }
                    listener(85)

                    //mark time of this successful sync
                    Preferences.lastUpdateTime = timeStamp

                    //Save flights with current timestamps and clear `changed` flags
                    //listsner from 85 to 100

                    flightRepository.save(flightsToSend.map { it.copy(unknownToServer = false) } + newFlightsFromServer,
                        sync = false, addToUndo = false) // { listener(85 + it * 15 / 100) } // TODO Listsner not implemented


                    // Profit!
                    listener(100)
                    timeStamp
                }
            }
        }
    } finally {
        syncingFlights = false
    }

    suspend fun sendFeedback(feedback: String, contactInfo: String): CloudFunctionResults = withContext(Dispatchers.IO) {
        Client.getInstance().use{ client ->
            val feedbackData = FeedbackData(feedback, contactInfo)
            ServerFunctions.sendFeedback(client, feedbackData)
        }
    }
}
