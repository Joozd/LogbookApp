package nl.joozd.logbookapp.core.background

import kotlinx.coroutines.flow.combine
import nl.joozd.logbookapp.core.TaskFlags
import nl.joozd.logbookapp.data.sharedPrefs.ServerPrefs
import nl.joozd.logbookapp.data.sharedPrefs.Prefs
import nl.joozd.logbookapp.workmanager.userManagementWorkers.ServerFunctionsWorkersHub

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

    private suspend fun handleSyncDataFiles() {
        TaskFlags.syncDataFilesFlow.doIfTrueCollected {
            ServerFunctionsWorkersHub().scheduleSyncDataFiles()
        }
    }





    private val validEmailFlow = combine (ServerPrefs.emailAddressFlow, ServerPrefs.emailVerifiedFlow){
        address, verified -> address.isNotBlank() && verified
    }

    private val useCloudFlow = combine(Prefs.useCloudFlow, Prefs.acceptedCloudSyncTermsFlow){
        useCloud, acceptedTerms -> useCloud && acceptedTerms
    }

    private fun emailConfirmationWantedFlow() = combine(TaskFlags.verifyEmailCodeFlow, useCloudFlow, ServerPrefs.emailConfirmationStringWaitingFlow){
        wanted, enabled, value -> wanted && enabled && value.isNotBlank()
    }

    private fun newUserWantedFlow() = combine(TaskFlags.createNewUserFlow, useCloudFlow) {
        needed, enabled -> needed && enabled
    }

    private fun emailUpdateWantedFlow() = combine(TaskFlags.updateEmailWithServerFlow, useCloudFlow, ServerPrefs.emailAddressFlow){
        wanted, enabled, address -> wanted && enabled && address.isNotBlank()
    }

    private fun loginLinkWantedFlow() = combine(TaskFlags.sendLoginLinkFlow, useCloudFlow, validEmailFlow ){
        wanted, enabled, valid -> wanted && enabled && valid
    }

    private fun backupEmailWantedFlow() = combine(TaskFlags.sendBackupEmailFlow, useCloudFlow, validEmailFlow){
        wanted, enabled, valid -> wanted && enabled && valid
    }


}