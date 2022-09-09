package nl.joozd.logbookapp.core.background

import nl.joozd.logbookapp.R
import nl.joozd.logbookapp.core.messages.MessageCenter
import nl.joozd.logbookapp.core.messages.MessagesWaiting
import nl.joozd.logbookapp.core.emailFunctions.EmailCenter
import nl.joozd.logbookapp.ui.utils.JoozdlogActivity

/**
 * Send all persistant messages to [MessageCenter]
 * This does not change any flags other than the message flags.
 */
class PersistentMessagesDispatcher private constructor (private val messagesWaiting: MessagesWaiting = MessagesWaiting): BackgroundTasksDispatcher() {
    override fun startCollectors(activity: JoozdlogActivity) {
        handleNoEmailEntered(activity)
        handleNoVerificationCodeSavedBug(activity)
        handleBadVerificationCodeCLicked(activity)
        handleEmailConfirmed(activity)
    }


    private fun handleNoEmailEntered(scope: JoozdlogActivity) {
        messagesWaiting.noEmailEntered.flow.doIfTrueEmitted(scope) {
            postNoEmailEnteredMessage()
        }
    }

    private fun handleNoVerificationCodeSavedBug(scope: JoozdlogActivity) {
        messagesWaiting.noVerificationCodeSavedBug.flow.doIfTrueEmitted(scope) {
            postNoVerificationCodeSavedBugMessage()
        }
    }

    private fun handleBadVerificationCodeCLicked(scope: JoozdlogActivity) {
        messagesWaiting.badVerificationCodeClicked.flow.doIfTrueEmitted(scope) {
            postBadVerificationCodeClickedMessage()
        }
    }

    private fun handleEmailConfirmed(scope: JoozdlogActivity) {
        messagesWaiting.emailConfirmed.flow.doIfTrueEmitted(scope) {
            showEmailConfirmedMessage()
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
                EmailCenter().requestEmailVerificationMail()
                showEmailRequestedMessage()
            }
            setNegativeButton(android.R.string.cancel){
                messagesWaiting.badVerificationCodeClicked(false)
            }
        }
    }

    // Secondary message, does not need to reset a flag.
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
                messagesWaiting.emailConfirmed(false)
            }
        }
    }

    companion object{
        val instance by lazy { PersistentMessagesDispatcher() }
    }
}
