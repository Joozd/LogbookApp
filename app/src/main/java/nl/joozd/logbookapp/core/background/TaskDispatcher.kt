package nl.joozd.logbookapp.core.background

import androidx.lifecycle.Lifecycle
import androidx.lifecycle.lifecycleScope
import androidx.lifecycle.repeatOnLifecycle
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.launch
import nl.joozd.logbookapp.core.JoozdlogWorkersHubOld
import nl.joozd.logbookapp.core.TaskFlags
import nl.joozd.logbookapp.data.sharedPrefs.EmailPrefs
import nl.joozd.logbookapp.data.sharedPrefs.Prefs
import nl.joozd.logbookapp.ui.utils.JoozdlogActivity
import nl.joozd.logbookapp.workmanager.userManagementWorkers.UserManagementWorkersHub

/**
 * Run all tasks set in [TaskFlags].
 * All tasks will go straight to Worker as none is very time-sensitive.
 */
class TaskDispatcher(private val activity: JoozdlogActivity) {
    fun start() {
        activity.lifecycleScope.launch {
            activity.repeatOnLifecycle(Lifecycle.State.STARTED) {
                //these functions collect their respective Flow and handle that flow's output.
                //TODO this one is the way I want it. Do the rest the same way.
                newUserWanted()
                emailUpdateWanted()
                emailConfirmationWanted() // Worker takes care of checking for bad email confirmation string to prevent infinite loop.

                //TODO below this line needs reworking
                backupEmailWanted()
                loginLinkWanted()
            }
        }
    }

    private suspend fun newUserWanted() {
        newUserWantedFlow().collect{
            if(it)
                UserManagementWorkersHub().scheduleCreateNewUser()
        }
    }

    private suspend fun emailUpdateWanted(){
        emailUpdateWantedFlow().collect{
            if(it){
                UserManagementWorkersHub().scheduleUpdateEmail()
            }
        }
    }

    private suspend fun emailConfirmationWanted() {
        emailConfirmationWantedFlow().collect{
            if(it)
                UserManagementWorkersHub().scheduleConfirmEmail()
        }
    }



    private suspend fun loginLinkWanted() {
        loginLinkWantedFlow.collect {
            if (it)
                JoozdlogWorkersHubOld.scheduleLoginLinkEmail()
        }
    }

    private suspend fun backupEmailWanted() {
        backupEmailWantedFlow.collect {
            if (it)
                JoozdlogWorkersHubOld.scheduleBackupEmail()
        }
    }


    private val validEmailFlow = combine (EmailPrefs.emailAddressFlow, EmailPrefs.emailVerifiedFlow){
        a, v -> a.isNotBlank() && v
    }

    private val useCloudFlow = combine(Prefs.useCloudFlow, Prefs.acceptedCloudSyncTermsFlow){
        use, accepted -> use && accepted
    }

    private fun emailConfirmationWantedFlow() = combine(TaskFlags.verifyEmailCodeFlow, Prefs.useCloudFlow, EmailPrefs.emailConfirmationStringWaitingFlow){
        wanted, enabled, value -> wanted && enabled && value.isNotBlank()
    }

    private fun newUserWantedFlow() = combine(TaskFlags.createNewUserFlow, useCloudFlow) {
            needed, enabled -> needed && enabled
    }

    private fun emailUpdateWantedFlow() = combine(TaskFlags.updateEmailWithServerFlow, Prefs.useCloudFlow, EmailPrefs.emailAddressFlow){
        wanted, enabled, address -> wanted && enabled && address.isNotBlank()
    }

    private val loginLinkWantedFlow = combine(validEmailFlow, TaskFlags.sendLoginLinkFlow){
        valid, wanted -> valid && wanted
    }

    private val backupEmailWantedFlow = combine(validEmailFlow, TaskFlags.sendBackupEmailFlow){
        valid, wanted -> valid && wanted
    }


}