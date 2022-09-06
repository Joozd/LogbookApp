package nl.joozd.logbookapp.comm

//These functions know where Preferences such as login data or email addresses can be found.

import android.util.Log
import kotlinx.coroutines.withContext
import nl.joozd.joozdlogcommon.BackupEmailData
import nl.joozd.joozdlogcommon.FeedbackData
import nl.joozd.logbookapp.core.TaskFlags
import nl.joozd.logbookapp.core.messages.MessagesWaiting
import nl.joozd.logbookapp.core.emailFunctions.UsernameWithKey
import nl.joozd.logbookapp.core.emailFunctions.EmailCenter
import nl.joozd.logbookapp.core.emailFunctions.checkConfirmationString
import nl.joozd.logbookapp.data.export.FlightsRepositoryExporter
import nl.joozd.logbookapp.data.repository.aircraftrepository.AircraftRepository
import nl.joozd.logbookapp.data.repository.airportrepository.AirportRepository
import nl.joozd.logbookapp.data.sharedPrefs.*
import nl.joozd.logbookapp.exceptions.CloudException
import nl.joozd.logbookapp.extensions.nullIfBlank
import nl.joozd.logbookapp.utils.DispatcherProvider
import nl.joozd.logbookapp.utils.generateKey
import java.time.Instant

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
suspend fun updateEmailAddressOnServer(cloud: Cloud = Cloud(), userManagement: EmailCenter = EmailCenter()): ServerFunctionResult =
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

suspend fun sendBackupMailThroughServer(
    cloud: Cloud = Cloud(),
    userManagement: EmailCenter = EmailCenter()
){
    val backupEmailData = generateBackupEmailData(userManagement)
    cloud.sendBackupMailThroughServer(backupEmailData) // if this fails it will throw exception
    TaskFlags.sendBackupEmail(false)
    BackupPrefs.mostRecentBackup(Instant.now().epochSecond)

}

suspend fun generateBackupEmailData(
    userManagement: EmailCenter,
    flightsRepositoryExporter: FlightsRepositoryExporter = FlightsRepositoryExporter()
): BackupEmailData {
    val csv = flightsRepositoryExporter.buildCsvString()
    val username: String = userManagement.getUsername() ?: throw(CloudException(CloudFunctionResult.SERVER_REFUSED))
    val emailAddress: String = ServerPrefs.emailAddress()
    return BackupEmailData(username, emailAddress, csv)
}

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
    EmailCenter().requestEmailVerificationMail()
}

//TODO THIS NEEDS WORK
private fun storeLoginData(username: String, key: ByteArray, userManagement: EmailCenter = EmailCenter()) {
    userManagement.storeNewLoginData(username, key)
    Prefs.username = username
}

object ServerPrefs: JoozdLogPreferences() {
    override val preferencesFileKey = "nl.joozd.logbookapp.SERVER_PREFS_KEY"

    /*
    private const val XXXXXXXXXX = "XXXXXXXXXX"
     */

    private const val EMAIL_ADDRESS = "EMAIL_ADDRESS"
    private const val EMAIL_VERIFIED = "EMAIL_VERIFIED"
    private const val MOST_RECENT_FLIGHT_SYNC_EPOCH_SECOND = "MOST_RECENT_FLIGHT_SYNC_EPOCH_SECOND"

    val emailAddress by JoozdlogSharedPreferenceDelegate(EMAIL_ADDRESS,"")
    /**
     * [emailVerified] is true if email verification code was deemed correct by server
     * set this to false if server gives an INCORRECT_EMAIL_ADDRESS error
     */
    val emailVerified by JoozdlogSharedPreferenceDelegate(EMAIL_VERIFIED,false)
    val mostRecentFlightsSyncEpochSecond by JoozdlogSharedPreferenceDelegate(MOST_RECENT_FLIGHT_SYNC_EPOCH_SECOND, -1L)
}

