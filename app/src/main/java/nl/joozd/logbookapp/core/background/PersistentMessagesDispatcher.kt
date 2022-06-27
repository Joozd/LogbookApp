package nl.joozd.logbookapp.core.background


import android.content.Intent
import androidx.activity.ComponentActivity
import androidx.lifecycle.lifecycleScope
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.launch
import nl.joozd.logbookapp.R
import nl.joozd.logbookapp.core.messages.MessageCenter
import nl.joozd.logbookapp.core.messages.MessagesWaiting
import nl.joozd.logbookapp.core.usermanagement.UserManagement
import nl.joozd.logbookapp.ui.utils.JoozdlogActivity

/**
 * Send all persistant messages to [MessageCenter]
 * This does not change any flags other than the message flags.
 */
class PersistentMessagesDispatcher private constructor (private val messagesWaiting: MessagesWaiting = MessagesWaiting): BackgroundTasksDispatcher() {
    override fun startCollectors(activity: JoozdlogActivity) {
        handleNewCloudAccountCreated(activity)
        handleNoEmailEntered(activity)
        handleNoVerificationCodeSavedBug(activity)
        handleBadVerificationCodeCLicked(activity)
        handleEmailConfirmed(activity)
        handleMergeWithServerPerformed(activity)
        handleNoLoginDataSaved(activity)
    }

    private fun handleNewCloudAccountCreated(scope: JoozdlogActivity) {
        messagesWaiting.newCloudAccountCreated.flow.doIfTrueEmitted(scope) {
            postNewCloudAccountCreatedMessage()
        }
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

    private fun handleMergeWithServerPerformed(scope: JoozdlogActivity) {
        messagesWaiting.mergeWithServerPerformed.flow.doIfTrueEmitted(scope) {
            showMergeWithServerPerformedMessage()
        }
    }

    private fun handleNoLoginDataSaved(scope: JoozdlogActivity) {
        messagesWaiting.noLoginDataSaved.flow.doIfTrueEmitted(scope) {
            showNoLoginDataSavedMessage()
        }
    }


    private fun postNewCloudAccountCreatedMessage(){
        MessageCenter.commitMessage {
            titleResource = R.string.new_cloud_account_made
            descriptionResource = R.string.new_cloud_account_made_message_please_send_link_to_yourself
            setPositiveButton(R.string.share_link){
                lifecycleScope.launch {
                    println("lalala yes clicked")
                    messagesWaiting.newCloudAccountCreated(false)
                    sendLoginLinkIntent()
                }
            }
            setNegativeButton(android.R.string.cancel){
                println("lalala no clicked")
                messagesWaiting.newCloudAccountCreated(false)
            }
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
            titleResource = R.string.data_has_been_merged
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

    private suspend fun ComponentActivity.sendLoginLinkIntent() {
        UserManagement().generateLoginLink()?.let { loginLink ->
            val sendIntent: Intent = Intent().apply {
                action = Intent.ACTION_SEND
                putExtra(Intent.EXTRA_TEXT, loginLink)
                type = "text/plain"
            }

            val shareIntent = Intent.createChooser(sendIntent, null)
            startActivity(shareIntent)
        }
    }

    companion object{
        val instance by lazy { PersistentMessagesDispatcher() }
    }


}
