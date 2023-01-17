package nl.joozd.logbookapp.comm

import nl.joozd.joozdlogcommon.comms.JoozdlogCommsResponses
import nl.joozd.logbookapp.core.MessageCenter
import nl.joozd.logbookapp.core.EmailCenter
import nl.joozd.logbookapp.core.messages.MessageBarMessage
import nl.joozd.logbookapp.exceptions.CloudException

fun handleServerResult(serverResult: String?): CloudFunctionResult? =
    when(JoozdlogCommsResponses.from(serverResult ?: JoozdlogCommsResponses.CONNECTION_ERROR.keyword)){
        JoozdlogCommsResponses.OK -> CloudFunctionResult.OK

        JoozdlogCommsResponses.CONNECTION_ERROR, // this happens when serverResult == null
        JoozdlogCommsResponses.SERVER_ERROR -> CloudFunctionResult.SERVER_REFUSED
        JoozdlogCommsResponses.BAD_DATA_RECEIVED -> CloudFunctionResult.SERVER_REFUSED

        JoozdlogCommsResponses.EMAIL_NOT_KNOWN_OR_VERIFIED -> handleUnknownOrUnverifiedEmail()
        JoozdlogCommsResponses.NOT_A_VALID_EMAIL_ADDRESS -> handleBadEmailAddress()

        JoozdlogCommsResponses.ID_NOT_FOUND, // this will get passed on to receiving function; it must always be handled during P2P session as those happen in foreground.
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
    handleServerResultAndThrowExceptionOnError(serverResult?.toString(Charsets.UTF_8))

private fun handleUnknownOrUnverifiedEmail(): CloudFunctionResult {
    println("BOTERHAMZAKJE")
    EmailCenter().invalidateEmail()
    MessageCenter.scheduleMessage(MessageBarMessage.UNKNOWN_OR_UNVERIFIED_EMAIL)
    //pushMessage(MessagesOld.unknownOrUnverifiedEmailMessage)
    return CloudFunctionResult.SERVER_REFUSED
}

/*
 * This one should have been caught by checking for valid email when user enters email address.
 */
private fun handleBadEmailAddress(): CloudFunctionResult {
    EmailCenter().invalidateEmail()
    MessageCenter.scheduleMessage(MessageBarMessage.INVALID_EMAIL_ADDRESS)
    return CloudFunctionResult.SERVER_REFUSED
}
