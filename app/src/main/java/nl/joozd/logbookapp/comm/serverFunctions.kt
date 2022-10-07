package nl.joozd.logbookapp.comm

//These functions know where Preferences such as login data or email addresses can be found.

import nl.joozd.joozdlogcommon.EmailData
import nl.joozd.joozdlogcommon.FeedbackData
import nl.joozd.logbookapp.core.TaskFlags
import nl.joozd.logbookapp.data.export.FlightsRepositoryExporter
import nl.joozd.logbookapp.data.repository.aircraftrepository.AircraftRepository
import nl.joozd.logbookapp.data.repository.airportrepository.AirportRepository
import nl.joozd.logbookapp.data.sharedPrefs.*
import nl.joozd.logbookapp.exceptions.CloudException
import nl.joozd.logbookapp.extensions.nullIfBlank
import java.time.Instant



/**
 * This will send a new email address to server
 * - Will attempt to get email address and emailID.
 * - Sends email to server with current email data
 * - On success, sets EmailVerified to false and saves emailID received from server.
 *      (if emailID was not set this will be a new one, if not it will probably stay the same)
 * - on failure, will schedule an update and throw a [CloudException]
 */
suspend fun updateEmailAddressOnServer(cloud: Cloud = Cloud(), emailPrefs: EmailPrefs = EmailPrefs) {
    val emailAddress = emailPrefs.emailAddress()
    try {
        cloud.sendNewEmailAddress(emailAddress)?.let {
            emailPrefs.emailID(it)
            emailPrefs.emailVerified(false)
        }
    } catch( e: CloudException){
        TaskFlags.updateEmailWithServer(true)
        return
    }
    TaskFlags.updateEmailWithServer(false)
}


/**
 * This will confirm email confirmation code with server
 * - Will attempt to get login data
 * - will send code to server, login data is needed to check confirmation code. If code check fails, will throw Exception.
 *  --- CHECK CONFIRMATION STRING IN CALLING FUNCTION
 * - On success, sets EmailVerified to true, removes stored confirmation string and resets TaskFlag.
 */
suspend fun confirmEmail(confirmationString: String, cloud: Cloud = Cloud()): ServerFunctionResult =
    sendEmailConfirmationCode(confirmationString, cloud).also {
        if (it == ServerFunctionResult.SUCCESS)
            EmailPrefs.emailVerified(true)
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
    val id: Long = EmailPrefs.emailID()
    val emailAddress: String = EmailPrefs.emailAddress()
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
        cloud.sendFeedback(FeedbackData(feedback, TaskPayloads.feedbackContactInfo()))
            .correspondingServerFunctionResult()
    }  ?: ServerFunctionResult.SUCCESS)


// NOTE If a bad confirmationString is sent to server it will result in an endless loop, so check data before calling this function
private suspend fun sendEmailConfirmationCode(confirmationString: String, cloud: Cloud): ServerFunctionResult {
    return cloud.confirmEmail(confirmationString).correspondingServerFunctionResult()
}

suspend fun migrateLoginDataIfNeeded(emailPrefs: EmailPrefs){
    val userName = Prefs.username()
    val emailAddress = emailPrefs.emailAddress()
    val migrationNeeded =
        userName != null
            && emailAddress.isNotBlank()
            && emailPrefs.emailID() == EmailData.EMAIL_ID_NOT_SET
    if (migrationNeeded){
        migrateEmail(userName!!, emailAddress)?.let{
            Prefs.username(null)
            emailPrefs.emailID(it)
        }
    }
}

