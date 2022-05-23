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
    fun postUseLoginLink(value: Boolean) = post(USE_LOGIN_LINK, value)


    private const val SEND_BACKUP_EMAIL = "SEND_BACKUP_EMAIL"
    var sendBackupEmail by JoozdLogSharedPreferenceNotNull(SEND_BACKUP_EMAIL,false)
    val sendBackupEmailFlow by PrefsFlow(SEND_BACKUP_EMAIL, false)
    fun postSendBackupEmail(value: Boolean) = post(SEND_BACKUP_EMAIL, value)
/*
    private const val REQUEST_EMAIL_ADDRESS_VERIFICATION_MAIL = "REQUEST_EMAIL_ADDRESS_VERIFICATION_MAIL"
    var requestVerificationEmail by JoozdLogSharedPreferenceNotNull(REQUEST_EMAIL_ADDRESS_VERIFICATION_MAIL, false)
    val requestVerificationEmailFlow by PrefsFlow(REQUEST_EMAIL_ADDRESS_VERIFICATION_MAIL, false)
    fun postRequestVerificationEmail(value: Boolean) = post(REQUEST_EMAIL_ADDRESS_VERIFICATION_MAIL, value)
 */

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
    private const val CREATE_NEW_USER = "CREATE_NEW_USER"
    var createNewUser by JoozdLogSharedPreferenceNotNull(CREATE_NEW_USER,false)
    val createNewUserFlow by PrefsFlow(CREATE_NEW_USER, false)
    fun postCreateNewUser(value: Boolean) = post(CREATE_NEW_USER, value)

    private const val CHANGE_PASSWORD = "CHANGE_PASSWORD"
    var changePassword by JoozdLogSharedPreferenceNotNull(CHANGE_PASSWORD,false)
    val changePasswordFlow by PrefsFlow(CHANGE_PASSWORD, false)
    fun postChangePassword(value: Boolean) = post(CHANGE_PASSWORD, value)

    private const val NOTIFY_BAD_LOGIN_DATA = "NOTIFY_BAD_LOGIN_DATA"
    var notifyBadLoginData by JoozdLogSharedPreferenceNotNull(NOTIFY_BAD_LOGIN_DATA,false)
    val notifyBadLoginDataFlow by PrefsFlow(NOTIFY_BAD_LOGIN_DATA, false)
}