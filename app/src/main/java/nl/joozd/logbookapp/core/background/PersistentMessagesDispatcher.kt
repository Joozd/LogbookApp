package nl.joozd.logbookapp.core.background


import nl.joozd.logbookapp.R
import nl.joozd.logbookapp.core.messages.MessageCenter
import nl.joozd.logbookapp.core.messages.MessagesWaiting
import nl.joozd.logbookapp.core.usermanagement.UserManagement

/**
 * Send all persistant messages to [MessageCenter]
 * This does not change any flags other than the message flags.
 */
class PersistentMessagesDispatcher: BackgroundTasksDispatcher() {
    override suspend fun startCollectors() {
        handleNoEmailEntered()
        handleNoVerificationCodeSavedBug()
        handleBadVerificationCodeCLicked()
        handleEmailConfirmed()
    }

    private suspend fun handleNoEmailEntered() {
        MessagesWaiting.noEmailEnteredFlow.doIfTrueCollected {
            postNoEmailEnteredMessage()
        }
    }

    private suspend fun handleNoVerificationCodeSavedBug() {
        MessagesWaiting.noVerificationCodeSavedBugFlow.doIfTrueCollected {
            postNoVerificationCodeSavedBugMessage()
        }
    }

    private suspend fun handleBadVerificationCodeCLicked() {
        MessagesWaiting.badVerificationCodeClickedFlow.doIfTrueCollected {
            postBadVerificationCodeClickedMessage()
        }
    }

    private suspend fun handleEmailConfirmed() {
        MessagesWaiting.emailConfirmedFlow.doIfTrueCollected {
            showEmailConfirmedMessage()
        }
    }




    private fun postNoEmailEnteredMessage(){
        MessageCenter.commitMessage {
            titleResource = R.string.no_email
            descriptionResource = R.string.no_email_text
            setPositiveButton(android.R.string.ok){ MessagesWaiting.postNoEmailEntered(false) }
        }
    }

    private fun postNoVerificationCodeSavedBugMessage(){
        MessageCenter.commitMessage {
            titleResource = R.string.error
            descriptionResource = R.string.email_verification_code_not_saved_bug
            setPositiveButton(android.R.string.ok){ MessagesWaiting.postBadVerificationCodeSavedBug(false) }
        }
    }

    private fun postBadVerificationCodeClickedMessage(){
        MessageCenter.commitMessage {
            titleResource = R.string.verification_mail
            descriptionResource = R.string.email_verification_invalid_data
            setPositiveButton(android.R.string.ok){
                MessagesWaiting.postBadVerificationCodeClicked(false)
                UserManagement().requestEmailVerificationMail()
                showEmailRequestedMessage()
            }
            setNegativeButton(android.R.string.cancel){
                MessagesWaiting.postBadVerificationCodeClicked(false)
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