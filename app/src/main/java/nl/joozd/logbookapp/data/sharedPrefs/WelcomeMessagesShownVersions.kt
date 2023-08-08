package nl.joozd.logbookapp.data.sharedPrefs

import nl.joozd.logbookapp.data.sharedPrefs.utils.JoozdLogPreferences
import nl.joozd.logbookapp.data.sharedPrefs.utils.JoozdlogSharedPreferenceDelegate

/**
 * Keep track of what "First Time Messages" have been shown so users can be informed about what changes there are in a certain environment
 */
object WelcomeMessagesShownVersions: JoozdLogPreferences() {
    override val preferencesFileKey = "nl.joozd.logbookapp.FIRST_TIME_MESSAGES_SHOWN"

    private const val NAMES2_DIALOG = "NAMES2_DIALOG"
    val names2Dialog by JoozdlogSharedPreferenceDelegate(NAMES2_DIALOG, 0)

    private const val EDIT_FLIGHT_FRAGMENT = "EDIT_FLIGHT_FRAGMENT"
    val editFlightFragment by JoozdlogSharedPreferenceDelegate(EDIT_FLIGHT_FRAGMENT, 0)
}