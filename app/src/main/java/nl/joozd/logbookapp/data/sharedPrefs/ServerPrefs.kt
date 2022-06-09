package nl.joozd.logbookapp.data.sharedPrefs

import kotlinx.coroutines.flow.first

object ServerPrefs: JoozdLogPreferences() {
    override val preferencesFileKey = "nl.joozd.logbookapp.SERVER_PREFS_KEY"

    /*
    private const val XXXXXXXXXX = "XXXXXXXXXX"
     */

    private const val MOST_RECENT_FLIGHT_SYNC_EPOCH_SECOND = "MOST_RECENT_FLIGHT_SYNC_EPOCH_SECOND"

    private const val EMAIL_ADDRESS = "EMAIL_ADDRESS"
    var emailAddress by JoozdLogSharedPreferenceNotNull(EMAIL_ADDRESS,"")
    val emailAddressFlow by PrefsFlow(EMAIL_ADDRESS, "")
    suspend fun emailAddress() = emailAddressFlow.first()
    fun postEmailAddress(value: String) = post(EMAIL_ADDRESS, value)

    /**
     * [emailVerified] is true if email verification code was deemed correct by server
     * set this to false if server gives an INCORRECT_EMAIL_ADDRESS error
     */
    private const val EMAIL_VERIFIED = "EMAIL_VERIFIED"
    var emailVerified: Boolean by JoozdLogSharedPreferenceNotNull(EMAIL_VERIFIED,false)
    val emailVerifiedFlow by PrefsFlow(EMAIL_VERIFIED, false)
    fun postEmailVerified(value: Boolean) = post(EMAIL_VERIFIED, value)

    val mostRecentFlightsSyncEpochSecond by JoozdlogSharedPreferenceDelegate(MOST_RECENT_FLIGHT_SYNC_EPOCH_SECOND, -1L)
}