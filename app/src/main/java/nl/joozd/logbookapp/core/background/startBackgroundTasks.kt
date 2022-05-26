package nl.joozd.logbookapp.core.background

import nl.joozd.logbookapp.ui.utils.JoozdlogActivity

/**
 * Start all background tasks
 */
fun JoozdlogActivity.startBackgroundTasks() {
    TaskDispatcher().start(this)
    PersistentMessagesDispatcher().start(this)
    BackupCenter().makeOrScheduleBackupNotification(this)
}