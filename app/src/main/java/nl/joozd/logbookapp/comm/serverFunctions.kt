package nl.joozd.logbookapp.comm

//These functions know where Preferences such as login data or email addresses can be found.

import android.util.Log
import kotlinx.coroutines.withContext
import nl.joozd.joozdlogcommon.FeedbackData
import nl.joozd.logbookapp.core.TaskFlags
import nl.joozd.logbookapp.core.messages.MessagesWaiting
import nl.joozd.logbookapp.core.usermanagement.UsernameWithKey
import nl.joozd.logbookapp.core.usermanagement.UserManagement
import nl.joozd.logbookapp.core.usermanagement.checkConfirmationString
import nl.joozd.logbookapp.data.sync.FlightsSynchronizer
import nl.joozd.logbookapp.data.repository.aircraftrepository.AircraftRepository
import nl.joozd.logbookapp.data.repository.airportrepository.AirportRepository
import nl.joozd.logbookapp.data.repository.flightRepository.FlightRepositoryWithDirectAccess
import nl.joozd.logbookapp.data.sharedPrefs.DataVersions
import nl.joozd.logbookapp.data.sharedPrefs.ServerPrefs
import nl.joozd.logbookapp.data.sharedPrefs.Prefs
import nl.joozd.logbookapp.data.sharedPrefs.TaskPayloads
import nl.joozd.logbookapp.extensions.nullIfBlank
import nl.joozd.logbookapp.utils.DispatcherProvider
import nl.joozd.logbookapp.utils.TimestampMaker
import nl.joozd.logbookapp.utils.generateKey

/**
 * This will invalidate current login data.
 * - Asks a username from server
 * - generates a password
 * - Creates a new user on server with those data.
 * - On success, saves the data, requests a confirmation email and resets TaskFlag.
 * @return success() if new data received and saved, retry() if connection error, failure() if server refused.
 */
suspend fun generateNewUserAndCreateOnServer(cloud: Cloud = Cloud()): ServerFunctionResult =
    cloud.requestUsername()?.let { n ->
        val loginData = UsernameWithKey(n, generateKey())
        when (createNewUserOnServer(loginData, cloud)) {
                ServerFunctionResult.SUCCESS -> {
                    saveLoginDataAsNewUser(loginData)
                    TaskFlags.createNewUserAndEnableCloud(false)
                    Prefs.useCloud(true)
                    MessagesWaiting.newCloudAccountCreated(true)
                    ServerFunctionResult.SUCCESS
                }
                ServerFunctionResult.RETRY -> ServerFunctionResult.RETRY
                ServerFunctionResult.FAILURE -> generateNewUserAndCreateOnServer(cloud)
            }
        } ?: ServerFunctionResult.RETRY

private suspend fun saveLoginDataAsNewUser(loginData: UsernameWithKey) {
    withContext(DispatcherProvider.io()) { storeLoginData(loginData.username, loginData.key) } // blocking is OK in this context
    resetEmailData()
}


/**
 * This will send a new email address to server
 * - Will attempt to get login data
 * - Will attempt to get email address
 * - Sends email to server with current login data
 * - On success, sets EmailVerified to false and resets TaskFlag.
 */
suspend fun updateEmailAddressOnServer(cloud: Cloud = Cloud(), userManagement: UserManagement = UserManagement()): ServerFunctionResult =
    userManagement.getUsernameWithKey()?.let { loginData ->
        getEmailAddressFromPrefs()?.let{ emailAddress ->
            sendEmailAddressToServer(loginData, emailAddress, cloud).also{
                if (it.isOK()) {
                    ServerPrefs.emailVerified(false)
                    TaskFlags.updateEmailWithServer(false)
                }
            }
        }
    } ?: ServerFunctionResult.FAILURE

/**
 * This will confirm email confirmation code with server
 * - Will attempt to get login data
 * - will send code to server, login data is needed to check confirmation code. If code check fails, will throw Exception.
 *  --- CHECK CONFIRMATION STRING IN CALLING FUNCTION (use [checkConfirmationString])
 * - On success, sets EmailVerified to true, removes stored confirmation string and resets TaskFlag.
 */
