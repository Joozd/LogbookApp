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
import nl.joozd.logbookapp.data.sharedPrefs.JoozdlogSharedPreferenceDelegate
import nl.joozd.logbookapp.data.sharedPrefs.Prefs
import nl.joozd.logbookapp.databinding.DialogPersistantDialogBinding
import nl.joozd.logbookapp.extensions.showFragment
import nl.joozd.logbookapp.ui.utils.JoozdlogActivity

@Suppress("ClassName")
sealed class PersistentDialogMessage: PersistantMessage {
    /**
     * The flag to track this Dialog by
     */

    protected abstract val titleRes: Int
    protected open val titleFormatArgs: Array<Any> = emptyArray()

    protected abstract val messageRes: Int
    protected open val messageFormatArgs: Array<Any> = emptyArray()

    /**
     * Positive button (right). Defaults to "OK" and  { /*do nothing*/ }
     */
    protected open val positiveButtonTextRes: Int = android.R.string.ok
    protected open val positiveButtonAction: () -> Unit = { /*do nothing*/ }

    /**
     * Negative button (left). Defaults to null which means not there.
     */
    protected open val negativeButtonTextRes: Int? = null // can be null, negative button not required. If not null, negativeButtonAction must not be null
    protected open val negativeButtonAction: (() -> Unit)? = null // if [negativeButtonText] is not null and this is null, it will only mark the dialog as read.

    fun displayDialog(activity: JoozdlogActivity) {
        //If a dialog is already opened when this gets executed, destroy the old dialog.
        activity.supportFragmentManager.findFragmentByTag(PERSISTANT_DIALOG_MESSAGE_TAG)?.let {
            activity.supportFragmentManager.commit { remove(it) }
        }

        if(this != NO_DIALOG) { // only show dialog if no dialog is being shown already, and if this is not NO_DIALOG
            activity.showFragment(object : Fragment() {
                override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View =
                    DialogPersistantDialogBinding.bind(inflater.inflate(R.layout.dialog_persistant_dialog, container, false)).apply {
                        persistantDialogTitleTextView.text = if (titleFormatArgs.isEmpty()) getString(titleRes) else getString(titleRes, *titleFormatArgs)
                        persistantDialogMessage.text = if (messageFormatArgs.isEmpty()) getString(messageRes) else getString(messageRes, *messageFormatArgs)
                        persistantDialogPositiveButton.setText(positiveButtonTextRes)
                        persistantDialogPositiveButton.setOnClickListener {
                            positiveButtonAction()
                            activity.lifecycleScope.launch {
                                delay(600) // delay to make sure user sees dialog is properly closed before next dialog opens if one is there
                                messageNeedsToBeDisplayedFlag(false)
                            }
                        }
                        if (negativeButtonTextRes != null) {
                            persistantDialogNegativeButton.setText(
                                negativeButtonTextRes ?: android.R.string.ok
                            ) // this elvis operator is probably never used but better than forced not null
                            persistantDialogNegativeButton.setOnClickListener {
                                negativeButtonAction?.let { it() }
                                activity.lifecycleScope.launch {
                                    delay(600) // delay to make sure user sees dialog is properly closed before next dialog opens if one is there
                                    messageNeedsToBeDisplayedFlag(false)
                                }
                            }
                        } else {
                            persistantDialogNegativeButton.visibility = View.GONE
                        }
                    }.root
            }, tag = PERSISTANT_DIALOG_MESSAGE_TAG, addToBackStack = true)
        }
    }



    object NO_VERIFICATION_CODE_SAVED_BUG : PersistentDialogMessage() {
        override val messageNeedsToBeDisplayedFlag: JoozdlogSharedPreferenceDelegate.Pref<Boolean> = MessagesWaiting.serverRejectedEmail

        override val titleRes = R.string.email
        override val messageRes = R.string.email_verification_code_not_saved_bug

        override val negativeButtonTextRes: Int = R.string.disable
        override val negativeButtonAction: (()-> Unit) = { Prefs.sendBackupEmails(false) } // disable backup mails
    }

    object TEST_DIALOG: PersistentDialogMessage(){
        override val messageNeedsToBeDisplayedFlag: JoozdlogSharedPreferenceDelegate.Pref<Boolean> = MessagesWaiting.testDialogMessage

        override val titleRes = R.string.placeholder
        override val messageRes = R.string.test_message
    }

    // DO NOT DISPLAY THIS, just tell activity to close whatever PersistentDialog is open.
    object NO_DIALOG: PersistentDialogMessage(){
        override val messageNeedsToBeDisplayedFlag: JoozdlogSharedPreferenceDelegate.Pref<Boolean> = MessagesWaiting.noDialog
        override val titleRes = -1
        override val messageRes = -1
    }

    companion object{
        private const val PERSISTANT_DIALOG_MESSAGE_TAG = "PERSISTANT_DIALOG_MESSAGE"
    }
}



