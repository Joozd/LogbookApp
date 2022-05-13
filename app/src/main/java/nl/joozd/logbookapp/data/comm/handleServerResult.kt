package nl.joozd.logbookapp.data.comm

import android.util.Log
import nl.joozd.joozdlogcommon.comms.JoozdlogCommsKeywords
import nl.joozd.logbookapp.R
import nl.joozd.logbookapp.core.MessageCenter
import nl.joozd.logbookapp.core.UserManagement
import nl.joozd.logbookapp.utils.UserMessage

suspend fun handleServerResult(serverResult: String?): CloudFunctionResult =
    when(serverResult){
        JoozdlogCommsKeywords.OK  -> CloudFunctionResult.OK

        null,
        JoozdlogCommsKeywords.SERVER_ERROR,
        JoozdlogCommsKeywords.BAD_DATA_RECEIVED -> CloudFunctionResult.CONNECTION_ERROR

        JoozdlogCommsKeywords.EMAIL_NOT_KNOWN_OR_VERIFIED -> handleUnknownOrUnverifiedEmail()
        JoozdlogCommsKeywords.NOT_A_VALID_EMAIL_ADDRESS -> handleBadEmailAddress()
        JoozdlogCommsKeywords.UNKNOWN_USER_OR_PASS -> handleBadLoginData()
        JoozdlogCommsKeywords.NOT_LOGGED_IN -> handleNotLoggedInSituation()
        JoozdlogCommsKeywords.USER_ALREADY_EXISTS -> handleUserAlreadyExists()

        else -> {
            Log.e("HandleServerResult", "cannot handle response from server: $serverResult")
            CloudFunctionResult.CONNECTION_ERROR
        }
    }

private suspend fun handleUnknownOrUnverifiedEmail(): CloudFunctionResult {
    TODO("Set email to unverified, let MessageCenter display message about this with possible user action")
    return CloudFunctionResult.SERVER_REFUSED
}

/*
 * This one should have been caught by checking for valid email when user enters email address.
 */
private fun handleBadEmailAddress(): CloudFunctionResult {
    UserManagement.invalidateEmail()
    val message = UserMessage.Builder().apply{
        titleResource = R.string.not_an_email_address
        descriptionResource = R.string.server_not_an_email_address_please_enter_again
        setPositiveButton(R.string.enter_email){

        }
    }.build()
    MessageCenter.pushMessage(message)
    return CloudFunctionResult.SERVER_REFUSED
}

private suspend fun handleBadLoginData(): CloudFunctionResult {
    TODO("Log out, ask user to generate new login data or click login link")
    return CloudFunctionResult.SERVER_REFUSED
}

private suspend fun handleNotLoggedInSituation(): CloudFunctionResult {
    TODO("Show message saying not logged in situation, this is an error message, should not happen.")
    return CloudFunctionResult.SERVER_REFUSED
}

private suspend fun handleUserAlreadyExists(): CloudFunctionResult {
    UserManagement.createNewUser()
    return CloudFunctionResult.SERVER_REFUSED
}