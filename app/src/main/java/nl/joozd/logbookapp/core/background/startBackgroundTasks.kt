package nl.joozd.logbookapp.core.background

import androidx.lifecycle.lifecycleScope
import nl.joozd.logbookapp.ui.utils.JoozdlogActivity

/**
 * Start all background tasks
 */
fun JoozdlogActivity.startBackgroundTasks() {
    TaskDispatcher.instance.start(this)
    PersistentMessagesDispatcher.instance.start(this)
    BackupCenter.instance.makeOrScheduleBackupNotification(this)
    SyncCenter.instance.launchSyncAllIfNotJustDone(this.lifecycleScope)
}