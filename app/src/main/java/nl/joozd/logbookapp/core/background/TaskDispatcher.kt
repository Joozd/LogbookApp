package nl.joozd.logbookapp.core.background

import androidx.lifecycle.Lifecycle
import androidx.lifecycle.lifecycleScope
import androidx.lifecycle.repeatOnLifecycle
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.launch
import nl.joozd.logbookapp.core.JoozdlogWorkersHub
import nl.joozd.logbookapp.core.TaskFlags
import nl.joozd.logbookapp.data.comm.OldCloud
import nl.joozd.logbookapp.data.comm.InternetStatus
import nl.joozd.logbookapp.data.sharedPrefs.EmailPrefs
import nl.joozd.logbookapp.ui.utils.JoozdlogActivity

/**
 * Run all tasks set in [TaskFlags].
 */
class TaskDispatcher(private val activity: JoozdlogActivity) {
    fun start(){
        activity.lifecycleScope.launch{
            activity.repeatOnLifecycle(Lifecycle.State.STARTED) {
                backupEmailWantedFlow.collect {
                    if (it) {
                        if (InternetStatus.internetAvailable == true)
                            OldCloud.requestBackupEmail()
                        else {
                            JoozdlogWorkersHub.scheduleBackupEmail()
                        }
                    }
                }
                loginLinkWantedFlow.collect{
                    if (it){
                        if (InternetStatus.internetAvailable == true)
                            OldCloud.requestBackupEmail()
                        else {
                            JoozdlogWorkersHub.scheduleLoginLinkEmail()
                        }
                    }
                }
            }
        }
    }

    private val validEmailFlow = combine (EmailPrefs.emailAddressFlow, EmailPrefs.emailVerifiedFlow){
        a, v ->
        a.isNotBlank() && v
    }

    private val loginLinkWantedFlow = combine(validEmailFlow, TaskFlags.sendLoginLinkFlow){
        valid, wanted -> valid && wanted
    }

    private val backupEmailWantedFlow = combine(validEmailFlow, TaskFlags.sendBackupEmailFlow){
        valid, wanted -> valid && wanted
    }
}