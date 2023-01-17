package nl.joozd.logbookapp.core.messages

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.fragment.app.Fragment
import androidx.fragment.app.commit
import androidx.lifecycle.lifecycleScope
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import nl.joozd.logbookapp.R
import nl.joozd.logbookapp.core.BackupCenter
import nl.joozd.logbookapp.core.Constants
import nl.joozd.logbookapp.core.EmailCenter
import nl.joozd.logbookapp.data.sharedPrefs.BackupPrefs
import nl.joozd.logbookapp.data.sharedPrefs.JoozdlogSharedPreferenceDelegate
import nl.joozd.logbookapp.databinding.FragmentGenericNotificationBinding
import nl.joozd.logbookapp.extensions.atStartOfDay
import nl.joozd.logbookapp.extensions.makeCsvSharingIntent
import nl.joozd.logbookapp.extensions.removeByTagAnimated
import nl.joozd.logbookapp.extensions.showFragment
import nl.joozd.logbookapp.ui.utils.JoozdlogActivity
import nl.joozd.logbookapp.ui.utils.toast
import java.time.Instant
import java.time.ZoneOffset

@Suppress("ClassName")
sealed class MessageBarMessage: PersistantMessage {
    protected abstract val messageRes: Int
    protected open val messageFormatArgs: Array<Any> = emptyArray()

    protected open val positiveButtonTextRes: Int = android.R.string.ok
    protected open val positiveButtonAction: JoozdlogActivity.() -> Unit = { /* do nothing */ }

    protected open val negativeButtonTextRes: Int? = null // can be null, negative button not required. If not null, negativeButtonAction must not be null
    protected open val negativeButtonAction: (JoozdlogActivity.() -> Unit)? = {  /*do nothing */ } // if [negativeButtonText] is not null and this is null, it will only mark the dialog as read.

    /*
            if (it != null && supportFragmentManager.findFragmentById(it.id) == null) {
        supportFragmentManager.commit {
            setCustomAnimations(R.anim.slide_in_from_top, 0)
            add(R.id.message_bar_target, it, MESSAGE_BAR_FRAGMENT_TAG)
        }
    } else {
        supportFragmentManager.removeByTagAnimated(MESSAGE_BAR_FRAGMENT_TAG, R.anim.slide_out_to_top)
    }
 */

    fun displayMessage(activity: JoozdlogActivity, target: View) {
        if (this is NO_MESSAGE){
            activity.clearMessage()
            return
        }
        val messageBar = object : Fragment() {
            override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View =
                FragmentGenericNotificationBinding.bind(inflater.inflate(R.layout.fragment_generic_notification, container, false)).apply {
                    genericNotificationMessage.text = if (messageFormatArgs.isEmpty()) getString(messageRes) else getString(messageRes, *messageFormatArgs)
                    positiveButton.setText(positiveButtonTextRes)
                    positiveButton.setOnClickListener {
                        activity.positiveButtonAction()
                        activity.clearMessage()
                    }
                    if (negativeButtonTextRes != null) {
                        negativeButton.setText(
                            negativeButtonTextRes ?: android.R.string.ok
                        ) // this elvis operator is probably never used but better than forced not null
                        negativeButton.setOnClickListener {
                            negativeButtonAction?.let { activity.it() }
                            activity.clearMessage()
                        }
                    } else {
                        negativeButton.visibility = View.GONE
                    }
                }.root
        }
        if(activity.supportFragmentManager.findFragmentByTag(MESSAGE_BAR_FRAGMENT_TAG) == null)
            activity.showFragment(messageBar, target, MESSAGE_BAR_FRAGMENT_TAG, addToBackStack = false)
        else{
            with(activity.supportFragmentManager) {
                commit{
                    replace(target.id, messageBar)
                }
            }

        }
    }

    private fun JoozdlogActivity.clearMessage() {
        lifecycleScope.launch {
            supportFragmentManager.removeByTagAnimated(MESSAGE_BAR_FRAGMENT_TAG, R.anim.slide_out_to_top)
            delay(600) // delay to make sure user sees dialog is properly closed before next dialog opens if one is there
            messageNeedsToBeDisplayedFlag(false)
        }
    }

    object BACKUP_NEEDED : MessageBarMessage() {
        override val messageNeedsToBeDisplayedFlag: JoozdlogSharedPreferenceDelegate.Pref<Boolean> = MessagesWaiting.backupNeeded

