package nl.joozd.logbookapp.core.background

import nl.joozd.logbookapp.ui.utils.JoozdlogActivity

/**
 * Start all background tasks
 */
fun startBackgroundTasks(activity: JoozdlogActivity) {
    TaskDispatcher(activity).start()
    BackupCenter(activity).makeOrScheduleBackupNotification()
}