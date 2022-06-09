package nl.joozd.logbookapp.core.background

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
    override suspend fun startCollectors() {
        //these functions collect their respective Flow and handle that flow's output.
        handleNewUserWanted()
        handleEmailUpdateWanted()
        handleEmailConfirmationWanted()
        handleBackupEmailWanted()
        handleLoginLinkWanted()
        handleFeedbackWaiting()
        handleSyncDataFiles()
        handleSyncFlights()
    }

    private suspend fun handleNewUserWanted() {
        newUserWantedFlow().doIfTrueCollected {
                ServerFunctionsWorkersHub().scheduleCreateNewUser()
        }
    }

    private suspend fun handleEmailUpdateWanted() {
        emailUpdateWantedFlow().doIfTrueCollected {
            ServerFunctionsWorkersHub().scheduleUpdateEmail()
        }
    }



    private suspend fun handleEmailConfirmationWanted() {
        emailConfirmationWantedFlow().doIfTrueCollected {
            ServerFunctionsWorkersHub().scheduleConfirmEmail() // Worker takes care of checking for bad email confirmation string to prevent infinite loop.
        }
    }

    private suspend fun handleBackupEmailWanted() {
        backupEmailWantedFlow().doIfTrueCollected {
            ServerFunctionsWorkersHub().scheduleBackupEmail()
        }
    }

    private suspend fun handleLoginLinkWanted() {
        loginLinkWantedFlow().doIfTrueCollected {
            ServerFunctionsWorkersHub().scheduleLoginLinkEmail()
        }
    }

    private suspend fun handleFeedbackWaiting() {
        TaskFlags.feedbackWaiting.flow.doIfTrueCollected {
            ServerFunctionsWorkersHub().scheduleSubmitFeedback()
        }
    }

    private suspend fun handleSyncDataFiles() {
        TaskFlags.syncDataFiles.flow.doIfTrueCollected {
            ServerFunctionsWorkersHub().scheduleSyncDataFiles()
        }
    }

    private suspend fun handleSyncFlights(){
        syncNeededFlow().doIfTrueCollected {
            ServerFunctionsWorkersHub().scheduleSyncFlights()
        }
    }





    private val validEmailFlow = combine (ServerPrefs.emailAddressFlow, ServerPrefs.emailVerifiedFlow){
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

    private fun emailUpdateWantedFlow() = combine(TaskFlags.updateEmailWithServer.flow, useCloudFlow, ServerPrefs.emailAddressFlow){
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