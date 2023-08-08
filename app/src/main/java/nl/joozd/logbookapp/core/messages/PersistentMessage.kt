package nl.joozd.logbookapp.core.messages

import nl.joozd.logbookapp.data.sharedPrefs.utils.JoozdlogSharedPreferenceDelegate

sealed interface PersistentMessage: Message {
    /**
     * The flag to track this Dialog by
     */
    val messageNeedsToBeDisplayedFlag: JoozdlogSharedPreferenceDelegate.Pref<Boolean>
}