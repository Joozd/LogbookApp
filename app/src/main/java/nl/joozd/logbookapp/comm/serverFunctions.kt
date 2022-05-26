package nl.joozd.logbookapp.comm

//These functions know where Preferences such as login data or email addresses can be found.

import android.util.Log
import androidx.work.ListenableWorker
import kotlinx.coroutines.withContext
import nl.joozd.logbookapp.core.TaskFlags
import nl.joozd.logbookapp.core.messages.MessagesWaiting
import nl.joozd.logbookapp.core.usermanagement.UsernameWithKey
import nl.joozd.logbookapp.core.usermanagement.UserManagement
import nl.joozd.logbookapp.core.usermanagement.ServerFunctionResult
import nl.joozd.logbookapp.core.usermanagement.checkConfirmationString
import nl.joozd.logbookapp.data.sharedPrefs.EmailPrefs
import nl.joozd.logbookapp.data.sharedPrefs.Prefs
import nl.joozd.logbookapp.utils.DispatcherProvider
import nl.joozd.logbookapp.utils.generateKey
import nl.joozd.logbookapp.workmanager.userManagementWorkers.ServerFunctionsWorkersHub

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
                    TaskFlags.postCreateNewUser(false) // blocking is OK in this context
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
suspend fun updateEmailAddressOnServer(cloud: Cloud = Cloud()): ServerFunctionResult =
    UsernameWithKey.fromPrefs()?.let { loginData ->
        getEmailAddressFromPrefs()?.let{ emailAddress ->
            sendEmailAddressToServer(loginData, emailAddress, cloud).also{
                if (it()) {
                    EmailPrefs.postEmailVerified(false)
                    TaskFlags.postUpdateEmailWithServer(false)
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
            EmailPrefs.emailVerified = true // blocking is OK in this context
            EmailPrefs.emailConfirmationStringWaiting = "" // blocking is OK in this context
            TaskFlags.verifyEmailCode = false // blocking is OK in this context
        }
    }

/**
 * This will request a backup email from the server.
 * - Ask Cloud to send a request for a backup email
 * - will send code to server, login data is needed to check confirmation code. If code check fails, will throw Exception.
 *  --- CHECK CONFIRMATION STRING IN CALLING FUNCTION (use [checkConfirmationString])
 * - On success, sets EmailVerified to true, removes stored confirmation string and resets TaskFlag.
 */
suspend fun requestBackupMail(cloud: Cloud = Cloud()): ServerFunctionResult =
    UsernameWithKey.fromPrefs()?.let { uk ->
        getEmailAddressFromPrefs()?.let { email ->
            cloud.requestBackupEmail(uk.username, uk.key, email).correspondingServerFunctionResult().also{
                if(it())
                    TaskFlags.postSendBackupEmail(false)
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
suspend fun requestLoginLinkEmail(cloud: Cloud = Cloud()): ServerFunctionResult =
    UsernameWithKey.fromPrefs()?.let { uk ->
        getEmailAddressFromPrefs()?.let { email ->
            cloud.requestLoginLinkMail(uk.username, uk.key, email).correspondingServerFunctionResult().also{
                if(it())
                    TaskFlags.postSendLoginLink(false)
            }
        }
    } ?: ServerFunctionResult.FAILURE






private suspend fun sendEmailConfirmationCode(confirmationString: String, cloud: Cloud): ServerFunctionResult {
    // If bad data is sent to server (server cannot discern between malformed data in transport or malformed data because bad data was sent)
    // we will get into an endless loop, se we check if data is parsable by server here.
    require(checkConfirmationString(confirmationString)) { "Bad confirmation string $confirmationString received, this should have been checked by calling function" }
    return cloud.confirmEmail(confirmationString).correspondingServerFunctionResult().also {
        if (it())
            resetEmailCodeVerificationFlag()
    }
}

private suspend fun resetEmailCodeVerificationFlag() = withContext(DispatcherProvider.io()){
    TaskFlags.verifyEmailCode = false // blocking is OK in this context
    EmailPrefs.emailConfirmationStringWaiting = "" // // blocking is OK in this context
}

private suspend fun sendEmailAddressToServer(loginData: UsernameWithKey, emailAddress: String, cloud: Cloud): ServerFunctionResult =
    //NOTE this does NOT save the email address nor does any other marking, flagging or anything.
    cloud.sendNewEmailAddress(loginData.username, loginData.key, emailAddress).correspondingServerFunctionResult()

private suspend fun createNewUserOnServer(loginData: UsernameWithKey, cloud: Cloud): ServerFunctionResult =
    when(cloud.createNewUser(loginData.username, loginData.key)){
        CloudFunctionResult.OK -> ServerFunctionResult.SUCCESS
        CloudFunctionResult.CONNECTION_ERROR -> ServerFunctionResult.RETRY
        CloudFunctionResult.SERVER_REFUSED -> ServerFunctionResult.FAILURE // this only happens when server returns USER_ALREADY_EXISTS; in this case just rerun function with new username/key
    }

// If there is no email address stored, this will handle it (eg. prompt user to enter email address so requested calling function can be performed)
private suspend fun getEmailAddressFromPrefs(): String? =
    EmailPrefs.emailAddress().ifBlank {
        // Fallback handling of no email address entered when one is needed.
        // Any functions being called should only be triggered when an emal address is entered.
        // Therefore, this should only happen after user triggered an action in a way unforeseen at the time of this writing
        Log.e("getEmailAddressFromPrefs", "getEmailAddressFromPrefs() was called but an EmailPrefs.emailAddress is blank. User is notified to fix this.")
        MessagesWaiting.postNoEmailEntered(true) // this will trigger display of "no email entered" message to user.
        EmailPrefs.emailVerified = false

        null
    }

private fun resetEmailData() {
    EmailPrefs.postEmailVerified(false)
    UserManagement().requestEmailVerificationMail()
}

// has blocking IO ops
private fun storeLoginData(username: String, key: ByteArray) {
    Prefs.username = username
    Prefs.key = key
    Prefs.postLastUpdateTime(-1)
}

private fun CloudFunctionResult.correspondingServerFunctionResult(): ServerFunctionResult = when(this){
    CloudFunctionResult.OK -> ServerFunctionResult.SUCCESS
    CloudFunctionResult.CONNECTION_ERROR -> ServerFunctionResult.RETRY
    CloudFunctionResult.SERVER_REFUSED -> ServerFunctionResult.FAILURE
}

