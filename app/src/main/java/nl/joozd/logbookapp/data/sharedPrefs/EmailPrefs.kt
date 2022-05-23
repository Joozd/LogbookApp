package nl.joozd.logbookapp.data.sharedPrefs

import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.first

object EmailPrefs: JoozdLogPreferences() {
    override val preferencesFileKey = "nl.joozd.logbookapp.EMAIL_PREFS_KEY"

    /*
    private const val XXXXXXXXXX = "XXXXXXXXXX"
     */

    private const val EMAIL_ADDRESS = "EMAIL_ADDRESS"
    var emailAddress by JoozdLogSharedPreferenceNotNull(EMAIL_ADDRESS,"")
    val emailAddressFlow by PrefsFlow(EMAIL_ADDRESS, "")
    suspend fun emailAddress() = emailAddressFlow.first()
    fun postEmailAddress(value: String) = post(EMAIL_ADDRESS, value)

    /**
     * Email confirmation string waiting for a network connection. Handled by [nl.joozd.logbookapp.workmanager.ConfirmEmailWorker]
     */
    private const val EMAIL_CONF_STRING_WAITING = "EMAIL_CONF_STRING_WAITING"
    var emailConfirmationStringWaiting by JoozdLogSharedPreferenceNotNull(EMAIL_CONF_STRING_WAITING,"")
    val emailConfirmationStringWaitingFlow by PrefsFlow(EMAIL_CONF_STRING_WAITING, "")
    fun postEmailConfirmationStringWaiting(value: String) = post(EMAIL_CONF_STRING_WAITING, value)
    suspend fun emailConfirmationStringWaiting() = emailConfirmationStringWaitingFlow.first()


    /**
     * [emailVerified] is true if email verification code was deemed correct by server
     * set this to false if server gives an INCORRECT_EMAIL_ADDRESS error
     */
    private const val EMAIL_VERIFIED = "EMAIL_VERIFIED"
    var emailVerified: Boolean by JoozdLogSharedPreferenceNotNull(EMAIL_VERIFIED,false)
    val emailVerifiedFlow by PrefsFlow(EMAIL_VERIFIED, false)
    fun postEmailVerified(value: Boolean) = post(EMAIL_VERIFIED, value)

    val backupEmailEnabledFlow = combine(emailAddressFlow, emailVerifiedFlow, Prefs.backupFromCloudFlow){
        addr, verified, enabled ->
        addr.isNotBlank() && verified && enabled
    }

}