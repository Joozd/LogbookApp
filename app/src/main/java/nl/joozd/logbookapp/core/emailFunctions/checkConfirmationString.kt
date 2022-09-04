package nl.joozd.logbookapp.core.emailFunctions

import nl.joozd.logbookapp.ui.utils.base64Decode

/**
 * Checks if confirmation string can be read by server.
 * Sending a bad confirmation string to server will result in BAD_DATA_RECEIVED which is handled the same as a connection error and will lead to an infinite loop.
 */
suspend fun checkConfirmationString(confirmationString: String, userManagement: EmailCenter = EmailCenter()) =
    userManagement.getUsernameWithKey()?.let{
        checkConfirmationString(confirmationString, it)
    } ?: false

private fun checkConfirmationString(confirmationString: String, loginData: UsernameWithKey): Boolean =
    ':' in confirmationString
            && confirmationString.split(':').let {
        it.first() == loginData.username
                && canBeBase64Decoded(it.last()) // true if can be base64 decoded else false
    }


private fun canBeBase64Decoded(it: String) = try {
    base64Decode(it)
    true
} catch (e: Exception) {
    false
}