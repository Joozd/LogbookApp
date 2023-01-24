package nl.joozd.logbookapp.core.background

import nl.joozd.logbookapp.core.BackupCenter
import nl.joozd.logbookapp.core.SyncCenter
import nl.joozd.logbookapp.ui.utils.JoozdlogActivity

/**
 * Start all background tasks
 */
fun JoozdlogActivity.startBackgroundTasks() {
    SyncCenter().syncDataFiles()
    TaskDispatcher.instance.start(this)
    BackupCenter.instance.makeOrScheduleBackupNotification(this)
}