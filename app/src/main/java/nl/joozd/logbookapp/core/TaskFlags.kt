package nl.joozd.logbookapp.core

import nl.joozd.logbookapp.data.sharedPrefs.JoozdLogPreferences
import nl.joozd.logbookapp.data.sharedPrefs.JoozdlogSharedPreferenceDelegate


// TODO change all of these to JoozdlogSharedPreferenceDelegate for cleaner code.
/**
 * Every task represents something that needs to be done.
 * TaskDispatcher will observe these and dispatch those tasks to whomever is supposed to handle them.
 */
object TaskFlags: JoozdLogPreferences() {
    override val preferencesFileKey = "nl.joozd.logbookapp.TASK_FLAGS_FILE_KEY"

    private const val CREATE_NEW_USER = "CREATE_NEW_USER"
    private const val SYNC_FLIGHTS = "SYNC_FLIGHTS"
    private const val SYNC_DATA_FILES = "SYNC_DATA_FILES"
    private const val FEEDBACK_WAITING = "FEEDBACK_WAITING"

    private const val SEND_BACKUP_EMAIL = "SEND_BACKUP_EMAIL"
    var sendBackupEmail by JoozdLogSharedPreferenceNotNull(SEND_BACKUP_EMAIL,false)
    val sendBackupEmailFlow by PrefsFlow(SEND_BACKUP_EMAIL, false)
    fun postSendBackupEmail(value: Boolean) = post(SEND_BACKUP_EMAIL, value)

    private const val VERIFY_EMAIL_CODE = "VERIFY_EMAIL_CODE"
    var verifyEmailCode by JoozdLogSharedPreferenceNotNull(VERIFY_EMAIL_CODE, false)
    val verifyEmailCodeFlow by PrefsFlow(VERIFY_EMAIL_CODE, false)
    fun postVerifyEmailCode(value: Boolean) = post(VERIFY_EMAIL_CODE, value)

    private const val UPDATE_EMAIL = "UPDATE_EMAIL"
    var updateEmailWithServer by JoozdLogSharedPreferenceNotNull(UPDATE_EMAIL, false)
    val updateEmailWithServerFlow by PrefsFlow(UPDATE_EMAIL, false)
    fun postUpdateEmailWithServer(value: Boolean) = post(UPDATE_EMAIL, value)

    private const val SEND_LOGIN_LINK = "SEND_LOGIN_LINK"
    var sendLoginLink by JoozdLogSharedPreferenceNotNull(SEND_LOGIN_LINK,false)
    val sendLoginLinkFlow by PrefsFlow(SEND_LOGIN_LINK, false)
    fun postSendLoginLink(value: Boolean) = post(UPDATE_EMAIL, value)

    //create a new user both local and on server

    val createNewUser by JoozdlogSharedPreferenceDelegate(CREATE_NEW_USER,false)
    val feedbackWaiting by JoozdlogSharedPreferenceDelegate(FEEDBACK_WAITING, false)
    val syncDataFiles by JoozdlogSharedPreferenceDelegate(SYNC_DATA_FILES,false)
    val syncFlights by JoozdlogSharedPreferenceDelegate(SYNC_FLIGHTS, false)
}