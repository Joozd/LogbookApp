package nl.joozd.logbookapp.core

import nl.joozd.logbookapp.data.sharedPrefs.JoozdLogPreferences

/**
 * Every task represents something that needs to be done.
 * TaskDispatcher will observe these and dispatch those tasks to whomever is supposed to handle them.
 */
object TaskFlags: JoozdLogPreferences() {
    override val preferencesFileKey = "nl.joozd.logbookapp.TASK_FLAGS_FILE_KEY"

    /*
    private const val XXXXXXXXXX = "XXXXXXXXXX"
     */
    private const val USE_LOGIN_LINK = "USE_LOGIN_LINK"
    var useLoginLink by JoozdLogSharedPreferenceNotNull(USE_LOGIN_LINK,false)
    val useLoginLinkFlow by PrefsFlow(USE_LOGIN_LINK, false)
    fun pushUseLoginLink(value: Boolean) = post(USE_LOGIN_LINK, value)


    private const val SEND_BACKUP_EMAIL = "SEND_BACKUP_EMAIL"
    var sendBackupEmail by JoozdLogSharedPreferenceNotNull(SEND_BACKUP_EMAIL,false)
    val sendBackupEmailFlow by PrefsFlow(SEND_BACKUP_EMAIL, false)

    private const val VERIFY_EMAIL = "VERIFY_EMAIL"
    var verifyEmail by JoozdLogSharedPreferenceNotNull(VERIFY_EMAIL, false)
    val verifyEmailFlow by PrefsFlow(VERIFY_EMAIL, false)

    private const val UPDATE_EMAIL = "UPDATE_EMAIL"
    var updateEmailWithServer by JoozdLogSharedPreferenceNotNull(UPDATE_EMAIL, false)
    val updateEmailWithServerFlow by PrefsFlow(UPDATE_EMAIL, false)
    fun pushUpdateEmailWithServer(value: Boolean) = post(UPDATE_EMAIL, value)

    private const val SEND_LOGIN_LINK = "SEND_LOGIN_LINK"
    var sendLoginLink by JoozdLogSharedPreferenceNotNull(SEND_LOGIN_LINK,false)
    val sendLoginLinkFlow by PrefsFlow(SEND_LOGIN_LINK, false)

    //create a new user both local and on server
    private const val CREATE_NEW_USER = "CREATE_NEW_USER"
    var createNewUser by JoozdLogSharedPreferenceNotNull(CREATE_NEW_USER,false)
    val createNewUserFlow by PrefsFlow(CREATE_NEW_USER, false)

    private const val NOTIFY_BAD_LOGIN_DATA = "NOTIFY_BAD_LOGIN_DATA"
    var notifyBadLoginData by JoozdLogSharedPreferenceNotNull(NOTIFY_BAD_LOGIN_DATA,false)
    val notifyBadLoginDataFlow by PrefsFlow(NOTIFY_BAD_LOGIN_DATA, false)
}