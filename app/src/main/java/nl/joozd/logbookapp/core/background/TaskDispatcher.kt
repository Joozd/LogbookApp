package nl.joozd.logbookapp.core.background

import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.flow.combine
import nl.joozd.logbookapp.core.TaskFlags
import nl.joozd.logbookapp.data.sharedPrefs.ServerPrefs
import nl.joozd.logbookapp.data.sharedPrefs.Prefs
import nl.joozd.logbookapp.data.sharedPrefs.TaskPayloads
import nl.joozd.logbookapp.workmanager.ServerFunctionsWorkersHub

/**
 * Run all tasks set in [taskFlags].
 * All tasks will go straight to Worker as none is very time-sensitive.
 */
class TaskDispatcher(private val taskFlags: TaskFlags = TaskFlags): BackgroundTasksDispatcher() {
    override fun startCollectors(scope: CoroutineScope) {
        //these functions collect their respective Flow and handle that flow's output.
        handleNewUserWanted(scope)
        handleEmailUpdateWanted(scope)
        handleEmailConfirmationWanted(scope)
        handleBackupEmailWanted(scope)
        handleLoginLinkWanted(scope)
        handleFeedbackWaiting(scope)
        handleSyncDataFiles(scope)
        handleSyncFlights(scope)
        handleMergeAllDataFromServer(scope)
    }

    private fun handleNewUserWanted(scope: CoroutineScope) {
        taskFlags.createNewUserAndEnableCloud.flow.doIfTrueEmitted(scope) {
                ServerFunctionsWorkersHub().scheduleCreateNewUser()
        }
    }

    private fun handleEmailUpdateWanted(scope: CoroutineScope) {
        emailUpdateWantedFlow().doIfTrueEmitted(scope) {
            ServerFunctionsWorkersHub().scheduleUpdateEmail()
        }
    }



    private fun handleEmailConfirmationWanted(scope: CoroutineScope) {
        emailConfirmationWantedFlow().doIfTrueEmitted(scope) {
            ServerFunctionsWorkersHub().scheduleConfirmEmail() // Worker takes care of checking for bad email confirmation string to prevent infinite loop.
        }
    }

    private fun handleBackupEmailWanted(scope: CoroutineScope) {
        backupEmailWantedFlow().doIfTrueEmitted(scope) {
            ServerFunctionsWorkersHub().scheduleBackupEmail()
        }
    }

    private fun handleLoginLinkWanted(scope: CoroutineScope) {
        loginLinkWantedFlow().doIfTrueEmitted(scope) {
            ServerFunctionsWorkersHub().scheduleLoginLinkEmail()
        }
    }

    private fun handleFeedbackWaiting(scope: CoroutineScope) {
        taskFlags.feedbackWaiting.flow.doIfTrueEmitted(scope) {
            ServerFunctionsWorkersHub().scheduleSubmitFeedback()
        }
    }

    private fun handleSyncDataFiles(scope: CoroutineScope) {
        taskFlags.syncDataFiles.flow.doIfTrueEmitted(scope) {
            ServerFunctionsWorkersHub().scheduleSyncDataFiles()
        }
    }

    private fun handleSyncFlights(scope: CoroutineScope){
        syncNeededFlow().doIfTrueEmitted(scope) {
            ServerFunctionsWorkersHub().scheduleSyncFlights()
        }
    }

    private fun handleMergeAllDataFromServer(scope: CoroutineScope){
        mergeNeededFlow().doIfTrueEmitted(scope){
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
}