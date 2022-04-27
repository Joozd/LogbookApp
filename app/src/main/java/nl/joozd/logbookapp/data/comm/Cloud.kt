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

import android.util.Log
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.withContext
import nl.joozd.comms.Client
import nl.joozd.joozdlogcommon.AircraftType
import nl.joozd.joozdlogcommon.FeedbackData
import nl.joozd.joozdlogcommon.ForcedTypeData
import nl.joozd.logbookapp.data.dataclasses.Airport
import nl.joozd.logbookapp.data.repository.airportrepository.AirportRepository
import nl.joozd.logbookapp.data.repository.flightRepository.FlightRepositoryWithDirectAccess
import nl.joozd.logbookapp.data.sharedPrefs.BackupPrefs
import nl.joozd.logbookapp.model.dataclasses.Flight

import nl.joozd.logbookapp.data.sharedPrefs.Prefs
import nl.joozd.logbookapp.data.sharedPrefs.errors.ScheduledErrors
import nl.joozd.logbookapp.data.utils.Encryption
import nl.joozd.logbookapp.exceptions.NotAuthorizedException
import nl.joozd.logbookapp.utils.CastFlowToMutableFlowShortcut
import nl.joozd.logbookapp.utils.DispatcherProvider
import nl.joozd.logbookapp.utils.TimestampMaker
import java.time.Instant

/**
 * Cloud will take care of all things happening in the cloud.
 * It consists of a number of functions, that will communicate with
 * the JoozdlogServer and take care of everything that happens
 */

object Cloud {
    const val name = "JoozdLogCloud object"

    //This will emit [false] if server tells us login data are not valid
    val loginDataValidFlow: Flow<Boolean> = MutableStateFlow(true)
    private var loginDataValid: Boolean by CastFlowToMutableFlowShortcut(loginDataValidFlow)


    /**********************************************************************************************
     * Utility functions
     **********************************************************************************************/

    suspend fun getTime(): Long? =
        Client.getInstance().use { client ->
            ServerFunctions.getTimestamp(client)
        }


    /**
     * Creates a new user
     * Calling function should consider storing username and pasword in [Prefs]
     * @return @see [ServerFunctions.createNewAccount]
     */
    suspend fun createNewUser(name: String, key: ByteArray): CloudFunctionResults =
        Client.getInstance().use {
            ServerFunctions.createNewAccount(it, name, key)
        }