suspend fun confirmEmail(confirmationString: String, cloud: Cloud = Cloud()): ServerFunctionResult =
    sendEmailConfirmationCode(confirmationString, cloud).also{
        if (it == ServerFunctionResult.SUCCESS) withContext (DispatcherProvider.io()) {
            resetEmailCodeVerificationFlag()
            ServerPrefs.emailVerified(true) // blocking is OK in this context
        }
    }

/**
 * This will request a backup email from the server.
 * - Ask Cloud to send a request for a backup email
 * - will send code to server, login data is needed to check confirmation code. If code check fails, will throw Exception.
 *  --- CHECK CONFIRMATION STRING IN CALLING FUNCTION (use [checkConfirmationString])
 * - On success, sets EmailVerified to true, removes stored confirmation string and resets TaskFlag.
 */
suspend fun requestBackupMail(cloud: Cloud = Cloud(), userManagement: UserManagement = UserManagement()): ServerFunctionResult =
    userManagement.getUsernameWithKey()?.let { uk ->
        getEmailAddressFromPrefs()?.let { email ->
            cloud.requestBackupEmail(uk.username, uk.key, email).correspondingServerFunctionResult().also{
                if(it.isOK())
                    TaskFlags.sendBackupEmail(false)
            }
        }
    } ?: ServerFunctionResult.FAILURE

/**
 * This will request a Login Link email from the server.
 * - Ask Cloud to send a request for a backup email
 * - will send code to server, login data is needed to check confirmation code. If code check fails, will throw Exception.
 *  --- CHECK CONFIRMATION STRING IN CALLING FUNCTION (use [checkConfirmationString])
 * - On success, sets EmailVerified to true, removes stored confirmation string and resets TaskFlag.
 */
suspend fun requestLoginLinkEmail(cloud: Cloud = Cloud(), userManagement: UserManagement = UserManagement()): ServerFunctionResult =
    userManagement.getUsernameWithKey()?.let { uk ->
        getEmailAddressFromPrefs()?.let { email ->
            cloud.requestLoginLinkMail(uk.username, uk.key, email).correspondingServerFunctionResult().also{
                if(it.isOK())
                    TaskFlags.sendLoginLink(false)
            }
        }
    } ?: ServerFunctionResult.FAILURE

/**
 * Get time from server, or not.
 */
suspend fun getTimeFromServer(cloud: Cloud = Cloud()): Long? =
    cloud.getTime()

/**
 * Update data files if needed
 * Only returns SUCCESS or RETRY
 */
suspend fun updateDataFiles(server: HTTPServer,
                            aircraftRepository: AircraftRepository = AircraftRepository.instance,
                            airportRepository: AirportRepository = AirportRepository.instance
): ServerFunctionResult {
    val metaData = server.getDataFilesMetaData() ?: return ServerFunctionResult.RETRY

    if(metaData.aircraftTypesVersion > DataVersions.aircraftTypesVersion()){
        aircraftRepository.updateAircraftTypes(server.getAircraftTypes(metaData) ?: return ServerFunctionResult.RETRY)
        DataVersions.aircraftTypesVersion(metaData.aircraftTypesVersion)
    }
    if(metaData.aircraftForcedTypesVersion > DataVersions.aircraftForcedTypesVersion()){
        aircraftRepository.updateForcedTypes(server.getForcedTypes(metaData) ?: return ServerFunctionResult.RETRY)
        DataVersions.aircraftForcedTypesVersion(metaData.aircraftForcedTypesVersion)
    }

    if(metaData.airportsVersion > DataVersions.airportsVersion()){
        airportRepository.updateAirports(server.getAirports(metaData) ?: return ServerFunctionResult.RETRY)
        DataVersions.airportsVersion(metaData.airportsVersion)
    }

    return ServerFunctionResult.SUCCESS
}

