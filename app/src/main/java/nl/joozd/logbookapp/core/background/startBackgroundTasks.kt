package nl.joozd.logbookapp.core.background

import androidx.lifecycle.lifecycleScope
import nl.joozd.logbookapp.ui.utils.JoozdlogActivity

/**
 * Start all background tasks
 */
fun JoozdlogActivity.startBackgroundTasks() {
    SyncCenter().syncDataFiles()
    TaskDispatcher.instance.start(this)
    PersistentMessagesDispatcher.instance.start(this)
    BackupCenter.instance.makeOrScheduleBackupNotification(this)
}