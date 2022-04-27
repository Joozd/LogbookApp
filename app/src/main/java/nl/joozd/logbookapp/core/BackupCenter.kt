package nl.joozd.logbookapp.core

import androidx.work.ExistingWorkPolicy
import androidx.work.OneTimeWorkRequestBuilder
import androidx.work.WorkManager
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.MainScope
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.launch
import nl.joozd.logbookapp.core.Constants.ONE_DAY_IN_SECONDS
import nl.joozd.logbookapp.data.comm.Cloud
import nl.joozd.logbookapp.data.export.JoozdlogExport
import nl.joozd.logbookapp.data.sharedPrefs.BackupPrefs
import nl.joozd.logbookapp.data.sharedPrefs.Prefs
import nl.joozd.logbookapp.extensions.toDateStringForFiles
import nl.joozd.logbookapp.model.viewmodels.status.BackupCenterStatus
import nl.joozd.logbookapp.ui.fragments.BackupNotificationFragment
import nl.joozd.logbookapp.utils.CastFlowToMutableFlowShortcut
import nl.joozd.logbookapp.workmanager.ScheduleBackupNotificationWorker
import java.time.Duration
import java.time.Instant
import java.time.LocalDate

object BackupCenter: CoroutineScope by MainScope() {
    val statusFlow: StateFlow<BackupCenterStatus?> = MutableStateFlow(null)
    private var status by CastFlowToMutableFlowShortcut(statusFlow)

    fun putBackupUriInStatus() = launch {
        val dateString = LocalDate.now().toDateStringForFiles()
        status = BackupCenterStatus.BuildingCsv
        val uri = JoozdlogExport.shareCsvExport("joozdlog_backup_$dateString")
        status = BackupCenterStatus.SharedUri(uri)
        BackupPrefs.backupIgnoredExtraDays = 0
        BackupPrefs.mostRecentBackup = Instant.now().epochSecond
    }

    /**
     * Reset status.
     */
    fun reset(){
        status = null
    }

    /**
     * This will trigger again when relevant preferences are updated
     * @see backupActionFlow for which preferences are monitored.
     */
    fun makeOrScheduleBackupNotification() =
        launch{
            reset()
            backupActionFlow.collect{ backupAction ->
                when(backupAction){
                    BackupAction.NOTIFY -> MessageCenter.pushMessageBarFragment(BackupNotificationFragment())
                    BackupAction.EMAIL -> Cloud.requestBackupEmail()
                    is BackupAction.SCHEDULE -> scheduleBackupNotification(backupAction.notifyInThisManySeconds)
                }
            }
        }

    private fun scheduleBackupNotification(atTime: Long){
        val task = OneTimeWorkRequestBuilder<ScheduleBackupNotificationWorker>()
            .addTag(BACKUP_NOTIFICATION_TAG)
            .setInitialDelay(Duration.between(Instant.now(), Instant.ofEpochSecond(atTime)))
            .build()

        WorkManager.getInstance(App.instance)
            .enqueueUniqueWork(BACKUP_NOTIFICATION_TAG, ExistingWorkPolicy.REPLACE, task)

    }

    private val backupActionFlow: Flow<BackupAction> =
        combine(BackupPrefs.nextBackupNeededFlow, Prefs.backupFromCloudFlow) {
            backupNeededAt, backupFromCloud ->
                val backupOverdueBy = Instant.now().epochSecond - backupNeededAt
                println("DEBUG: Backup overdue by $backupOverdueBy seconds")
                when {
                    backupNotificationNeeded(backupFromCloud, backupOverdueBy) -> BackupAction.NOTIFY
                    backupEmailNeeded(backupOverdueBy, backupFromCloud) -> BackupAction.EMAIL
                    else -> BackupAction.SCHEDULE(backupOverdueBy * -1)
                }.also{
                    println("backupActionFlow emitted $it")
                }
        }

    private fun backupEmailNeeded(backupOverdueBy: Long, backupFromCloud: Boolean) =
        backupOverdueBy > 0 && backupFromCloud

    private fun backupNotificationNeeded(backupFromCloud: Boolean, backupOverdueBy: Long) =
        (backupFromCloud && backupOverdueBy > ONE_DAY_IN_SECONDS) || !backupFromCloud && backupOverdueBy > 0


    private const val BACKUP_NOTIFICATION_TAG = "BACKUP_NOTIFICATION_TAG"

    private sealed class BackupAction{
        object EMAIL: BackupAction()
        object NOTIFY: BackupAction()
        class SCHEDULE(val notifyInThisManySeconds: Long): BackupAction()
    }
}