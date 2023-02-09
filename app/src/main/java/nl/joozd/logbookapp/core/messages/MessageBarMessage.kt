package nl.joozd.logbookapp.core.messages


import android.view.View
import androidx.lifecycle.lifecycleScope
import kotlinx.coroutines.launch
import nl.joozd.logbookapp.R
import nl.joozd.logbookapp.core.BackupCenter
import nl.joozd.logbookapp.core.Constants
import nl.joozd.logbookapp.core.EmailCenter
import nl.joozd.logbookapp.data.sharedPrefs.BackupPrefs
import nl.joozd.logbookapp.data.sharedPrefs.JoozdlogSharedPreferenceDelegate
import nl.joozd.logbookapp.extensions.atStartOfDay
import nl.joozd.logbookapp.extensions.makeCsvSharingIntent
import nl.joozd.logbookapp.extensions.removeByTagAnimated
import nl.joozd.logbookapp.extensions.showFragment
import nl.joozd.logbookapp.ui.utils.JoozdlogActivity
import nl.joozd.logbookapp.ui.utils.toast
import java.time.Instant
import java.time.ZoneOffset

@Suppress("ClassName")
abstract class MessageBarMessage: PersistentMessage {
    abstract val messageRes: Int
    open val messageFormatArgs: Array<Any> = emptyArray()

    open val positiveButtonTextRes: Int = android.R.string.ok
    open fun positiveButtonAction(activity: JoozdlogActivity) { /* do nothing */ }

    open val negativeButtonTextRes: Int? = null // can be null, negative button not required. If not null, negativeButtonAction must not be null
    open fun negativeButtonAction(activity: JoozdlogActivity) {  /*do nothing */ } // if [negativeButtonText] is not null and this is null, it will only mark the dialog as read.

    fun displayMessage(activity: JoozdlogActivity, target: View) {
        if (this is NO_MESSAGE){
            clearMessage(activity)
            return
        }

        val messageBar = MessageBarMessageFragment(this)
        if(activity.supportFragmentManager.findFragmentByTag(MESSAGE_BAR_FRAGMENT_TAG) == null)
            activity.showFragment(messageBar, target, MESSAGE_BAR_FRAGMENT_TAG, addToBackStack = false)
        // don't replace, just accept there is one already there. If it gets closed, a new one should be shown if another one is still waiting.

        // !!! NOTE there might be an edge case here where the flow won't emit a value if it is not distinct/changed
    }

    fun clearMessage(activity: JoozdlogActivity) {
        with(activity) {
            lifecycleScope.launch {
                supportFragmentManager.removeByTagAnimated(MESSAGE_BAR_FRAGMENT_TAG, R.anim.slide_out_to_top){
                    messageNeedsToBeDisplayedFlag(false)
                }
            }
        }
    }

    object BACKUP_NEEDED : MessageBarMessage() {
        override val messageNeedsToBeDisplayedFlag: JoozdlogSharedPreferenceDelegate.Pref<Boolean> = MessagesWaiting.backupNeeded

        private val mostRecentBackup get() = BackupPrefs.mostRecentBackup.valueBlocking
        private val hasNeverBackedUp get() = mostRecentBackup == 0L
        override val messageRes get() = if (hasNeverBackedUp) R.string.you_have_never_backed_up else R.string.you_have_not_backed_up_n_days
        override val messageFormatArgs: Array<Any> = if (hasNeverBackedUp) emptyArray() else arrayOf((Instant.now().epochSecond - mostRecentBackup) / Constants.ONE_DAY_IN_SECONDS)

        override val positiveButtonTextRes: Int = R.string.backup_now

        override fun positiveButtonAction(activity: JoozdlogActivity) {
            with (activity) {
                this.lifecycleScope.launch {
                    makeCsvSharingIntent(BackupCenter.makeBackupUri())
                }
            }
        }

        override val negativeButtonTextRes: Int = R.string.ignore_for_one_day
        override fun negativeButtonAction(activity: JoozdlogActivity) {
            BackupPrefs.backupIgnoredUntil(Instant.now().atStartOfDay(currentLocalZoneOffset()).epochSecond + Constants.ONE_DAY_IN_SECONDS)
        }

