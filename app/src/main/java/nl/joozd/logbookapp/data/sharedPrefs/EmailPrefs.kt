package nl.joozd.logbookapp.data.sharedPrefs

import nl.joozd.joozdlogcommon.EmailData

object EmailPrefs: JoozdLogPreferences() {
    override val preferencesFileKey = "nl.joozd.logbookapp.SERVER_PREFS_KEY"

    /*
    private const val XXXXXXXXXX = "XXXXXXXXXX"
     */

    private const val EMAIL_ID = "EMAIL_ID"
    private const val EMAIL_ADDRESS = "EMAIL_ADDRESS"
    private const val EMAIL_VERIFIED = "EMAIL_VERIFIED"

    val emailID by JoozdlogSharedPreferenceDelegate(EMAIL_ID, EmailData.EMAIL_ID_NOT_SET)

    val emailAddress by JoozdlogSharedPreferenceDelegate(EMAIL_ADDRESS, "")
    /**
     * [emailVerified] is true if email verification code was deemed correct by server
     * set this to false if server gives an INCORRECT_EMAIL_ADDRESS error
     */
    val emailVerified by JoozdlogSharedPreferenceDelegate(EMAIL_VERIFIED, false)
}