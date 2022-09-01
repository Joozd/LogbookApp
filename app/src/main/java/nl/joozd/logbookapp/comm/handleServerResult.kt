package nl.joozd.logbookapp.comm


import nl.joozd.joozdlogcommon.comms.JoozdlogCommsKeywords
import nl.joozd.logbookapp.R
import nl.joozd.logbookapp.core.messages.MessageCenter
import nl.joozd.logbookapp.core.messages.MessagesWaiting
import nl.joozd.logbookapp.core.usermanagement.UserManagement
import nl.joozd.logbookapp.exceptions.CloudException

fun handleServerResult(serverResult: ByteArray?) = handleServerResult(serverResult?.toString())

fun handleServerResult(serverResult: String?): CloudFunctionResult? =
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

        // null means something else was sent by server; probably data that we want to receive.
        // This way, we can check a serverResult for one of the above responses.
        // example: val x = readFromServer(); handleServerResult(x)?.let { return it }; doSomethingWith(x)
        else -> null
    }

fun handleServerResultAndThrowExceptionOnError(serverResult: String?){
    val result = handleServerResult(serverResult)
    if (result == null || result.isOK()) return // result is OK or data
    else throw CloudException(result)
}

fun handleServerResultAndThrowExceptionOnError(serverResult: ByteArray?) =
    handleServerResultAndThrowExceptionOnError(serverResult.toString())

private fun handleUnknownOrUnverifiedEmail(): CloudFunctionResult {
    UserManagement().invalidateEmail()
    MessageCenter.commitMessage {
        titleResource = R.string.email
        descriptionResource = R.string.server_reported_email_not_verified_new_mail_will_be_sent
        setPositiveButton(android.R.string.ok){
            UserManagement().requestEmailVerificationMail()
        }
    }
    return CloudFunctionResult.SERVER_REFUSED
}

/*
 * This one should have been caught by checking for valid email when user enters email address.
 */
private fun handleBadEmailAddress(): CloudFunctionResult {
    UserManagement().invalidateEmail()
    MessageCenter.commitMessage {
        titleResource = R.string.email
        descriptionResource = R.string.server_not_an_email_address_please_enter_again
        setPositiveButton(android.R.string.ok){ }
    }
    return CloudFunctionResult.SERVER_REFUSED
}

private fun handleBadLoginData(): CloudFunctionResult {
    UserManagement().logOut()
    MessagesWaiting.noLoginDataSaved(true)
    return CloudFunctionResult.SERVER_REFUSED
}

private fun handleNotLoggedInSituation(): CloudFunctionResult {
    MessageCenter.commitMessage {
        titleResource = R.string.login_error
        descriptionResource = R.string.not_signed_in_bug_please_tell_joozd
        setPositiveButton(android.R.string.ok){ }
    }
    return CloudFunctionResult.SERVER_REFUSED
}
// this only happens when creating a new user, in extremely rare cases. The calling function will try again on a SERVER_REFUSED.
private fun handleUserAlreadyExists(): CloudFunctionResult {
    return CloudFunctionResult.SERVER_REFUSED
}