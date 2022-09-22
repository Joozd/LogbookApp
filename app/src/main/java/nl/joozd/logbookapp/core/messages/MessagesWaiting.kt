package nl.joozd.logbookapp.core.messages

import nl.joozd.logbookapp.data.sharedPrefs.JoozdLogPreferences
import nl.joozd.logbookapp.data.sharedPrefs.JoozdlogSharedPreferenceDelegate

object MessagesWaiting: JoozdLogPreferences() {
    override val preferencesFileKey = "nl.joozd.logbookapp.MESSAGES_WAITING_KEY"

    private const val EMAIL_CONFIRMED = "EMAIL_CONFIRMED"
    private const val NO_EMAIL_ENTERED = "NO_EMAIL_ENTERED"
    private const val SERVER_REJECTED_EMAIL = "SERVER_REJECTED_EMAIL"
    private const val NO_VERIFICATION_CODE_SAVED_BUG = "NO_VERIFICATION_CODE_SAVED_BUG"
    private const val BAD_VERIFICATION_CODE_CLICKED = "BAD_VERIFICATION_CODE_CLICKED"


    val emailConfirmed by JoozdlogSharedPreferenceDelegate(EMAIL_CONFIRMED, false)
    val noEmailEntered by JoozdlogSharedPreferenceDelegate(NO_EMAIL_ENTERED, false)
    val serverRejectedEmail by JoozdlogSharedPreferenceDelegate(SERVER_REJECTED_EMAIL, false)
    val noVerificationCodeSavedBug by JoozdlogSharedPreferenceDelegate(NO_VERIFICATION_CODE_SAVED_BUG, false)
    val badVerificationCodeClicked by JoozdlogSharedPreferenceDelegate(BAD_VERIFICATION_CODE_CLICKED, false)
}