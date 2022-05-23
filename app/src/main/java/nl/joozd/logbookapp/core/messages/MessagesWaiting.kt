package nl.joozd.logbookapp.core.messages

import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.first
import nl.joozd.logbookapp.data.sharedPrefs.JoozdLogPreferences

object MessagesWaiting: JoozdLogPreferences() {
    override val preferencesFileKey = "nl.joozd.logbookapp.MESSAGES_WAITING_KEY"

    /*
    private const val KEY_GOES_HERE = "KEY_GOES_HERE"
    //can be made non-private if blocking access allowed.
    private var nameOfPreference by JoozdLogSharedPreferenceNotNull(XXXXXXXXXX_KEY_XXXXXXXXXXXXXX, XXXdefaultValueXXX)
    val nameOfPreferenceFlow by PrefsFlow(XXXXXXXXXX_KEY_XXXXXXXXXXXXXX, XXXdefaultValueXXX)
    fun postNameOfPreference(value: Boolean) = post(XXXXXXXXXX_KEY_XXXXXXXXXXXXXX, XXXdefaultValueXXX)
    suspend fun nameOfPreference() = nameOfPreferenceFlow.first()


     */

    private const val NO_EMAIL_ENTERED = "NO_EMAIL_ENTERED"
    private var noEmailEntered by JoozdLogSharedPreferenceNotNull(NO_EMAIL_ENTERED, false)
    val noEmailEnteredFlow by PrefsFlow(NO_EMAIL_ENTERED, false)
    fun postNoEmailEntered(value: Boolean) = post(NO_EMAIL_ENTERED, value)

    private const val NO_VERIFICATION_CODE_SAVED_BUG = "NO_VERIFICATION_CODE_SAVED_BUG"
    //can be made non-private if blocking access allowed.
    private var noVerificationCodeSavedBug by JoozdLogSharedPreferenceNotNull(NO_VERIFICATION_CODE_SAVED_BUG, false)
    val noVerificationCodeSavedBugFlow by PrefsFlow(NO_VERIFICATION_CODE_SAVED_BUG, false)
    fun postBadVerificationCodeSavedBug(value: Boolean) = post(NO_VERIFICATION_CODE_SAVED_BUG, false)
    suspend fun noVerificationCodeSavedBug() = noVerificationCodeSavedBugFlow.first()
}