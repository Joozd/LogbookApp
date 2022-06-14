package nl.joozd.logbookapp.core.background


import kotlinx.coroutines.CoroutineScope
import nl.joozd.logbookapp.R
import nl.joozd.logbookapp.core.messages.MessageCenter
import nl.joozd.logbookapp.core.messages.MessagesWaiting
import nl.joozd.logbookapp.core.usermanagement.UserManagement

/**
 * Send all persistant messages to [MessageCenter]
 * This does not change any flags other than the message flags.
 */
class PersistentMessagesDispatcher: BackgroundTasksDispatcher() {
    override fun startCollectors(scope: CoroutineScope) {
        handleNoEmailEntered(scope)
        handleNoVerificationCodeSavedBug(scope)
        handleBadVerificationCodeCLicked(scope)
        handleEmailConfirmed(scope)
    }

    private fun handleNoEmailEntered(scope: CoroutineScope) {
        MessagesWaiting.noEmailEntered.flow.launchDoIfTrueCollected(scope) {
            postNoEmailEnteredMessage()
        }
    }

    private fun handleNoVerificationCodeSavedBug(scope: CoroutineScope) {
        MessagesWaiting.noVerificationCodeSavedBug.flow.launchDoIfTrueCollected(scope) {
            postNoVerificationCodeSavedBugMessage()
        }
    }

    private fun handleBadVerificationCodeCLicked(scope: CoroutineScope) {
        MessagesWaiting.badVerificationCodeClicked.flow.launchDoIfTrueCollected(scope) {
            postBadVerificationCodeClickedMessage()
        }
    }

    private fun handleEmailConfirmed(scope: CoroutineScope) {
        MessagesWaiting.emailConfirmed.flow.launchDoIfTrueCollected(scope) {
            showEmailConfirmedMessage()
        }
    }




    private fun postNoEmailEnteredMessage(){
        MessageCenter.commitMessage {
            titleResource = R.string.no_email
            descriptionResource = R.string.no_email_text
            setPositiveButton(android.R.string.ok){ MessagesWaiting.noEmailEntered(false) }
        }
    }

    private fun postNoVerificationCodeSavedBugMessage(){
        MessageCenter.commitMessage {
            titleResource = R.string.error
            descriptionResource = R.string.email_verification_code_not_saved_bug
            setPositiveButton(android.R.string.ok){ MessagesWaiting.noVerificationCodeSavedBug(false) }
        }
    }

    private fun postBadVerificationCodeClickedMessage(){
        MessageCenter.commitMessage {
            titleResource = R.string.verification_mail
            descriptionResource = R.string.email_verification_invalid_data
            setPositiveButton(android.R.string.ok){
                MessagesWaiting.badVerificationCodeClicked(false)
                UserManagement().requestEmailVerificationMail()
                showEmailRequestedMessage()
            }
            setNegativeButton(android.R.string.cancel){
                MessagesWaiting.badVerificationCodeClicked(false)
            }
        }
    }

    private fun showEmailRequestedMessage(){
        MessageCenter.commitMessage {
            titleResource = R.string.verification_mail
            descriptionResource = R.string.email_verification_scheduled_message
            setPositiveButton(android.R.string.ok) { }
        }
    }

    private fun showEmailConfirmedMessage(){
        MessageCenter.commitMessage {
            titleResource = R.string.verification_mail
            descriptionResource = R.string.email_verified
            setPositiveButton(android.R.string.ok){ }
        }
    }
}
