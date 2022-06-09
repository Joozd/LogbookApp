package nl.joozd.logbookapp.data.sharedPrefs

import kotlinx.coroutines.flow.first

object ServerPrefs: JoozdLogPreferences() {
    override val preferencesFileKey = "nl.joozd.logbookapp.SERVER_PREFS_KEY"

    /*
    private const val XXXXXXXXXX = "XXXXXXXXXX"
     */

    private const val EMAIL_ADDRESS = "EMAIL_ADDRESS"
    private const val EMAIL_VERIFIED = "EMAIL_VERIFIED"
    private const val MOST_RECENT_FLIGHT_SYNC_EPOCH_SECOND = "MOST_RECENT_FLIGHT_SYNC_EPOCH_SECOND"

    val emailAddress by JoozdlogSharedPreferenceDelegate(EMAIL_ADDRESS,"")
    /**
     * [emailVerified] is true if email verification code was deemed correct by server
     * set this to false if server gives an INCORRECT_EMAIL_ADDRESS error
     */
    val emailVerified by JoozdlogSharedPreferenceDelegate(EMAIL_VERIFIED,false)
    val mostRecentFlightsSyncEpochSecond by JoozdlogSharedPreferenceDelegate(MOST_RECENT_FLIGHT_SYNC_EPOCH_SECOND, -1L)
}