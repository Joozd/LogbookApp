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
import nl.joozd.logbookapp.data.sharedPrefs.EmailPrefs
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
class BackupCenter(private val activity: JoozdlogActivity) {
    /**
     * This will trigger again when relevant preferences are updated
     * @see backupActionFlow for which preferences are monitored.
     */
    fun makeOrScheduleBackupNotification() {
        activity.lifecycleScope.launch {
            activity.repeatOnLifecycle(Lifecycle.State.STARTED) {
                backupActionFlow.collect { backupAction ->
                    when (backupAction) {
                        is BackupAction.NOTIFY -> {
                            if (backupAction.emailNeeded) {
                                //try to send a backup email, give it a second to see if it works, else retry.
                                TaskFlags.postSendBackupEmail(true)
                                delay(5000)
                                makeOrScheduleBackupNotification() // call this recursively, if TaskFlags.sendBackupEmail == true, this will go to [else] statement below this line.
                            } else MessageCenter.pushMessageBarFragment(BackupNotificationFragment())
                        }
                        BackupAction.EMAIL -> TaskFlags.postSendBackupEmail(true)
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
        combine(BackupPrefs.nextBackupNeededFlow, TaskFlags.sendBackupEmailFlow, EmailPrefs.backupEmailEnabledFlow) {
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

    private fun backupEmailNeeded(backupOverdueBy: Long, alreadyScheduled: Boolean,  backupFromCloudEnabled: Boolean) =
        backupOverdueBy > 0                             // backup is overdue
                && backupFromCloudEnabled
                && !alreadyScheduled                    // a request for a backup email is not still pending

    private fun backupNotificationNeeded(backupFromCloud: Boolean, backupOverdueBy: Long) =
        if (backupFromCloud) backupOverdueBy > ONE_DAY_IN_SECONDS
        else backupOverdueBy > 0


    companion object{
        private const val BACKUP_NOTIFICATION_TAG = "BACKUP_NOTIFICATION_TAG"

        suspend fun makeBackupUri(): Uri {
            val dateString = LocalDate.now().toDateStringForFiles()
            BackupPrefs.backupIgnoredUntil = 0
            BackupPrefs.mostRecentBackup = Instant.now().epochSecond
            return JoozdlogExport.shareCsvExport("joozdlog_backup_$dateString")
        }
    }

    private sealed interface BackupAction {
        object EMAIL: BackupAction
        class NOTIFY(val emailNeeded: Boolean): BackupAction
        class SCHEDULE(val delay: Long): BackupAction
    }


}