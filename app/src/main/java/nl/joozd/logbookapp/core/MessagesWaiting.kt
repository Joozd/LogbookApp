package nl.joozd.logbookapp.core

import nl.joozd.logbookapp.data.sharedPrefs.JoozdLogPreferences

object MessagesWaiting: JoozdLogPreferences() {
    override val preferencesFileKey = "nl.joozd.logbookapp.MESSAGES_WAITING_KEY"
}