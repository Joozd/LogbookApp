package nl.joozd.logbookapp.core.background

import android.net.Uri
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.lifecycleScope
import androidx.lifecycle.repeatOnLifecycle
import androidx.work.ExistingWorkPolicy
import androidx.work.OneTimeWorkRequestBuilder
import androidx.work.WorkManager
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.launch
import nl.joozd.logbookapp.core.App
import nl.joozd.logbookapp.core.Constants.ONE_DAY_IN_SECONDS
import nl.joozd.logbookapp.core.messages.MessageCenter
import nl.joozd.logbookapp.core.TaskFlags
import nl.joozd.logbookapp.data.export.JoozdlogExport
import nl.joozd.logbookapp.data.sharedPrefs.BackupPrefs
import nl.joozd.logbookapp.data.sharedPrefs.Prefs
import nl.joozd.logbookapp.data.sharedPrefs.ServerPrefs
import nl.joozd.logbookapp.extensions.toDateStringForFiles
import nl.joozd.logbookapp.ui.fragments.BackupNotificationFragment
import nl.joozd.logbookapp.ui.utils.JoozdlogActivity
import nl.joozd.logbookapp.workmanager.ScheduleBackupNotificationWorker
import java.time.Duration
import java.time.Instant
import java.time.LocalDate

/**
 * Takes care of all things backup that need to be done in background.
 */
class BackupCenter private constructor() {
    /**
     * This will trigger again when relevant preferences are updated
     * @see backupActionFlow for which preferences are monitored.
     */
    fun makeOrScheduleBackupNotification(activity: JoozdlogActivity) {
        activity.lifecycleScope.launch {
            activity.repeatOnLifecycle(Lifecycle.State.STARTED) {
                backupActionFlow.collect { backupAction ->
                    when (backupAction) {
                        is BackupAction.NOTIFY -> {
                            if (backupAction.emailNeeded) {
                                /*
                                   try to send a backup email.
                                   Doing this will make backupActionFlow emit again (because that combines TaskFlags.sendBackupEmail which is set here).
                                   backupActionFlow will emit either NOTIFY(emailNeeded = false) or SCHEDULE.
                                 */
                                TaskFlags.sendBackupEmail(true)
                            } else MessageCenter.pushMessageBarFragment(BackupNotificationFragment())
                        }
                        BackupAction.EMAIL -> TaskFlags.sendBackupEmail(true)
                        // SCHEDULE will reschedule in case a successful backup has been made,
                        // because a successful backup will update a flow which eventually makes backupActionFlow emit again.
                        is BackupAction.SCHEDULE -> scheduleBackupNotification(backupAction.delay)
                    }
                }
            }
        }
    }

    private fun scheduleBackupNotification(delay: Long){
        MessageCenter.pullMessageBarFragmentByTag(BACKUP_NOTIFICATION_TAG)
        val task = OneTimeWorkRequestBuilder<ScheduleBackupNotificationWorker>()
            .addTag(BACKUP_NOTIFICATION_TAG)
            .setInitialDelay(Duration.ofSeconds(delay))
            .build()

        WorkManager.getInstance(App.instance)
            .enqueueUniqueWork(BACKUP_NOTIFICATION_TAG, ExistingWorkPolicy.REPLACE, task)

    }

    private val backupActionFlow: Flow<BackupAction> =
        combine(BackupPrefs.nextBackupNeededFlow, TaskFlags.sendBackupEmail.flow, backupEmailEnabledFlow()) {
            backupNeededAt, emailBackupAlreadyScheduled, emailBackupEnabled ->
                val backupOverdueBy = Instant.now().epochSecond - backupNeededAt
                when {
                    backupNotificationNeeded(emailBackupEnabled, backupOverdueBy) -> BackupAction.NOTIFY(
                        backupEmailNeeded(backupOverdueBy, emailBackupAlreadyScheduled, emailBackupEnabled)
                    )
                    backupEmailNeeded(backupOverdueBy, emailBackupAlreadyScheduled, emailBackupEnabled) -> BackupAction.EMAIL
                    else -> BackupAction.SCHEDULE(backupOverdueBy * -1)
                }
        }

    private fun backupEmailEnabledFlow() = combine(ServerPrefs.emailAddress.flow, ServerPrefs.emailVerified.flow, Prefs.backupFromCloud.flow){
            address, verified, enabled ->
        address.isNotBlank() && verified && enabled
    }

    private fun backupEmailNeeded(backupOverdueBy: Long, alreadyScheduled: Boolean,  backupFromCloudEnabled: Boolean) =
        backupOverdueBy > 0                             // backup is overdue
                && backupFromCloudEnabled
                && !alreadyScheduled                    // a request for a backup email is not still pending


    // This will give an extra day margin if backupFromCloud is supposed to send a backup email, so users don't get a notification while an email is on the way.
    private fun backupNotificationNeeded(backupFromCloud: Boolean, backupOverdueBy: Long) =
        if (backupFromCloud) backupOverdueBy > ONE_DAY_IN_SECONDS
        else backupOverdueBy > 0


    companion object{
        val instance by lazy { BackupCenter() }

        private const val BACKUP_NOTIFICATION_TAG = "BACKUP_NOTIFICATION_TAG"

        suspend fun makeBackupUri(): Uri {
            val dateString = LocalDate.now().toDateStringForFiles()
            BackupPrefs.backupIgnoredUntil(0)
            BackupPrefs.mostRecentBackup(Instant.now().epochSecond)
            return JoozdlogExport.shareCsvExport("joozdlog_backup_$dateString")
        }
    }

    private sealed interface BackupAction {
        object EMAIL: BackupAction
        class NOTIFY(val emailNeeded: Boolean): BackupAction
        class SCHEDULE(val delay: Long): BackupAction
    }


}