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
class PersistentMessagesDispatcher(private val messagesWaiting: MessagesWaiting = MessagesWaiting): BackgroundTasksDispatcher() {
    override fun startCollectors(scope: CoroutineScope) {
        handleNoEmailEntered(scope)
        handleNoVerificationCodeSavedBug(scope)
        handleBadVerificationCodeCLicked(scope)
        handleEmailConfirmed(scope)
        handleNoLoginDataSaved(scope)
    }

    private fun handleNoEmailEntered(scope: CoroutineScope) {
        messagesWaiting.noEmailEntered.flow.doIfTrueEmitted(scope) {
            postNoEmailEnteredMessage()
        }
    }

    private fun handleNoVerificationCodeSavedBug(scope: CoroutineScope) {
        messagesWaiting.noVerificationCodeSavedBug.flow.doIfTrueEmitted(scope) {
            postNoVerificationCodeSavedBugMessage()
        }
    }

    private fun handleBadVerificationCodeCLicked(scope: CoroutineScope) {
        messagesWaiting.badVerificationCodeClicked.flow.doIfTrueEmitted(scope) {
            postBadVerificationCodeClickedMessage()
        }
    }

    private fun handleEmailConfirmed(scope: CoroutineScope) {
        messagesWaiting.emailConfirmed.flow.doIfTrueEmitted(scope) {
            showEmailConfirmedMessage()
        }
    }

    private fun handleNoLoginDataSaved(scope: CoroutineScope) {
        messagesWaiting.noLoginDataSaved.flow.doIfTrueEmitted(scope) {
            showNoLoginDataSavedMessage()
        }
    }




    private fun postNoEmailEnteredMessage(){
        MessageCenter.commitMessage {
            titleResource = R.string.no_email
            descriptionResource = R.string.no_email_text
            setPositiveButton(android.R.string.ok){ messagesWaiting.noEmailEntered(false) }
        }
    }

    private fun postNoVerificationCodeSavedBugMessage(){
        MessageCenter.commitMessage {
            titleResource = R.string.error
            descriptionResource = R.string.email_verification_code_not_saved_bug
            setPositiveButton(android.R.string.ok){ messagesWaiting.noVerificationCodeSavedBug(false) }
        }
    }

    private fun postBadVerificationCodeClickedMessage(){
        MessageCenter.commitMessage {
            titleResource = R.string.verification_mail
            descriptionResource = R.string.email_verification_invalid_data
            setPositiveButton(android.R.string.ok){
                messagesWaiting.badVerificationCodeClicked(false)
                UserManagement().requestEmailVerificationMail()
                showEmailRequestedMessage()
            }
            setNegativeButton(android.R.string.cancel){
                messagesWaiting.badVerificationCodeClicked(false)
            }
        }
    }

    private fun showMergeWithServerPerformedMessage() {
        MessageCenter.commitMessage {
            titleResource = R.string.login_error
            descriptionResource = R.string.server_merge_performed
            setPositiveButton(android.R.string.ok){
                messagesWaiting.mergeWithServerPerformed(false)
            }
        }
    }

    private fun showNoLoginDataSavedMessage() {
        MessageCenter.commitMessage {
            titleResource = R.string.login_error
            descriptionResource = R.string.no_login_data_cloud_disabled
            setPositiveButton(android.R.string.ok){
                messagesWaiting.noLoginDataSaved(false)
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
            setPositiveButton(android.R.string.ok){

            }
        }
    }


}
