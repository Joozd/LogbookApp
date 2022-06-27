package nl.joozd.logbookapp.core.messages

import nl.joozd.logbookapp.data.sharedPrefs.JoozdLogPreferences
import nl.joozd.logbookapp.data.sharedPrefs.JoozdlogSharedPreferenceDelegate

object MessagesWaiting: JoozdLogPreferences() {
    override val preferencesFileKey = "nl.joozd.logbookapp.MESSAGES_WAITING_KEY"


    private const val NEW_CLOUD_ACCOUNT_CREATED = "NEW_CLOUD_ACCOUNT_CREATED"
    private const val EMAIL_CONFIRMED = "EMAIL_CONFIRMED"
    private const val NO_EMAIL_ENTERED = "NO_EMAIL_ENTERED"
    private const val NO_VERIFICATION_CODE_SAVED_BUG = "NO_VERIFICATION_CODE_SAVED_BUG"
    private const val BAD_VERIFICATION_CODE_CLICKED = "BAD_VERIFICATION_CODE_CLICKED"
    private const val MERGE_WITH_SERVER_PERFORMED = "MERGE_WITH_SERVER_PERFORMED"
    private const val NO_LOGIN_DATA_SAVED = "NO_LOGIN_DATA_SAVED"


    val newCloudAccountCreated by JoozdlogSharedPreferenceDelegate(NEW_CLOUD_ACCOUNT_CREATED, false)
    val emailConfirmed by JoozdlogSharedPreferenceDelegate(EMAIL_CONFIRMED, false)
    val noEmailEntered by JoozdlogSharedPreferenceDelegate(NO_EMAIL_ENTERED, false)
    val noVerificationCodeSavedBug by JoozdlogSharedPreferenceDelegate(NO_VERIFICATION_CODE_SAVED_BUG, false)
    val badVerificationCodeClicked by JoozdlogSharedPreferenceDelegate(BAD_VERIFICATION_CODE_CLICKED, false)
    val mergeWithServerPerformed by JoozdlogSharedPreferenceDelegate(MERGE_WITH_SERVER_PERFORMED, false)
    val noLoginDataSaved by JoozdlogSharedPreferenceDelegate(NO_LOGIN_DATA_SAVED, false)
}