    /**
     * Creates a new user
     * Calling function should consider storing username and pasword in [Prefs]
     */
    suspend fun createNewUser(name: String, password: String): CloudFunctionResults =
        Client.getInstance().use {
            ServerFunctions.createNewAccount(it, name, Encryption.md5Hash(password))

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
    suspend fun sendNewEmailAddress(): CloudFunctionResults =
        Client.getInstance().use { client ->
            ServerFunctions.sendNewEmailData(client, Prefs.emailAddress)
        }



    /**
     * Confirm email address by sending hash to server
     */
    suspend fun confirmEmail(confirmationString: String): CloudFunctionResults {
        require(":" in confirmationString) { "A confirmationString must have a \':\' in it" }
        return withContext(DispatcherProvider.io()) {
            Client.getInstance().use { client ->
                ServerFunctions.confirmEmail(client, confirmationString)
            }
        }
    }

    /**
     * Request a login link.
     * @return true if login link is emailed, false if someting else happened.
     */
    suspend fun requestLoginLinkMail(): CloudFunctionResults =
        Client.getInstance().use { client ->
            ServerFunctions.requestLoginLinkMail(client).also {
                when (it) {
                    CloudFunctionResults.OK -> {
                        Prefs.emailJobsWaiting.sendLoginLink = false
                    }
                    CloudFunctionResults.EMAIL_DOES_NOT_MATCH -> {
                        Prefs.emailVerified =
                            false // error dialogs etc will be handled by calling function
                        Prefs.emailJobsWaiting.sendLoginLink = true
                    }
                    else -> Log.w("requestLoginLinkMail()", "unhandled result $it")
                }
            }
        }


    /**
     * Send pending email jobs to server, remove them from pending jobs when successful
     */
    suspend fun sendPendingEmailJobs() {
        Prefs.emailJobsWaiting.forEach {
            it()
        }
    }


    /**
     * Changes a user's password
     * Calling function should consider storing username and password in [Prefs]
     * For returns see [ServerFunctions.login] and [ServerFunctions.changePassword]
     */
    suspend fun changePassword(newPassword: String, email: String?): CloudFunctionResults =
        Client.getInstance().use { client ->
            ServerFunctions.login(client).let {
                if (!it.isOK()) return it.also {
                    Log.w("changePassword", "Incorrect login credentials given")
                }
            }
            ServerFunctions.changePassword(client, Encryption.md5Hash(newPassword), email ?: "")
        }

    /**
     * Check username / pass
     * ServerFunctions.testLogin returns 1 if success, 2 if failed, negative value if connection failed
     * @return true if correct, false if incorrect, null if unexpected response, server error or no connection
     */
    suspend fun checkUser(username: String, password: String): CloudFunctionResults =
        Client.getInstance().use {
            ServerFunctions.testLogin(it, username, password)
        }


    suspend fun checkUser(): CloudFunctionResults =
        Client.getInstance().use {
            ServerFunctions.login(it)
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
    suspend fun checkUserFromLink(username: String, password: String): CloudFunctionResults =
        Client.getInstance().use {
            ServerFunctions.testLoginFromLink(it, username, password)
        }


    /**
     * This will send a mail with all flights currently in cloud.
     * Flights will be synced first just to be sure things are correct.
     */
    suspend fun requestBackupEmail(): CloudFunctionResults {
        syncAllFlights()
        return Client.getInstance().use { client ->
            ServerFunctions.login(client)

            val result = ServerFunctions.requestBackup(client)
            when (result) {
                CloudFunctionResults.OK -> {
                    Prefs.emailJobsWaiting.sendBackupCsv = false
                    BackupPrefs.mostRecentBackup = Instant.now().epochSecond
                    BackupPrefs.backupIgnoredUntil = 0
                }
                CloudFunctionResults.EMAIL_DOES_NOT_MATCH -> {
                    Prefs.emailVerified = false
                    Prefs.emailJobsWaiting.sendBackupCsv = true
                }
                else -> Log.w("requestBackup()", "unhandled result $result")
            }
            result
        }
    }


    /**********************************************************************************************
     * Airport sync functions
     **********************************************************************************************/

    suspend fun getServerAirportDbVersion(): Int =
        Client.getInstance().use { server ->
            ServerFunctions.getAirportDbVersion(server) // no need to handle errors as negative values won't be higher than available ones
        }


    // returns List<BasicAirport>
    suspend fun downloadAirportsDatabase(listener: (Int) -> Unit = {}): CloudFunctionResults {
        val serverDbVersion = getServerAirportDbVersion()
        when (serverDbVersion) {
            -1 -> return CloudFunctionResults.SERVER_ERROR
            -2 -> return CloudFunctionResults.CLIENT_ERROR
        }
        val airports =Client.getInstance().use {
            ServerFunctions.getAirports(it, listener)

        }?.map { Airport(it) } ?: return CloudFunctionResults.SERVER_ERROR

        AirportRepository.instance.replaceDbWith(airports)
        Prefs.airportDbVersion = serverDbVersion

        return CloudFunctionResults.OK
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

    suspend fun getAircraftTypes(listener: (Int) -> Unit = {}): List<AircraftType>? =
        Client.getInstance().use {
            ServerFunctions.getAircraftTypes(it, listener)
        }


    suspend fun getForcedTypes(listener: (Int) -> Unit = {}): List<ForcedTypeData>? =
        Client.getInstance().use {
            ServerFunctions.getForcedTypes(it, listener)
        }


    suspend fun getAircraftTypesVersion(listener: (Int) -> Unit = {}): Int? =
        Client.getInstance().use {
            ServerFunctions.getAircraftTypesVersion(it, listener).also {
                Log.d("aircraftTypes", "version: $it")
            }
        }


    suspend fun getForcedAircraftTypesVersion(listener: (Int) -> Unit = {}): Int? =
        Client.getInstance().use {
            ServerFunctions.getForcedAircraftTypesVersion(it, listener)
        }



    /**********************************************************************************************
     * Flights sync functions
     **********************************************************************************************/

    /**
     * @return timestamp on success,
     *         -1 on critical fail (ie wrong credentials),
     *         null on server error (retry later)
     */
    suspend fun syncAllFlights(flightRepository: FlightRepositoryWithDirectAccess = FlightRepositoryWithDirectAccess.instance): Long? {
        TimestampMaker().getAndSaveTimeOffset() ?: return null
        val timeStamp = TimestampMaker().nowForSycPurposes
        return Client.getInstance().use { server ->
            with(ServerFunctions) {
                loginAndHandleIfThatFails(server) ?: return -1L
                val newFlightsFromServer =
                    getNewFlightsFromServer(server)
                        ?: return -1L
                var allFlightsInDB = flightRepository.getAllFlightsInDB()

                //fix possible flightID conflicts
                val highestTakenIDFromServer =
                    newFlightsFromServer.maxOfOrNull { it.flightID } ?: Int.MIN_VALUE
                val flightsUnknownToServer = allFlightsInDB.filter { it.unknownToServer }
                if (flightsUnknownToServer.any { it.flightID <= highestTakenIDFromServer }) {
                    //fixing flights also updates timestamps for them.
                    fixFlightsUnknownToServerIDs(
                        flightsUnknownToServer,
                        highestTakenIDFromServer
                    )
                    //update list since we just changed some flights in it.
                    allFlightsInDB = flightRepository.getAllFlightsInDB()
                }

                // changing all timestamps to now means that editing flights on two devices
                // before syncing will stick to most recent sync, not most recent edit)
                val flightsToSend =
                    getFlightsToSendToServerAndSetTimestamp(allFlightsInDB, timeStamp)

                //send the flights to server, retry on fail as login worked earlier
                if (!sendFlights(server, flightsToSend).isOK()) return null
                //add timestamp to this transaction
                if (!sendTimeStamp(server, timeStamp).isOK()) return null
                //save changes on server
                if (!save(server).isOK()) return null.also {
                    Log.d("Cloud", "save returned null")
                }

                //mark time of this successful sync
                Prefs.lastUpdateTime = timeStamp

                //Save flights with current timestamps and clear `unknownToServer` flags
                flightRepository.save(flightsToSend.map { it.copy(unknownToServer = false) } + newFlightsFromServer)

                timeStamp
            }
        }
    }



    private fun getFlightsToSendToServerAndSetTimestamp(
        allFlightsInDB: List<Flight>,
        timeStamp: Long
    ) = allFlightsInDB.filter {
        (it.timeStamp > Prefs.lastUpdateTime || it.unknownToServer)
    }
        .filter { !it.isPlanned || !it.unknownToServer } // don't send planned flights unless server knows about them somehow
        .map { it.copy(timeStamp = timeStamp, unknownToServer = false) }


    /*
     * Updates flightnumbers that conflict with freshly downloaded flights
     * Retrieve them again from DB to get updated flights.
     */
    private suspend fun fixFlightsUnknownToServerIDs(flights: List<Flight>, highestTakenID: Int){
        val repo = FlightRepositoryWithDirectAccess.instance
        val fixedFlights = flights.map {
            it.copy (flightID = repo.generateAndReserveNewFlightID(highestTakenID))
        }
        repo.deleteHard(flights)
        repo.save(fixedFlights)
    }

    /**
     * Handle login result
     * @return Unit if login OK, null if not.
     * If login failed due to bad login data, set flag
     */
    private suspend fun loginAndHandleIfThatFails(server: Client): Unit?{
        return when (val result = ServerFunctions.login(server)) {
            CloudFunctionResults.OK -> Unit
            CloudFunctionResults.UNKNOWN_USER_OR_PASS, CloudFunctionResults.NOT_LOGGED_IN -> {
                this.loginDataValid = false
                null
            }
            else -> {
                Log.w("syncAllFlights", "Failed, result was $result")
                null
            }
        }
    }

    /**
     * Get flights from [server]
     */
    private suspend fun getNewFlightsFromServer(server: Client): List<Flight>? =
        try {
            ServerFunctions.requestFlightsSince(server, Prefs.lastUpdateTime)
                ?.filter{ !it.isPlanned }
        } catch (e: NotAuthorizedException) {
            // should have been checked earlier but might as well check it here as well
            loginDataValid = false
            null
        }

    suspend fun sendFeedback(feedback: String, contactInfo: String): CloudFunctionResults =
        Client.getInstance().use{ client ->
            val feedbackData = FeedbackData(feedback, contactInfo)
            ServerFunctions.sendFeedback(client, feedbackData)
        }

}
