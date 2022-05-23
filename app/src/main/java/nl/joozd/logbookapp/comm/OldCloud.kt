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

package nl.joozd.logbookapp.comm

/**
 * Cloud will take care of all things happening in the cloud.
 * It consists of a number of functions, that will communicate with
 * the JoozdlogServer and take care of everything that happens
 */

@Deprecated("use Cloud")
object OldCloud {
    const val name = "JoozdLogCloud object"
    /*

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
     * Calling function should store username and password in [Prefs]
     * @return a CloudFunctionResult.
     */
    suspend fun createNewUser(name: String, key: ByteArray) =
        Client.getInstance().use {
            withContext(DispatcherProvider.io()) {
                ServerFunctions.createNewAccount(it, name, key)
            }
        }



    /**
     * Request a login link.
     * @return true if login link is emailed, false if someting else happened.
     */
    suspend fun requestLoginLinkMail(): ServerFunctionResult =
        Client.getInstance().use { client ->
            ServerFunctions.requestLoginLinkMail(client).also {
                when (it) {
                    ServerFunctionResult.OK -> {
                        TaskFlags.sendLoginLink = false
                    }
                    ServerFunctionResult.EMAIL_DOES_NOT_MATCH -> {
                        EmailPrefs.emailVerified = false    // error dialogs etc will be handled by calling function
                        TaskFlags.sendLoginLink = true      // when new email verified, send login link.
                    }
                    else -> Log.w("requestLoginLinkMail()", "unhandled result $it")
                }
            }
        }

    /**
     * Changes a user's password
     * Calling function should consider storing username and password in [Prefs]
     * For returns see [ServerFunctions.login] and [ServerFunctions.changePassword]
     */
    suspend fun changePassword(newPassword: String, email: String?): ServerFunctionResult =
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
    suspend fun checkUser(username: String, password: String): ServerFunctionResult =
        Client.getInstance().use {
            ServerFunctions.testLogin(it, username, password)
        }


    suspend fun checkUser(): ServerFunctionResult =
        Client.getInstance().use {
            ServerFunctions.login(it)
        }

    /**
     * Check username / pass
     * @return [ServerFunctionResult]:
     *  [ServerFunctionResult.OK] if logged in OK
     *  [ServerFunctionResult.UNKNOWN_USER_OR_PASS] if server rejected login data. In this case, an error to be shown to user will be scheduled through [ScheduledErrors.addError]
     *  [ServerFunctionResult.CLIENT_ERROR] if Client got an error (eg. died while receiving data)
     *  [ServerFunctionResult.CLIENT_NOT_ALIVE] if Client died
     *  [ServerFunctionResult.UNKNOWN_REPLY_FROM_SERVER] if server sent an unknown reply
     */
    suspend fun checkUserFromLink(username: String, password: String): ServerFunctionResult =
        Client.getInstance().use {
            ServerFunctions.testLoginFromLink(it, username, password)
        }





    /**********************************************************************************************
     * Airport sync functions
     **********************************************************************************************/




    // returns List<BasicAirport>
    suspend fun downloadAirportsDatabase(listener: (Int) -> Unit = {}): ServerFunctionResult {
        val serverDbVersion = getServerAirportDbVersion()
        when (serverDbVersion) {
            -1 -> return ServerFunctionResult.SERVER_ERROR
            -2 -> return ServerFunctionResult.CLIENT_ERROR
        }
        val airports =Client.getInstance().use {
            ServerFunctions.getAirports(it, listener)

        }?.map { Airport(it) } ?: return ServerFunctionResult.SERVER_ERROR

        AirportRepository.instance.replaceDbWith(airports)
        Prefs.airportDbVersion = serverDbVersion

        return ServerFunctionResult.OK
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
            ServerFunctionResult.OK -> Unit
            ServerFunctionResult.UNKNOWN_USER_OR_PASS, ServerFunctionResult.NOT_LOGGED_IN -> {
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

    suspend fun sendFeedback(feedback: String, contactInfo: String): ServerFunctionResult =
        Client.getInstance().use{ client ->
            val feedbackData = FeedbackData(feedback, contactInfo)
            ServerFunctions.sendFeedback(client, feedbackData)
        }


     */
}
