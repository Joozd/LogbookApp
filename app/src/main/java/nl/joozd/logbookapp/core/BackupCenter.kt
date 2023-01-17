package nl.joozd.logbookapp.core

import android.net.Uri
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.lifecycleScope
import androidx.lifecycle.repeatOnLifecycle
import androidx.work.ExistingWorkPolicy
import androidx.work.OneTimeWorkRequestBuilder
import androidx.work.WorkManager
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.launch
import nl.joozd.logbookapp.data.sharedPrefs.EmailPrefs
import nl.joozd.logbookapp.core.Constants.ONE_DAY_IN_SECONDS
import nl.joozd.logbookapp.core.messages.MessageBarMessage
import nl.joozd.logbookapp.data.export.JoozdlogExport
import nl.joozd.logbookapp.data.sharedPrefs.BackupPrefs
import nl.joozd.logbookapp.data.sharedPrefs.Prefs
import nl.joozd.logbookapp.extensions.toDateStringForFiles
import nl.joozd.logbookapp.ui.utils.JoozdlogActivity
import nl.joozd.logbookapp.workmanager.ScheduleBackupNotificationWorker
import java.time.Duration
import java.time.Instant
import java.time.LocalDate

/**
 * Takes care of all things backup that need to be done in background.
 */
class BackupCenter private constructor(private val emailCenter: EmailCenter = EmailCenter()) {
    /*
     * This will trigger again when relevant preferences are updated
     * @see backupActionFlow for which preferences are monitored.
     */
    // This checks to see what kind of action is needed:
    // - Send a backup email
    // - notify user
    // - schedule a backup email
    // - do nothing
    fun makeOrScheduleBackupNotification(activity: JoozdlogActivity) {
        activity.lifecycleScope.launch {
            activity.repeatOnLifecycle(Lifecycle.State.STARTED) {
                backupActionFlow().collect { backupAction ->
                    when (backupAction) {
                        is BackupAction.DISABLED -> { /* do nothing */ }
                        is BackupAction.NOTIFY -> {
                            if (backupAction.emailNeeded) {
                                /*
                                   try to send a backup email.
                                   Doing this will make backupActionFlow emit again (because that combines TaskFlags.sendBackupEmail which is set here).
                                   backupActionFlow will emit either NOTIFY(emailNeeded = false) or SCHEDULE.
                                 */
                                TaskFlags.sendBackupEmail(true)
                            } else MessageCenter.scheduleMessage(MessageBarMessage.BACKUP_NEEDED)
                        }
                        BackupAction.EMAIL -> emailCenter.scheduleBackupEmail()
                        // SCHEDULE will reschedule in case a successful backup has been made,
                        // because a successful backup will update a flow which eventually makes backupActionFlow emit again.
                        is BackupAction.SCHEDULE -> scheduleBackupNotification(backupAction.delay)
                    }
                }
            }
        }
    }

    private fun scheduleBackupNotification(delay: Long){
        MessageCenter.unscheduleMessage(MessageBarMessage.BACKUP_NEEDED)
        val task = OneTimeWorkRequestBuilder<ScheduleBackupNotificationWorker>()
            .addTag(BACKUP_NOTIFICATION_TAG)
            .setInitialDelay(Duration.ofSeconds(delay))
            .build()

        WorkManager.getInstance(App.instance)
            .enqueueUniqueWork(BACKUP_NOTIFICATION_TAG, ExistingWorkPolicy.REPLACE, task)

    }

    private fun backupActionFlow(): Flow<BackupAction> =
        combine(BackupPrefs.nextBackupNeededFlow, TaskFlags.sendBackupEmail.flow, backupEmailEnabledFlow()) {
            backupNeededAt, emailBackupAlreadyScheduled, emailBackupEnabled ->
                if(backupNeededAt == null) return@combine BackupAction.DISABLED
                val backupOverdueBy = Instant.now().epochSecond - backupNeededAt
                when {
                    backupNotificationNeeded(emailBackupEnabled, backupOverdueBy) -> BackupAction.NOTIFY(
                        backupEmailNeeded(backupOverdueBy, emailBackupAlreadyScheduled, emailBackupEnabled)
                    )
                    backupEmailNeeded(backupOverdueBy, emailBackupAlreadyScheduled, emailBackupEnabled) -> BackupAction.EMAIL
                    else -> BackupAction.SCHEDULE(backupOverdueBy * -1)
                }
        }

    private fun backupEmailEnabledFlow() = combine(EmailPrefs.emailAddress.flow, EmailPrefs.emailVerified.flow, Prefs.sendBackupEmails.flow){
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
        object DISABLED: BackupAction
        class NOTIFY(val emailNeeded: Boolean): BackupAction
        class SCHEDULE(val delay: Long): BackupAction
    }


}