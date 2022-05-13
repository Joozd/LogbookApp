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
                //TODO also: take care of handling unwanted situations like bad login data, unaccepted t&c's, etc.
                newUserWanted()
                verificationEmailWanted()

                //TODO below this line needs reworking
                backupEmailWanted()
                loginLinkWanted()
            }
        }
    }

    private suspend fun newUserWanted() {
        newUserWantedFlow.collect{
            if(it)
                UserManagementWorkersHub().scheduleCreateNewUser()
        }
    }

    private suspend fun verificationEmailWanted() {
        verificationMailWantedFlow.collect{
            if(it)
                UserManagementWorkersHub().scheduleRequestEmailVerificationMail()
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



    private val newUserWantedFlow = combine(TaskFlags.createNewUserFlow, useCloudFlow) {
            needed, enabled -> needed && enabled
    }

    private val verificationMailWantedFlow = combine(useCloudFlow, EmailPrefs.emailAddressFlow, TaskFlags.requestVerificationEmailFlow){
        enabled, address, wanted -> enabled && address.isNotBlank() && wanted

    }

    private val loginLinkWantedFlow = combine(validEmailFlow, TaskFlags.sendLoginLinkFlow){
        valid, wanted -> valid && wanted
    }

    private val backupEmailWantedFlow = combine(validEmailFlow, TaskFlags.sendBackupEmailFlow){
        valid, wanted -> valid && wanted
    }


}