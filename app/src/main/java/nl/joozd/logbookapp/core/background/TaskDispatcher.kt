package nl.joozd.logbookapp.core.background

import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.flow.combine
import nl.joozd.logbookapp.core.TaskFlags
import nl.joozd.logbookapp.data.sharedPrefs.ServerPrefs
import nl.joozd.logbookapp.data.sharedPrefs.Prefs
import nl.joozd.logbookapp.data.sharedPrefs.TaskPayloads
import nl.joozd.logbookapp.workmanager.ServerFunctionsWorkersHub

/**
 * Run all tasks set in [TaskFlags].
 * All tasks will go straight to Worker as none is very time-sensitive.
 */
class TaskDispatcher: BackgroundTasksDispatcher() {
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
    }

    private fun handleNewUserWanted(scope: CoroutineScope) {
        newUserWantedFlow().launchDoIfTrueCollected(scope) {
                ServerFunctionsWorkersHub().scheduleCreateNewUser()
        }
    }

    private fun handleEmailUpdateWanted(scope: CoroutineScope) {
        emailUpdateWantedFlow().launchDoIfTrueCollected(scope) {
            ServerFunctionsWorkersHub().scheduleUpdateEmail()
        }
    }



    private fun handleEmailConfirmationWanted(scope: CoroutineScope) {
        emailConfirmationWantedFlow().launchDoIfTrueCollected(scope) {
            ServerFunctionsWorkersHub().scheduleConfirmEmail() // Worker takes care of checking for bad email confirmation string to prevent infinite loop.
        }
    }

    private fun handleBackupEmailWanted(scope: CoroutineScope) {
        backupEmailWantedFlow().launchDoIfTrueCollected(scope) {
            ServerFunctionsWorkersHub().scheduleBackupEmail()
        }
    }

    private fun handleLoginLinkWanted(scope: CoroutineScope) {
        loginLinkWantedFlow().launchDoIfTrueCollected(scope) {
            ServerFunctionsWorkersHub().scheduleLoginLinkEmail()
        }
    }

    private fun handleFeedbackWaiting(scope: CoroutineScope) {
        TaskFlags.feedbackWaiting.flow.launchDoIfTrueCollected(scope) {
            ServerFunctionsWorkersHub().scheduleSubmitFeedback()
        }
    }

    private fun handleSyncDataFiles(scope: CoroutineScope) {
        TaskFlags.syncDataFiles.flow.launchDoIfTrueCollected(scope) {
            ServerFunctionsWorkersHub().scheduleSyncDataFiles()
        }
    }

    private fun handleSyncFlights(scope: CoroutineScope){
        syncNeededFlow().launchDoIfTrueCollected(scope) {
            ServerFunctionsWorkersHub().scheduleSyncFlights()
        }
    }


    private val validEmailFlow = combine (ServerPrefs.emailAddress.flow, ServerPrefs.emailVerified.flow){
        address, verified -> address.isNotBlank() && verified
    }

    private val useCloudFlow = combine(Prefs.useCloud.flow, Prefs.acceptedCloudSyncTerms.flow){
        useCloud, acceptedTerms -> useCloud && acceptedTerms
    }

    private fun emailConfirmationWantedFlow() = combine(TaskFlags.verifyEmailCode.flow, useCloudFlow, TaskPayloads.emailConfirmationStringWaiting.flow){
        wanted, enabled, value -> wanted && enabled && value.isNotBlank()
    }

    private fun newUserWantedFlow() = combine(TaskFlags.createNewUser.flow, useCloudFlow) {
        needed, enabled -> needed && enabled
    }

    private fun emailUpdateWantedFlow() = combine(TaskFlags.updateEmailWithServer.flow, useCloudFlow, ServerPrefs.emailAddress.flow){
        wanted, enabled, address -> wanted && enabled && address.isNotBlank()
    }

    private fun loginLinkWantedFlow() = combine(TaskFlags.sendLoginLink.flow, useCloudFlow, validEmailFlow ){
        wanted, enabled, valid -> wanted && enabled && valid
    }

    private fun backupEmailWantedFlow() = combine(TaskFlags.sendBackupEmail.flow, useCloudFlow, validEmailFlow){
        wanted, enabled, valid -> wanted && enabled && valid
    }

    private fun syncNeededFlow() = combine(TaskFlags.syncFlights.flow, useCloudFlow){ needed, enabled ->
        needed && enabled
    }
}