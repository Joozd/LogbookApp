package nl.joozd.logbookapp.utils

import android.content.Intent
import android.net.Uri
import nl.joozd.logbookapp.core.MessageCenter
import nl.joozd.logbookapp.core.EmailCenter
import nl.joozd.logbookapp.core.messages.MessageBarMessage
import nl.joozd.logbookapp.core.messages.MessagesWaiting

class IntentHandler(intent: Intent) {
    private val uri: Uri? = intent.data

    /**
     * Returns true if IntentHandler could handle this, of false if not
     */
    suspend fun handle(): Boolean = when(IntentType.ofUri(uri)){
        IntentType.INVALID -> false

        IntentType.EMAIL_VERIFICATION_LINK -> {
            handleEmailCodeConfirmation()
            true
        }
    }

    //Even though it looks like a Base64 string with '/' replaced by '-', it is never decoded so we leave it "as is"
    private suspend fun handleEmailCodeConfirmation(){
        getEmailConfirmationCodeFromIntent()?.let{
            if (EmailCenter().confirmEmail(it))
                showEmailConfirmationSheduledMessage()
            else
                MessagesWaiting.badVerificationCodeClicked(true)
        }
    }

    private fun getEmailConfirmationCodeFromIntent() =
        uri?.lastPathSegment

    private fun showEmailConfirmationSheduledMessage(){
        MessageCenter.scheduleMessage(MessageBarMessage.EMAIL_CONFIRMATION_SCHEDULED)
    }

    private enum class IntentType {
        EMAIL_VERIFICATION_LINK,
        INVALID;

        companion object {
            private const val INTENT_VERIFY_EMAIL_PATH = "verify-email"
            fun ofUri(uri: Uri?): IntentType = when (uri?.pathSegments?.firstOrNull()) {
                INTENT_VERIFY_EMAIL_PATH -> EMAIL_VERIFICATION_LINK
                else -> INVALID
            }
        }
    }
}