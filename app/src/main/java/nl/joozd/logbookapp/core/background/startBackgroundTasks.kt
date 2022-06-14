package nl.joozd.logbookapp.core.background

import androidx.lifecycle.lifecycleScope
import nl.joozd.logbookapp.ui.utils.JoozdlogActivity

/**
 * Start all background tasks
 */
fun JoozdlogActivity.startBackgroundTasks() {
    TaskDispatcher().start(this)
    PersistentMessagesDispatcher().start(this)
    BackupCenter().makeOrScheduleBackupNotification(this)
    SyncCenter().launchSyncAllIfNotJustDone(this.lifecycleScope)
}