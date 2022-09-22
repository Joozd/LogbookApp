package nl.joozd.logbookapp.core

import nl.joozd.logbookapp.data.sharedPrefs.JoozdLogPreferences
import nl.joozd.logbookapp.data.sharedPrefs.JoozdlogSharedPreferenceDelegate

/**
 * Every task represents something that needs to be done.
 * TaskDispatcher will observe these and dispatch those tasks to whomever is supposed to handle them.
 * Taskflags can be set by anybody who needs a task done
 * Taskflags will be reset by the function that calls the function we want to run (usually the worker)
 * Doc: scheduling.md
 */
object TaskFlags: JoozdLogPreferences() {
    override val preferencesFileKey = "nl.joozd.logbookapp.TASK_FLAGS_FILE_KEY"

    private const val SEND_BACKUP_EMAIL = "SEND_BACKUP_EMAIL"
    private const val VERIFY_EMAIL_CODE = "VERIFY_EMAIL_CODE"
    private const val UPDATE_EMAIL = "UPDATE_EMAIL"
    private const val SYNC_DATA_FILES = "SYNC_DATA_FILES"
    private const val FEEDBACK_WAITING = "FEEDBACK_WAITING"


    val sendBackupEmail by JoozdlogSharedPreferenceDelegate(SEND_BACKUP_EMAIL,false)
    val verifyEmailCode by JoozdlogSharedPreferenceDelegate(VERIFY_EMAIL_CODE, false)
    val updateEmailWithServer by JoozdlogSharedPreferenceDelegate(UPDATE_EMAIL, false)
    val feedbackWaiting by JoozdlogSharedPreferenceDelegate(FEEDBACK_WAITING, false)
    val syncDataFiles by JoozdlogSharedPreferenceDelegate(SYNC_DATA_FILES,false)
}