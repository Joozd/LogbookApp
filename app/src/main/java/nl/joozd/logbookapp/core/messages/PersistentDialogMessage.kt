package nl.joozd.logbookapp.core.messages

import androidx.annotation.CallSuper
import nl.joozd.logbookapp.R
import nl.joozd.logbookapp.data.sharedPrefs.utils.JoozdlogSharedPreferenceDelegate
import nl.joozd.logbookapp.data.sharedPrefs.Prefs
import nl.joozd.logbookapp.ui.utils.JoozdlogActivity
import nl.joozd.logbookapp.ui.utils.toast

@Suppress("ClassName")
sealed class PersistentDialogMessage: PersistentMessage, DialogMessage() {
    override val messageTag = PERSISTANT_DIALOG_MESSAGE_TAG

    @CallSuper
    override fun positiveButtonAction(activity: JoozdlogActivity) {
        messageNeedsToBeDisplayedFlag(false)
        super.positiveButtonAction(activity)
    }

    @CallSuper
    override fun negativeButtonAction(activity: JoozdlogActivity) {
        messageNeedsToBeDisplayedFlag(false)
        super.negativeButtonAction(activity)
    }

    object NO_VERIFICATION_CODE_SAVED_BUG : PersistentDialogMessage() {
        override val messageNeedsToBeDisplayedFlag: JoozdlogSharedPreferenceDelegate.Pref<Boolean> = MessagesWaiting.serverRejectedEmail

        override val titleRes = R.string.email
        override val messageRes = R.string.email_verification_code_not_saved_bug

        override val negativeButtonTextRes: Int = R.string.disable
        override fun negativeButtonAction(activity: JoozdlogActivity) {
            super.negativeButtonAction(activity)
            Prefs.sendBackupEmails(false) } // disable backup mails
    }

    object TEST_DIALOG: PersistentDialogMessage(){
        override val messageNeedsToBeDisplayedFlag: JoozdlogSharedPreferenceDelegate.Pref<Boolean> = MessagesWaiting.testDialogMessage

        override val titleRes = R.string.placeholder
        override val messageRes = R.string.test_message

        override fun positiveButtonAction(activity: JoozdlogActivity) {
            super.positiveButtonAction(activity)
            activity.toast("Test!")
        }
    }

    // DO NOT DISPLAY THIS, just tell activity to close whatever PersistentDialog is open.
    object NO_DIALOG: PersistentDialogMessage(), Message.NoMessage{
        override val messageNeedsToBeDisplayedFlag: JoozdlogSharedPreferenceDelegate.Pref<Boolean> = MessagesWaiting.noDialog
        override val titleRes = -1
        override val messageRes = -1
    }

    companion object{
        private const val PERSISTANT_DIALOG_MESSAGE_TAG = "PERSISTANT_DIALOG_MESSAGE"
    }
}



