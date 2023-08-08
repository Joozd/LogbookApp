package nl.joozd.logbookapp.ui.messages.welcomeMessages

import android.app.AlertDialog
import androidx.lifecycle.lifecycleScope
import kotlinx.coroutines.launch
import nl.joozd.logbookapp.data.sharedPrefs.WelcomeMessagesShownVersions
import nl.joozd.logbookapp.ui.dialogs.editFlightFragment.EditFlightFragment
import nl.joozd.logbookapp.ui.dialogs.editFlightFragment.namesDialog.Name2Dialog
import nl.joozd.logbookapp.ui.utils.JoozdlogFragment


/*
 * This is an easy way to show a welcome message to an activity.
 * Every supported Activity/Fragment needs it's own entry in the called function's 'when' switch.
 * Unsupported fragments//activities will do nothing.
 * Passing the fragment/activity to the function that is called from the 'when' switch means we can show pretty much whatever we want;
 * a simple dialog, a new fragment, launch a whole new activity; anything goes.
 * We probably should try to keep that a bit contained for continuity and UX.
 */
object WelcomeMessages {
    /**
     * Shows welcome message for [fragment].
     * Unsupported fragments will do nothing.
     */
    fun showWelcomeMessageForFragmentIfNeeded(fragment: JoozdlogFragment) = fragment.lifecycleScope.launch {
        when(fragment){
            is EditFlightFragment -> showWelcomeMessageForEditFlightFragment(fragment)
            is Name2Dialog -> showWelcomeMessageForName2Dialog(fragment)
        }
    }

    private suspend fun showWelcomeMessageForEditFlightFragment(fragment: JoozdlogFragment){
        val lastShownVersion = WelcomeMessagesShownVersions.editFlightFragment()
        EditFlightFragmentWelcomeMessages.getMessageForVersion(lastShownVersion)?.let{
            showDialog(fragment, it){
                WelcomeMessagesShownVersions.editFlightFragment(EditFlightFragmentWelcomeMessages.currentVersion)
            }
        }
    }

    private suspend fun showWelcomeMessageForName2Dialog(fragment: JoozdlogFragment){
        val lastShownVersion = WelcomeMessagesShownVersions.names2Dialog()
        Name2WelcomeMessages.getMessageForVersion(lastShownVersion)?.let{
            showDialog(fragment, it){
                WelcomeMessagesShownVersions.names2Dialog(Name2WelcomeMessages.currentVersion)
            }
        }
    }

    /**
     * Show a dialog displaying the data in [dialogContent]
     * Suggested use for [onClosed]: Update the data in [WelcomeMessagesShownVersions] so this message shows only once.
     */
    private fun showDialog(fragment: JoozdlogFragment, dialogContent: DialogContent, onClosed: OnDialogClosedListener){
        AlertDialog.Builder(fragment.requireActivity())
            .setTitle(dialogContent.titleRes)
            .setMessage(dialogContent.messageRes)
            .setPositiveButton(android.R.string.ok){ _, _ ->
                onClosed.onDialogClosed()
            }
            .create()
            .show()
    }

    private fun interface OnDialogClosedListener{
        fun onDialogClosed()
    }
}