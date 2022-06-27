package nl.joozd.logbookapp.core.background

import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.flow.combine
import nl.joozd.logbookapp.core.TaskFlags
import nl.joozd.logbookapp.data.sharedPrefs.ServerPrefs
import nl.joozd.logbookapp.data.sharedPrefs.Prefs
import nl.joozd.logbookapp.data.sharedPrefs.TaskPayloads
import nl.joozd.logbookapp.ui.utils.JoozdlogActivity
import nl.joozd.logbookapp.workmanager.ServerFunctionsWorkersHub

/**
 * Run all tasks set in [taskFlags].
 * All tasks will go straight to Worker as none is very time-sensitive.
 */
class TaskDispatcher private constructor(private val taskFlags: TaskFlags = TaskFlags): BackgroundTasksDispatcher() {
    override fun startCollectors(activity: JoozdlogActivity) {
        println("Starting Collectors! XOXOXOXOXOXOXOXOXOXOXOXOXO")
        //these functions collect their respective Flow and handle that flow's output.
        handleNewUserWanted(activity)
        handleEmailUpdateWanted(activity)
        handleEmailConfirmationWanted(activity)
        handleBackupEmailWanted(activity)
        handleLoginLinkWanted(activity)
        handleFeedbackWaiting(activity)
        handleSyncDataFiles(activity)
        handleSyncFlights(activity)
        handleMergeAllDataFromServer(activity)
    }

    private fun handleNewUserWanted(activity: JoozdlogActivity) {
        println("Collecting handleNewUserWanted")
        taskFlags.createNewUserAndEnableCloud.flow.doIfTrueEmitted(activity) {
            println("taskFlags.createNewUserAndEnableCloud emitted true!")
            ServerFunctionsWorkersHub().scheduleCreateNewUser()
        }
    }

    private fun handleEmailUpdateWanted(activity: JoozdlogActivity) {
        emailUpdateWantedFlow().doIfTrueEmitted(activity) {
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

    private fun handleLoginLinkWanted(activity: JoozdlogActivity) {
        loginLinkWantedFlow().doIfTrueEmitted(activity) {
            ServerFunctionsWorkersHub().scheduleLoginLinkEmail()
        }
    }

    private fun handleFeedbackWaiting(activity: JoozdlogActivity) {
        taskFlags.feedbackWaiting.flow.doIfTrueEmitted(activity) {
            ServerFunctionsWorkersHub().scheduleSubmitFeedback()
        }
    }

    private fun handleSyncDataFiles(activity: JoozdlogActivity) {
        taskFlags.syncDataFiles.flow.doIfTrueEmitted(activity) {
            ServerFunctionsWorkersHub().scheduleSyncDataFiles()
        }
    }

    private fun handleSyncFlights(activity: JoozdlogActivity){
        syncNeededFlow().doIfTrueEmitted(activity) {
            ServerFunctionsWorkersHub().scheduleSyncFlights()
        }
    }

    private fun handleMergeAllDataFromServer(activity: JoozdlogActivity){
        mergeNeededFlow().doIfTrueEmitted(activity){
            println("Merge Needed emitted true!")
            ServerFunctionsWorkersHub().scheduleMerge()
        }
    }


    private val validEmailFlow = combine (ServerPrefs.emailAddress.flow, ServerPrefs.emailVerified.flow){
        address, verified -> address.isNotBlank() && verified
    }

    private val useCloudFlow = combine(Prefs.useCloud.flow, Prefs.acceptedCloudSyncTerms.flow){
        useCloud, acceptedTerms -> useCloud && acceptedTerms
    }

    private fun emailConfirmationWantedFlow() = combine(taskFlags.verifyEmailCode.flow, useCloudFlow, TaskPayloads.emailConfirmationStringWaiting.flow){
        wanted, enabled, value -> wanted && enabled && value.isNotBlank()
    }

    private fun emailUpdateWantedFlow() = combine(taskFlags.updateEmailWithServer.flow, useCloudFlow, ServerPrefs.emailAddress.flow){
        wanted, enabled, address -> wanted && enabled && address.isNotBlank()
    }

    private fun loginLinkWantedFlow() = combine(taskFlags.sendLoginLink.flow, useCloudFlow, validEmailFlow ){
        wanted, enabled, valid -> wanted && enabled && valid
    }

    private fun backupEmailWantedFlow() = combine(taskFlags.sendBackupEmail.flow, useCloudFlow, validEmailFlow){
        wanted, enabled, valid -> wanted && enabled && valid
    }

    private fun syncNeededFlow() = combine(taskFlags.syncFlights.flow, taskFlags.mergeAllDataFromServer.flow, useCloudFlow){ needed, mergeNeeded, enabled ->
        needed && !mergeNeeded && enabled
    }

    private fun mergeNeededFlow() = combine(taskFlags.mergeAllDataFromServer.flow, useCloudFlow){ needed, enabled ->
        needed && enabled
    }

    companion object{
        val instance by lazy{
            TaskDispatcher()
        }
        fun mock(taskFlags: TaskFlags) = TaskDispatcher(taskFlags)
    }
}