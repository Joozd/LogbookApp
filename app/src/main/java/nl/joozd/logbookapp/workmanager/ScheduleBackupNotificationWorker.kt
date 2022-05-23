package nl.joozd.logbookapp.workmanager

import android.content.Context
import androidx.work.CoroutineWorker
import androidx.work.WorkerParameters
import kotlinx.coroutines.flow.first
import nl.joozd.logbookapp.core.messages.MessageCenter
import nl.joozd.logbookapp.data.sharedPrefs.BackupPrefs
import nl.joozd.logbookapp.ui.fragments.BackupNotificationFragment
import java.time.Instant

class ScheduleBackupNotificationWorker(appContext: Context, workerParams: WorkerParameters)
    : CoroutineWorker(appContext, workerParams) {
    override suspend fun doWork(): Result {
        if (BackupPrefs.nextBackupNeededFlow.first() >= Instant.now().epochSecond) // check if still needed
            MessageCenter.pushMessageBarFragment(BackupNotificationFragment())
        return Result.success()
    }

    companion object{
        const val TIME = "TIME"
        const val NOT_SET = -1L
    }
}