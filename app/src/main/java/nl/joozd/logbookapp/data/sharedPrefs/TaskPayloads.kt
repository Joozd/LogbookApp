package nl.joozd.logbookapp.data.sharedPrefs

/**
 * Payloads needed for tasks
 */
object TaskPayloads: JoozdLogPreferences() {
    override val preferencesFileKey = "TASK_PAYLOADS_KEY"

    private const val EMAIL_CONF_STRING_WAITING = "EMAIL_CONF_STRING_WAITING"
    private const val FEEDBACK_WAITING = "FEEDBACK_WAITING"
    private const val FEEDBACK_CONTACT_INFO_WAITING = "FEEDBACK_CONTACT_INFO_WAITING"

    val emailConfirmationStringWaiting by JoozdlogSharedPreferenceDelegate(EMAIL_CONF_STRING_WAITING,"")
    val feedbackWaiting by JoozdlogSharedPreferenceDelegate(FEEDBACK_WAITING, "")
    val feedbackContactInfoWaiting by JoozdlogSharedPreferenceDelegate(FEEDBACK_CONTACT_INFO_WAITING, "")
}