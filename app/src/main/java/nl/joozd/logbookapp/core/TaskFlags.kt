package nl.joozd.logbookapp.core

import nl.joozd.logbookapp.data.sharedPrefs.JoozdLogPreferences
import nl.joozd.logbookapp.data.sharedPrefs.JoozdlogSharedPreferenceDelegate

/**
 * Every task represents something that needs to be done.
 * TaskDispatcher will observe these and dispatch those tasks to whomever is supposed to handle them.
 */
object TaskFlags: JoozdLogPreferences() {
    override val preferencesFileKey = "nl.joozd.logbookapp.TASK_FLAGS_FILE_KEY"

    private const val SEND_BACKUP_EMAIL = "SEND_BACKUP_EMAIL"
    private const val VERIFY_EMAIL_CODE = "VERIFY_EMAIL_CODE"
    private const val UPDATE_EMAIL = "UPDATE_EMAIL"
    private const val SEND_LOGIN_LINK = "SEND_LOGIN_LINK"
    private const val CREATE_NEW_USER = "CREATE_NEW_USER"
    private const val SYNC_FLIGHTS = "SYNC_FLIGHTS"
    private const val SYNC_DATA_FILES = "SYNC_DATA_FILES"
    private const val FEEDBACK_WAITING = "FEEDBACK_WAITING"
    private const val MERGE_ALL_DATA_FROM_SERVER = "MERGE_ALL_DATA_FROM_SERVER"


    val sendBackupEmail by JoozdlogSharedPreferenceDelegate(SEND_BACKUP_EMAIL,false)
    val verifyEmailCode by JoozdlogSharedPreferenceDelegate(VERIFY_EMAIL_CODE, false)
    val updateEmailWithServer by JoozdlogSharedPreferenceDelegate(UPDATE_EMAIL, false)
    val sendLoginLink by JoozdlogSharedPreferenceDelegate(SEND_LOGIN_LINK,false)
    val createNewUserAndEnableCloud by JoozdlogSharedPreferenceDelegate(CREATE_NEW_USER,false, debug = true) // create a new user both local and on server
    val feedbackWaiting by JoozdlogSharedPreferenceDelegate(FEEDBACK_WAITING, false)
    val syncDataFiles by JoozdlogSharedPreferenceDelegate(SYNC_DATA_FILES,false)
    val syncFlights by JoozdlogSharedPreferenceDelegate(SYNC_FLIGHTS, false)
    val mergeAllDataFromServer by JoozdlogSharedPreferenceDelegate(MERGE_ALL_DATA_FROM_SERVER, false)
}