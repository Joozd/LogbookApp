package nl.joozd.logbookapp.utils

import android.content.Intent
import android.net.Uri
import nl.joozd.logbookapp.R
import nl.joozd.logbookapp.core.TaskFlags
import nl.joozd.logbookapp.core.messages.MessageCenter
import nl.joozd.logbookapp.core.EmailCenter
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

    private suspend fun handleEmailCodeConfirmation(){
        getEmailConfirmationCodeFromIntent()?.let{
            val usableString = it.replace('-', '/')
            if (EmailCenter().confirmEmail(usableString))
                showEmailConfirmationSheduledMessage()
            else
                MessagesWaiting.badVerificationCodeClicked(true)

        }
    }

    private fun getEmailConfirmationCodeFromIntent() =
        uri?.lastPathSegment

    private fun showEmailConfirmationSheduledMessage(){
        MessageCenter.commitMessage {
            titleResource = R.string.email_verification_scheduled_title
            descriptionResource = R.string.email_verification_scheduled_message
            setPositiveButton(android.R.string.ok){ }
        }
    }

    private fun showBadEmailConfirmationStringReceivedMessage(){
        MessageCenter.commitMessage {
            titleResource = R.string.error
            descriptionResource = R.string.email_verification_invalid_data
            setPositiveButton(android.R.string.ok){
                TaskFlags.updateEmailWithServer
            }
            setNegativeButton(android.R.string.cancel){
                // intentionally left blank
            }
        }
    }

    private enum class IntentType {
        EMAIL_VERIFICATION_LINK,
        INVALID;

        companion object {
            fun ofUri(uri: Uri?): IntentType = when (uri?.pathSegments?.firstOrNull()) {
                INTENT_VERIFY_EMAIL_PATH -> EMAIL_VERIFICATION_LINK
                else -> INVALID
            }
        }
    }

    companion object{
        private const val INTENT_VERIFY_EMAIL_PATH = "verify-email"
    }
}