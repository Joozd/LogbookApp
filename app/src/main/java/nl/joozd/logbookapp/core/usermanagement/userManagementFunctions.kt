package nl.joozd.logbookapp.core.usermanagement

import android.util.Log
import nl.joozd.logbookapp.core.TaskFlags
import nl.joozd.logbookapp.core.messages.MessagesWaiting
import nl.joozd.logbookapp.data.comm.Cloud
import nl.joozd.logbookapp.data.comm.CloudFunctionResult
import nl.joozd.logbookapp.data.sharedPrefs.EmailPrefs
import nl.joozd.logbookapp.data.sharedPrefs.Prefs
import nl.joozd.logbookapp.ui.utils.base64Decode
import nl.joozd.logbookapp.utils.generateKey

/**
 * This will invalidate current login data.
 * - Asks a username from server
 * - generates a password
 * - Creates a new user on server with those data.
 * - On success, saves the data, requests a confirmation email and resets TaskFlag.
 * @return success() if new data received and saved, retry() if connection error, failure() if server refused.
 */
suspend fun generateNewUserAndCreateOnServer(cloud: Cloud = Cloud()): UserManagementFunctionResult =
    cloud.requestUsername()?.let { n ->
        val loginData = LoginData(n, generateKey())
        createNewUserOnServer(loginData, cloud).also{
            if(it == UserManagementFunctionResult.SUCCESS) {
                storeLoginData(loginData.username, loginData.key) // blocking is OK in this context
                resetEmailData()
                TaskFlags.createNewUser = false // blocking is OK in this context
            }
        }
    } ?: UserManagementFunctionResult.RETRY


/**
 * This will send a new email address to server
 * - Will attempt to get login data
 * - Will attempt to get email address
 * - Sends email to server with current login data
 * - On success, sets EmailVerified to false and resets TaskFlag.
 */
suspend fun updateEmailAddressOnServer(cloud: Cloud = Cloud()): UserManagementFunctionResult =
    LoginData.fromPrefs()?.let { loginData ->
        getEmailAddressFromPrefs()?.let{ emailAddress ->
            sendEmailAddressToServer(loginData, emailAddress, cloud).also{
                if (it == UserManagementFunctionResult.SUCCESS) {
                    EmailPrefs.emailVerified = false // blocking is OK in this context
                    TaskFlags.updateEmailWithServer = false // blocking is OK in this context
                }
            }
        }
    } ?: UserManagementFunctionResult.FAILURE

/**
 * This will confirm email confirmation code with server
 * - Will attempt to get login data
 * - will send code to server, login data is needed to check confirmation code. If code check fails, will throw Exception.
 *  --- CHECK CODE IN CALLING FUNCTION (use [checkConfirmationString])
 * - On success, sets EmailVerified to true, removes stored confirmation string and resets TaskFlag.
 */
suspend fun confirmEmail(confirmationString: String, cloud: Cloud = Cloud()): UserManagementFunctionResult =
    LoginData.fromPrefs()?.let{ loginData ->
        sendEmailConfirmationCode(loginData, confirmationString, cloud).also{
            if (it == UserManagementFunctionResult.SUCCESS){
                EmailPrefs.emailVerified = true // blocking is OK in this context
                EmailPrefs.emailConfirmationStringWaiting = "" // blocking is OK in this context
                TaskFlags.verifyEmailCode = false // blocking is OK in this context
            }
        }
    } ?: UserManagementFunctionResult.FAILURE

private suspend fun sendEmailConfirmationCode(loginData: LoginData, confirmationString: String, cloud: Cloud): UserManagementFunctionResult {
    // If bad data is sent to server (server cannot discern between malformed data in transport or malformed data because bad data was sent)
    // we will get into an endless loop, se we check if data is parsable by server here.
    require(checkConfirmationString(confirmationString, loginData)) { "Bad confirmation string $confirmationString received, this should have been checked by calling function" }
    return when (cloud.confirmEmail(confirmationString)) {
        CloudFunctionResult.OK -> {
            TaskFlags.verifyEmailCode = false // blocking is OK in this context
            EmailPrefs.emailConfirmationStringWaiting = "" // // blocking is OK in this context
            UserManagementFunctionResult.SUCCESS
        }
        CloudFunctionResult.SERVER_REFUSED -> {
            // This means login data is bad, or code is incorrect. Cloud will take care of remedy.
            UserManagementFunctionResult.FAILURE
        }
        CloudFunctionResult.CONNECTION_ERROR ->
            UserManagementFunctionResult.RETRY
    }
}

private fun checkConfirmationString(confirmationString: String, loginData: LoginData): Boolean =
    ':' in confirmationString
    && confirmationString.split(':').let {
        it.first() == loginData.username
        && canBeBase64Decoded(it.last()) // true if can be base64 decoded else false
    }

/**
 * Checks if confirmation string can be read by server.
 * Sending a bad confirmation string to server will result in BAD_DATA_RECEIVED which is handled the same as a connection error and will lead to an infinite loop.
 */
fun checkConfirmationString(confirmationString: String) = LoginData.fromPrefs()?.let{
    checkConfirmationString(confirmationString, it)
} ?: false


private fun canBeBase64Decoded(it: String) = try {
    base64Decode(it)
    true
} catch (e: Exception) {
    false
}


/*
 * Send email address to server. Returns a ListenableWorker.Result on completion.
 * NOTE this does NOT save the email address nor does any other marking, flagging or anything.
 */
private suspend fun sendEmailAddressToServer(loginData: LoginData, emailAddress: String, cloud: Cloud): UserManagementFunctionResult =

        when (cloud.sendNewEmailAddress(loginData.username, loginData.key, emailAddress)) {
            CloudFunctionResult.OK -> UserManagementFunctionResult.SUCCESS
            CloudFunctionResult.CONNECTION_ERROR -> UserManagementFunctionResult.RETRY
            CloudFunctionResult.SERVER_REFUSED -> UserManagementFunctionResult.FAILURE
        }


/*
 * Create a new user on server.
 * Returns whether this can eb considered a success, failure or should be retried.
 */
private suspend fun createNewUserOnServer(loginData: LoginData, cloud: Cloud): UserManagementFunctionResult =
    when(cloud.createNewUser(loginData.username, loginData.key)){
        CloudFunctionResult.OK ->UserManagementFunctionResult.SUCCESS
        CloudFunctionResult.CONNECTION_ERROR -> UserManagementFunctionResult.RETRY
        CloudFunctionResult.SERVER_REFUSED -> UserManagementFunctionResult.FAILURE
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


// mark email as unverified, request verification mail if able.
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

