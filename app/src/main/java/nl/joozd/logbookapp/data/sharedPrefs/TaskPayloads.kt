package nl.joozd.logbookapp.data.sharedPrefs

import nl.joozd.logbookapp.data.sharedPrefs.utils.JoozdLogPreferences
import nl.joozd.logbookapp.data.sharedPrefs.utils.JoozdlogSharedPreferenceDelegate

/**
 * Payloads needed for tasks.
 * Not really preferences but an easy way to keep track of some flags
 */
object TaskPayloads: JoozdLogPreferences() {
    override val preferencesFileKey = "TASK_PAYLOADS_KEY"

    private const val EMAIL_CONF_STRING_WAITING = "EMAIL_CONF_STRING_WAITING"
    private const val FEEDBACK_WAITING = "FEEDBACK_WAITING"
    private const val FEEDBACK_CONTACT_INFO_WAITING = "FEEDBACK_CONTACT_INFO_WAITING"

    val emailConfirmationStringWaiting by JoozdlogSharedPreferenceDelegate(EMAIL_CONF_STRING_WAITING,"")
    val feedbackWaiting by JoozdlogSharedPreferenceDelegate(FEEDBACK_WAITING, "")
    val feedbackContactInfo by JoozdlogSharedPreferenceDelegate(FEEDBACK_CONTACT_INFO_WAITING, "")
}