package nl.joozd.logbookapp.core.background

import kotlinx.coroutines.flow.combine
import nl.joozd.logbookapp.data.sharedPrefs.EmailPrefs
import nl.joozd.logbookapp.core.TaskFlags
import nl.joozd.logbookapp.data.sharedPrefs.TaskPayloads
import nl.joozd.logbookapp.ui.utils.JoozdlogActivity
import nl.joozd.logbookapp.workmanager.ServerFunctionsWorkersHub

/**
 * Run all tasks set in [taskFlags].
 * All tasks will go straight to Worker as none is very time-sensitive.
 */
class TaskDispatcher private constructor(private val taskFlags: TaskFlags = TaskFlags): BackgroundTasksDispatcher() {
    override fun startCollectors(activity: JoozdlogActivity) {
        //these functions collect their respective Flow and handle that flow's output.
        handleEmailUpdateWanted(activity)
        handleEmailConfirmationWanted(activity)
        handleBackupEmailWanted(activity)
        handleFeedbackWaiting(activity)
        handleSyncDataFiles(activity)
    }

    private fun handleEmailUpdateWanted(activity: JoozdlogActivity) {
        emailUpdateWantedFlow.doIfTrueEmitted(activity) {
            println("VLIEGTUIG handleEmailUpdateWanted emitted true")
            ServerFunctionsWorkersHub().scheduleUpdateEmail()
        }
    }

    private fun handleEmailConfirmationWanted(activity: JoozdlogActivity) {
        emailConfirmationWantedFlow().doIfTrueEmitted(activity) {
            ServerFunctionsWorkersHub().scheduleConfirmEmail() // Worker takes care of checking for bad email confirmation string to prevent infinite loop.
        }
    }

    private fun handleBackupEmailWanted(activity: JoozdlogActivity) {
        backupEmailWantedFlow().doIfTrueEmitted(activity) {
            ServerFunctionsWorkersHub().scheduleBackupEmail()
        }
    }

    private fun handleFeedbackWaiting(activity: JoozdlogActivity) {
        feedbackReadyFlow().doIfTrueEmitted(activity) {
            ServerFunctionsWorkersHub().scheduleSubmitFeedback()
        }
    }

    private fun handleSyncDataFiles(activity: JoozdlogActivity) {
        taskFlags.syncDataFiles.flow.doIfTrueEmitted(activity) {
            ServerFunctionsWorkersHub().scheduleSyncDataFiles()
        }
    }


    private val validEmailFlow = combine (EmailPrefs.emailAddress.flow, EmailPrefs.emailVerified.flow){
        address, verified -> address.isNotBlank() && verified
    }


    private fun emailConfirmationWantedFlow() = combine(taskFlags.verifyEmailCode.flow, TaskPayloads.emailConfirmationStringWaiting.flow){
        wanted, value -> (wanted && value.isNotBlank())
    }

    private val emailUpdateWantedFlow = combine(taskFlags.updateEmailWithServer.flow, EmailPrefs.emailAddress.flow){
        wanted, address -> wanted && address.isNotBlank().also { println("emailUpdateWantedFlow might emit $it") }
    }

    private fun backupEmailWantedFlow() = combine(taskFlags.sendBackupEmail.flow, validEmailFlow){
        wanted, valid -> wanted && valid
    }

    private fun feedbackReadyFlow() = combine(taskFlags.feedbackWaiting.flow, TaskPayloads.feedbackWaiting.flow){
        wanted, feedback -> wanted && feedback.isNotBlank()
    }

    companion object{
        val instance by lazy{
            TaskDispatcher()
        }
        fun mock(taskFlags: TaskFlags) = TaskDispatcher(taskFlags)
    }
}