        private fun currentLocalZoneOffset(): ZoneOffset = ZoneOffset.systemDefault().rules.getOffset(Instant.now())
    }

    object EMAIL_CONFIRMATION_SCHEDULED: MessageBarMessage(){
        override val messageNeedsToBeDisplayedFlag: JoozdlogSharedPreferenceDelegate.Pref<Boolean> = MessagesWaiting.emailConfirmationScheduled
        override val messageRes: Int = R.string.email_verification_scheduled_message
    }

    object EMAIL_CONFIRMED : MessageBarMessage() {
        override val messageNeedsToBeDisplayedFlag: JoozdlogSharedPreferenceDelegate.Pref<Boolean> = MessagesWaiting.emailConfirmed
        override val messageRes = R.string.email_verified
    }

    object UNKNOWN_OR_UNVERIFIED_EMAIL : MessageBarMessage() {
        override val messageNeedsToBeDisplayedFlag: JoozdlogSharedPreferenceDelegate.Pref<Boolean> = MessagesWaiting.serverRejectedEmail

        override val messageRes = R.string.server_reported_email_not_verified
        override fun positiveButtonAction(activity: JoozdlogActivity) {  EmailCenter().invalidateEmail() }
    }

    object INVALID_EMAIL_ADDRESS : MessageBarMessage() {
        override val messageNeedsToBeDisplayedFlag: JoozdlogSharedPreferenceDelegate.Pref<Boolean> = MessagesWaiting.invalidEmailAddressStored

        override val messageRes = R.string.server_not_an_email_address_please_enter_again
        override fun positiveButtonAction(activity: JoozdlogActivity) {  EmailCenter().invalidateEmail() }
    }

    object NO_EMAIL_ENTERED_FOR_AUTO_BACKUP : MessageBarMessage() {
        override val messageNeedsToBeDisplayedFlag: JoozdlogSharedPreferenceDelegate.Pref<Boolean> = MessagesWaiting.noEmailEntered

        override val messageRes = R.string.no_email_address_entered_but_wanted_for_backup

        override val positiveButtonTextRes: Int = R.string.disable
        override fun positiveButtonAction(activity: JoozdlogActivity) {  EmailCenter().invalidateEmail() }
    }

    object BAD_VERIFICATION_CODE_CLICKED : MessageBarMessage() {
        override val messageNeedsToBeDisplayedFlag: JoozdlogSharedPreferenceDelegate.Pref<Boolean> = MessagesWaiting.serverRejectedEmail

        override val messageRes = R.string.email_verification_invalid_data

        override val positiveButtonTextRes: Int = R.string.yes
        override fun positiveButtonAction(activity: JoozdlogActivity) {
            EmailCenter().requestEmailVerificationMail()
            toast(R.string.email_verification_scheduled_message)
        }
        override val negativeButtonTextRes: Int = R.string.no
        override fun negativeButtonAction(activity: JoozdlogActivity) { EmailCenter().invalidateEmail() } // disable backup mails
    }

    object TEST_MESSAGE_BAR: MessageBarMessage(){
        override val messageNeedsToBeDisplayedFlag: JoozdlogSharedPreferenceDelegate.Pref<Boolean> = MessagesWaiting.testMessageBarMessage
        override val messageRes: Int = R.string.test_message
        override fun positiveButtonAction(activity: JoozdlogActivity) { toast("TEST 123 TEST") }
    }

    // DO NOT DISPLAY THIS, just tell activity to close whatever PersistentDialog is open.
    object NO_MESSAGE: MessageBarMessage(){
        override val messageNeedsToBeDisplayedFlag: JoozdlogSharedPreferenceDelegate.Pref<Boolean> = MessagesWaiting.noMessageBar
        override val messageRes = -1
    }

    companion object{
        private const val MESSAGE_BAR_FRAGMENT_TAG = "MESSAGE_BAR_FRAGMENT_TAG"
    }
}