        private val mostRecentBackup get() = BackupPrefs.mostRecentBackup.valueBlocking
        private val hasNeverBackedUp get() = mostRecentBackup == 0L
        override val messageRes get() = if (hasNeverBackedUp) R.string.you_have_never_backed_up else R.string.you_have_not_backed_up_n_days
        override val messageFormatArgs: Array<Any> = if (hasNeverBackedUp) emptyArray() else arrayOf((Instant.now().epochSecond - mostRecentBackup) / Constants.ONE_DAY_IN_SECONDS)

        override val positiveButtonAction: (JoozdlogActivity.() -> Unit) = {
            this.lifecycleScope.launch {
                makeCsvSharingIntent(BackupCenter.makeBackupUri())
            }
        }
        override val negativeButtonTextRes: Int = R.string.ignore_for_one_day
        override val negativeButtonAction: (JoozdlogActivity.() -> Unit) = {  BackupPrefs.backupIgnoredUntil(Instant.now().atStartOfDay(currentLocalZoneOffset()).epochSecond + Constants.ONE_DAY_IN_SECONDS) }

        private fun currentLocalZoneOffset(): ZoneOffset = ZoneOffset.systemDefault().rules.getOffset(Instant.now())
    }

    object TEST_MESSAGE_BAR: MessageBarMessage(){
        override val messageNeedsToBeDisplayedFlag: JoozdlogSharedPreferenceDelegate.Pref<Boolean> = MessagesWaiting.testMessageBarMessage
        override val messageRes: Int = R.string.test_message
        override val positiveButtonAction: (JoozdlogActivity.()-> Unit) = { toast("TEST 123 TEST") }
    }

    object EMAIL_CONFIRMED : MessageBarMessage() {
        override val messageNeedsToBeDisplayedFlag: JoozdlogSharedPreferenceDelegate.Pref<Boolean> = MessagesWaiting.serverRejectedEmail

        override val messageRes = R.string.email_verified
    }

    object UNKNOWN_OR_UNVERIFIED_EMAIL : MessageBarMessage() {
        override val messageNeedsToBeDisplayedFlag: JoozdlogSharedPreferenceDelegate.Pref<Boolean> = MessagesWaiting.serverRejectedEmail

        override val messageRes = R.string.server_reported_email_not_verified
        override val positiveButtonAction: (JoozdlogActivity.()-> Unit) = {  EmailCenter().invalidateEmail() }
    }

    object INVALID_EMAIL_ADDRESS : MessageBarMessage() {
        override val messageNeedsToBeDisplayedFlag: JoozdlogSharedPreferenceDelegate.Pref<Boolean> = MessagesWaiting.invalidEmailAddressStored

        override val messageRes = R.string.server_not_an_email_address_please_enter_again
        override val positiveButtonAction: (JoozdlogActivity.()-> Unit) = {  EmailCenter().invalidateEmail() }
    }

    object NO_EMAIL_ENTERED_FOR_AUTO_BACKUP : MessageBarMessage() {
        override val messageNeedsToBeDisplayedFlag: JoozdlogSharedPreferenceDelegate.Pref<Boolean> = MessagesWaiting.noEmailEntered

        override val messageRes = R.string.no_email_address_entered_but_wanted_for_backup

        override val positiveButtonTextRes: Int = R.string.disable
        override val positiveButtonAction: (JoozdlogActivity.()-> Unit) = {  EmailCenter().invalidateEmail() }
    }

    object BAD_VERIFICATION_CODE_CLICKED : MessageBarMessage() {
        override val messageNeedsToBeDisplayedFlag: JoozdlogSharedPreferenceDelegate.Pref<Boolean> = MessagesWaiting.serverRejectedEmail

        override val messageRes = R.string.email_verification_invalid_data

        override val positiveButtonTextRes: Int = R.string.yes
        override val positiveButtonAction: JoozdlogActivity.() -> Unit = {
            EmailCenter().requestEmailVerificationMail()
            toast(R.string.email_verification_scheduled_message)
        }
        override val negativeButtonTextRes: Int = R.string.no
        override val negativeButtonAction: (JoozdlogActivity.()-> Unit) = { EmailCenter().invalidateEmail() } // disable backup mails
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