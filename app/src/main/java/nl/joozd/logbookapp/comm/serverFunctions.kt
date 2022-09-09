package nl.joozd.logbookapp.comm

//These functions know where Preferences such as login data or email addresses can be found.

import kotlinx.coroutines.withContext
import nl.joozd.joozdlogcommon.EmailData
import nl.joozd.joozdlogcommon.FeedbackData
import nl.joozd.logbookapp.core.TaskFlags
import nl.joozd.logbookapp.core.emailFunctions.checkConfirmationString
import nl.joozd.logbookapp.data.export.FlightsRepositoryExporter
import nl.joozd.logbookapp.data.repository.aircraftrepository.AircraftRepository
import nl.joozd.logbookapp.data.repository.airportrepository.AirportRepository
import nl.joozd.logbookapp.data.sharedPrefs.*
import nl.joozd.logbookapp.exceptions.CloudException
import nl.joozd.logbookapp.extensions.nullIfBlank
import nl.joozd.logbookapp.utils.DispatcherProvider
import java.time.Instant



/**
 * This will send a new email address to server
 * - Will attempt to get email address and emailID.
 * - Sends email to server with current email data
 * - On success, sets EmailVerified to false and saves emailID received from server.
 *      (if emailID was not set this will be a new one, if not it will probably stay the same)
 */
suspend fun updateEmailAddressOnServer(cloud: Cloud = Cloud(), serverPrefs: ServerPrefs = ServerPrefs) {
    val id = serverPrefs.emailID()
    val emailAddress = serverPrefs.emailAddress()
    cloud.sendNewEmailAddress(id, emailAddress)?.let{
        serverPrefs.emailID(it)
        serverPrefs.emailVerified(false)
    }
}


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

// This does NOT check to see if migration is possible or needed. Do that in calling function.
suspend fun migrateEmail(username: String, emailAddress: String, cloud: Cloud = Cloud()): Long? =
    cloud.migrateEmailData(username, emailAddress)

/*
 * Sending a mail is normally done from a worker, which is triggered by a TaskFlag.
 * In case there are no (or bad) email data set, server will reply with NOT_A_VALID_EMAIL_ADDRESS, for which Cloud will schedule a dialog through MessageCenter.
 * In case the email data is not yet confirmed or registered, server will reply EMAIL_NOT_KNOWN_OR_VERIFIED for which Cloud will schedule a dialog as well.
 * In both cases, the TaskFlag will be left alone and Cloud will schedule things so that when email is verified,
 *  so the worker will run again when the email address has been verified.
 *
 * migrateLoginDataIfNeeded() will make sure email data is migrated on the server to the emailID system, from the userName system.
 */
suspend fun sendBackupMailThroughServer(
    cloud: Cloud = Cloud()
){
    migrateLoginDataIfNeeded()
    val backupEmailData = generateBackupEmailData()
    try{
        cloud.sendBackupMailThroughServer(backupEmailData)
    } catch (e: CloudException){
        return
    }
    TaskFlags.sendBackupEmail(false)
    BackupPrefs.mostRecentBackup(Instant.now().epochSecond)
}

private suspend fun generateBackupEmailData(
    flightsRepositoryExporter: FlightsRepositoryExporter = FlightsRepositoryExporter()
): EmailData {
    val csv = flightsRepositoryExporter.buildCsvString()
    val id: Long = ServerPrefs.emailID()
    val emailAddress: String = ServerPrefs.emailAddress()
    return EmailData(id, emailAddress, csv.toByteArray(Charsets.UTF_8))
}

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

private suspend fun migrateLoginDataIfNeeded(){
    val userName = Prefs.username()
    val emailAddress = ServerPrefs.emailAddress()
    val migrationNeeded =
        userName != null
            && emailAddress.isNotBlank()
            && ServerPrefs.emailID() == EmailData.EMAIL_ID_NOT_SET
    if (migrationNeeded){
        migrateEmail(userName!!, emailAddress)?.let{
            Prefs.username(null)
            ServerPrefs.emailID(it)
        }
    }
    val creationNeeded = ServerPrefs.emailID() == EmailData.EMAIL_ID_NOT_SET
    if (creationNeeded){
        updateEmailAddressOnServer()
    }
}

private fun resetEmailCodeVerificationFlag() {
    TaskFlags.verifyEmailCode(false)
    TaskPayloads.emailConfirmationStringWaiting("")
}