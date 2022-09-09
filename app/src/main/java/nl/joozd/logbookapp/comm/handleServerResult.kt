package nl.joozd.logbookapp.comm

import nl.joozd.joozdlogcommon.comms.JoozdlogCommsResponses
import nl.joozd.logbookapp.core.messages.MessageCenter
import nl.joozd.logbookapp.core.emailFunctions.EmailCenter
import nl.joozd.logbookapp.core.messages.Messages
import nl.joozd.logbookapp.exceptions.CloudException

fun handleServerResult(serverResult: ByteArray?) = handleServerResult(serverResult?.toString())

fun handleServerResult(serverResult: String?): CloudFunctionResult? =
    when(JoozdlogCommsResponses.toKeyword(serverResult?: JoozdlogCommsResponses.CONNECTION_ERROR.keyword)){
        JoozdlogCommsResponses.OK -> CloudFunctionResult.OK

        JoozdlogCommsResponses.CONNECTION_ERROR,
        JoozdlogCommsResponses.SERVER_ERROR,
        JoozdlogCommsResponses.BAD_DATA_RECEIVED -> CloudFunctionResult.CONNECTION_ERROR

        JoozdlogCommsResponses.EMAIL_NOT_KNOWN_OR_VERIFIED -> handleUnknownOrUnverifiedEmail()
        JoozdlogCommsResponses.NOT_A_VALID_EMAIL_ADDRESS -> handleBadEmailAddress()

        JoozdlogCommsResponses.P2P_SESSION_NOT_FOUND, // this will get passed on to receiving function; it must always be handled during P2P session as those happen in foreground.
        JoozdlogCommsResponses.UNKNOWN_KEYWORD -> null
        // null means something else was sent by server; probably data that we want to receive.
        // This way, we can check a serverResult for one of the above responses.
        // example: val x = readFromServer(); handleServerResult(x)?.let { throw(CloudException(it)) }; doSomethingWith(x)
    }

fun handleServerResultAndThrowExceptionOnError(serverResult: String?){
    val result = handleServerResult(serverResult)
    if (result == null || result.isOK()) return // result is OK or data
    else throw CloudException(result)
}

fun handleServerResultAndThrowExceptionOnError(serverResult: ByteArray?) =
    handleServerResultAndThrowExceptionOnError(serverResult.toString())

private fun handleUnknownOrUnverifiedEmail(): CloudFunctionResult {
    EmailCenter().setEmailUnverified()
    MessageCenter.pushMessage(Messages.unknownOrUnverifiedEmailMessage)
    return CloudFunctionResult.SERVER_REFUSED
}

/*
 * This one should have been caught by checking for valid email when user enters email address.
 */
private fun handleBadEmailAddress(): CloudFunctionResult {
    EmailCenter().invalidateEmail()
    MessageCenter.pushMessage(Messages.invalidEmailAddressSentToServer)
    return CloudFunctionResult.SERVER_REFUSED
}
