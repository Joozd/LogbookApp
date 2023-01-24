package nl.joozd.logbookapp.core.messages

import nl.joozd.logbookapp.R
import nl.joozd.logbookapp.data.sharedPrefs.Prefs
import nl.joozd.logbookapp.ui.utils.JoozdlogActivity

/**
 * An instant message can get parameters in its constructor, but can only be displayed right away.
 * Multiple instant messages can be displayed on top of each other, each needs their own tag for tracking. They cannot be retracted.
 * If an InstantMessage is dispatched without an activity that supports messages, it will be ignored.
 *
 * This is a sealed class because I only want predefined messages to be sent. Define them here!
 */
abstract class InstantMessage: DialogMessage() {
    class CalendarConflict(private val postponeUntilAfter: Long, override val messageRes: Int): InstantMessage(){
        override val titleRes = R.string.calendar_sync_conflict
        override val messageTag = "CALENDAR_CONFLICT_INSTANT_MESSAGE"

        override fun positiveButtonAction(activity: JoozdlogActivity) {
            super.positiveButtonAction(activity)
            Prefs.calendarDisabledUntil(postponeUntilAfter + 1)
        }
    }
/*
    class TestInstantMessage(override val messageRes: Int): InstantMessage(){
        override val titleRes = R.string.test_message
        override val messageTag = "TEST_INSTANT_MESSAGE"

        override fun positiveButtonAction(activity: JoozdlogActivity) {
            super.positiveButtonAction(activity)
            activity.toast(messageTag)
        }
    }
*/
}