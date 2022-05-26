package nl.joozd.logbookapp.core.messages


import kotlinx.coroutines.flow.first
import nl.joozd.logbookapp.data.sharedPrefs.JoozdLogPreferences

//TODO none of these are handled yet
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

    private const val EMAIL_CONFIRMED = "EMAIL_CONFIRMED"
    //can be made non-private if blocking access allowed.
    private var emailConfirmed by JoozdLogSharedPreferenceNotNull(EMAIL_CONFIRMED, false)
    val emailConfirmedFlow by PrefsFlow(EMAIL_CONFIRMED, false)
    fun postEmailConfirmed(value: Boolean) = post(EMAIL_CONFIRMED, value)
    // suspend fun nameOfPreference() = nameOfPreferenceFlow.first()


    private const val NO_EMAIL_ENTERED = "NO_EMAIL_ENTERED"
    private var noEmailEntered by JoozdLogSharedPreferenceNotNull(NO_EMAIL_ENTERED, false)
    val noEmailEnteredFlow by PrefsFlow(NO_EMAIL_ENTERED, false)
    fun postNoEmailEntered(value: Boolean) = post(NO_EMAIL_ENTERED, value)

    private const val NO_VERIFICATION_CODE_SAVED_BUG = "NO_VERIFICATION_CODE_SAVED_BUG"
    private var noVerificationCodeSavedBug by JoozdLogSharedPreferenceNotNull(NO_VERIFICATION_CODE_SAVED_BUG, false)
    val noVerificationCodeSavedBugFlow by PrefsFlow(NO_VERIFICATION_CODE_SAVED_BUG, false)
    fun postBadVerificationCodeSavedBug(value: Boolean) = post(NO_VERIFICATION_CODE_SAVED_BUG, value)
    // suspend fun noVerificationCodeSavedBug() = noVerificationCodeSavedBugFlow.first()





    private const val BAD_VERIFICATION_CODE_CLICKED = "BAD_VERIFICATION_CODE_CLICKED"
    //can be made non-private if blocking access allowed.
    private var badVerificationCodeClicked by JoozdLogSharedPreferenceNotNull(BAD_VERIFICATION_CODE_CLICKED, false)
    val badVerificationCodeClickedFlow by PrefsFlow(BAD_VERIFICATION_CODE_CLICKED, false)
    fun postBadVerificationCodeClicked(value: Boolean) = post(BAD_VERIFICATION_CODE_CLICKED, value)
    // suspend fun badVerificationCodeClicked() = badVerificationCodeClickedFlow.first()
}