suspend fun syncFlights(
    server: Cloud = Cloud(),
    userManagement: UserManagement = UserManagement(),
    repository: FlightRepositoryWithDirectAccess = FlightRepositoryWithDirectAccess.instance
): ServerFunctionResult =
    FlightsSynchronizer(server, userManagement, repository).synchronizeIfNotSynced().also{
        if (it.isOK()){
            TaskFlags.syncFlights(false)
        }
    }

// Note to self: If this fails halfway (e.g. server conenction drops before re-uploading all flights), next try will not update anything, still re-upload all files and report OK
suspend fun mergeFlightsWithServer(
    server: Cloud = Cloud(),
    userManagement: UserManagement = UserManagement(),
    repository: FlightRepositoryWithDirectAccess = FlightRepositoryWithDirectAccess.instance
): ServerFunctionResult =
    FlightsSynchronizer(server, userManagement, repository).mergeRepoWithServer().also{
        if (it.isOK()){
            TaskFlags.mergeAllDataFromServer(false)
            ServerPrefs.mostRecentFlightsSyncEpochSecond(TimestampMaker().nowForSycPurposes)
            MessagesWaiting.mergeWithServerPerformed(true)
        }
    }

suspend fun sendFeedback(cloud: Cloud = Cloud()): ServerFunctionResult =
    (TaskPayloads.feedbackWaiting().nullIfBlank()?.let{ feedback ->
        cloud.sendFeedback(FeedbackData(feedback, TaskPayloads.feedbackContactInfoWaiting()))
            .correspondingServerFunctionResult()
    }  ?: ServerFunctionResult.SUCCESS)
        .also{ if (it.isOK()) TaskFlags.feedbackWaiting(false) }


private suspend fun sendEmailConfirmationCode(confirmationString: String, cloud: Cloud): ServerFunctionResult {
    // If bad data is sent to server (server cannot discern between malformed data in transport or malformed data because bad data was sent)
    // we will get into an endless loop, se we check if data is parsable by server here.
    require(checkConfirmationString(confirmationString)) { "Bad confirmation string $confirmationString received, this should have been checked by calling function" }
    return cloud.confirmEmail(confirmationString).correspondingServerFunctionResult().also {
        if (it.isOK())
            resetEmailCodeVerificationFlag()
    }
}

private fun resetEmailCodeVerificationFlag() {
    TaskFlags.verifyEmailCode(false)
    TaskPayloads.emailConfirmationStringWaiting("")
}

private suspend fun sendEmailAddressToServer(loginData: UsernameWithKey, emailAddress: String, cloud: Cloud): ServerFunctionResult =
    //NOTE this does NOT save the email address nor does any other marking, flagging or anything.
    cloud.sendNewEmailAddress(loginData.username, loginData.key, emailAddress).correspondingServerFunctionResult()

private suspend fun createNewUserOnServer(loginData: UsernameWithKey, cloud: Cloud): ServerFunctionResult =
    cloud.createNewUser(loginData.username, loginData.key).correspondingServerFunctionResult()
    // FAILURE only happens in very rare case when a retry if



// If there is no email address stored, this will handle it (eg. prompt user to enter email address so requested calling function can be performed)
private suspend fun getEmailAddressFromPrefs(): String? =
    ServerPrefs.emailAddress().ifBlank {
        // Fallback handling of no email address entered when one is needed.
        // Any functions being called should only be triggered when an emal address is entered.
        // Therefore, this should only happen after user triggered an action in a way unforeseen at the time of this writing
        Log.e("getEmailAddressFromPrefs", "getEmailAddressFromPrefs() was called but an EmailPrefs.emailAddress is blank. User is notified to fix this.")
        MessagesWaiting.noEmailEntered(true) // this will trigger display of "no email entered" message to user.
        ServerPrefs.emailVerified(false)

        null
    }

private fun resetEmailData() {
    ServerPrefs.emailVerified(false)
    UserManagement().requestEmailVerificationMail()
}

private suspend fun storeLoginData(username: String, key: ByteArray, userManagement: UserManagement = UserManagement()) {
    userManagement.storeNewLoginData(username, key)
    Prefs.username = username
    Prefs.key = key
}

