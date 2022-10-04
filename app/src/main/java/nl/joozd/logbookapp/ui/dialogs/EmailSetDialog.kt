package nl.joozd.logbookapp.ui.dialogs

import android.app.AlertDialog
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.lifecycle.lifecycleScope
import kotlinx.coroutines.async
import kotlinx.coroutines.launch
import nl.joozd.logbookapp.R
import nl.joozd.logbookapp.core.emailFunctions.EmailCenter
import nl.joozd.logbookapp.data.sharedPrefs.EmailPrefs
import nl.joozd.logbookapp.databinding.DialogSetEmailBinding
import nl.joozd.logbookapp.extensions.getColorFromAttr
import nl.joozd.logbookapp.extensions.onTextChanged
import nl.joozd.logbookapp.ui.utils.JoozdlogFragment

/**
 * Dialog to set email. This is the ONLY way to let users set email, ALWAYS use this dialog.
 * This dialog checks if email address is valid before saving it, so that doesn't need to be done anywhere else.
 */
class EmailSetDialog : JoozdlogFragment() {
    private var currentlyEnteredText = ""
    private val currentlySavedEmailAsync = lifecycleScope.async { EmailPrefs.emailAddress() }

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View =
        // Inflate the layout for this fragment
        DialogSetEmailBinding.bind(inflater.inflate(R.layout.dialog_set_email, container, false)).apply {
            //onClickListeners for cancel button. OK button gets set through emailInputEditText.onTextChanged because it depends on entered text.
            setOnClickListeners()

            //onTextChangedListener for emailInputEditText
            setOnTextChangedListeners()

            //onFocusChangedListener for emailInputEditText
            setOnFocusChangedListeners()

            //restore previously entered data into emailInputEditText
            setInitialText(savedInstanceState)

        }.root


    override fun onSaveInstanceState(outState: Bundle) {
        super.onSaveInstanceState(outState)
        outState.putString(ENTERED_EMAIL_ADDRESS_TAG, currentlyEnteredText)
    }


    // If user entered text in a previous instance of this dialog, put that in the EditText.
    // Else put the currently saved email address in, unless it has focus.
    // (Don't do anything if it has focus. Edge case where user clicked faster than EmailPrefs provided email.)
    private fun DialogSetEmailBinding.setInitialText(savedInstanceState: Bundle?){
        val savedText = savedInstanceState?.getString(ENTERED_EMAIL_ADDRESS_TAG) ?: ""
        with(emailInputEditText) {
            if (savedText.isNotBlank()) {
                setText(savedText)
                enableSaveButtonIfValidEmail(savedText)
            } else lifecycleScope.launch {
                val currentEmail = currentlySavedEmailAsync.await()
                if (!hasFocus()) {
                    setText(currentEmail)
                    enableSaveButtonIfValidEmail(currentEmail)
                }
            }
        }
    }


    //onClickListener for OK button and background
    private fun DialogSetEmailBinding.setOnClickListeners(){
        //closes dialog, does nothing else.
        cancelEmailDialogTextview.setOnClickListener {
            closeFragment()
        }

        enterEmailDialogBackground.setOnClickListener { /* Intentionally left blank */ }
    }

    //onTextChangedListener for emailInputEditText
    //This constantly checks if entered text is a valid email address, and enables or disables SAVE button as appropriate.
    private fun DialogSetEmailBinding.setOnTextChangedListeners(){
        emailInputEditText.onTextChanged{
            currentlyEnteredText = it
            emailInputLayout.error = null
            enableSaveButtonIfValidEmail(it)
        }
    }

    //onFocusChangedListener for emailInputEditText. Shows an error in email box if focus lost and not a valid email address entered.
    private fun DialogSetEmailBinding.setOnFocusChangedListeners(){
        emailInputEditText.setOnFocusChangeListener { _, hasFocus ->
            if(!hasFocus && !isValidEmailAddress(currentlyEnteredText)) // if focus lost while entered text is not a valid email address
                emailInputLayout.error = getString(R.string.not_an_email_address)
                // Error will be removed as soon as user changes text in emailInputEditText
        }

    }


    // checks if enteredText is a valid email address, and enables or disables SAVE button as appropriate.
    private fun DialogSetEmailBinding.enableSaveButtonIfValidEmail(enteredText: String) {
        if (isValidEmailAddress(enteredText))
            enableSaveButton()
        else
            disableSaveButton()
    }


    private suspend fun enteredIsSameAsSaved(): Boolean =
        currentlyEnteredText == EmailPrefs.emailAddress()


    private fun isValidEmailAddress(emailAddress: String) =
        emailAddress.isNotBlank()
                && android.util.Patterns.EMAIL_ADDRESS.matcher(emailAddress).matches()


    private val okButtonOnClickLister: View.OnClickListener = View.OnClickListener {
        it.setOnClickListener { /* intentionally blank, will update when entered text is changed, this way we can save only once if launch is slow */ }
        lifecycleScope.launch {
            if (enteredIsSameAsSaved()) {
                askIfSameAddressShouldBeConfirmedAgain()
            } else
                saveEmail()
        }
    }

    private fun saveEmail() {
        currentlyEnteredText.let { enteredAddress ->// in let block so it cannot change while this is running
            if (isValidEmailAddress(enteredAddress)) {
                EmailCenter().changeEmailAddress(enteredAddress)
                showEmailSavedMessageAndCloseDialog()
            } else
                showBadEmailWhileSaveClickedError()
        }
    }

    private fun DialogSetEmailBinding.enableSaveButton(){
        saveEmailDialogTextview.apply{
            setOnClickListener(okButtonOnClickLister)
            setTextColor(requireActivity().getColorFromAttr(android.R.attr.colorAccent))
        }
    }

    private fun DialogSetEmailBinding.disableSaveButton(){
        saveEmailDialogTextview.apply{
            setOnClickListener { activity?.currentFocus?.clearFocus() } // clearing focus from EditText will show error on bad email address
            setTextColor(requireActivity().getColorFromAttr(android.R.attr.textColorTertiary))
        }
    }

    // edge-case dialog for when SAVE was not properly disabled for some reason. Closes fragment.
    private fun showBadEmailWhileSaveClickedError(){
        AlertDialog.Builder(activity).apply{
            setTitle(R.string.error)
            setMessage(R.string.not_an_email_address)
            setPositiveButton(android.R.string.ok){ _, _ ->
                closeFragment()
            }
        }.create().show()
    }

    // Shown when user tries to save the same address that is already saved. Clicking OK will tell EmailCenter to save the email as if it were new.
    private fun askIfSameAddressShouldBeConfirmedAgain(){
        AlertDialog.Builder(activity).apply{
            setTitle(R.string.email_address)
            setMessage(R.string.same_email_as_saved_do_you_want_to_reconfirm_)
            setPositiveButton(android.R.string.ok){ _, _ ->
                saveEmail()
            }
            setNegativeButton(android.R.string.cancel) { _, _ ->
                /* intentionally left blank */
            }
        }.create().show()
    }

    private fun showEmailSavedMessageAndCloseDialog(){
        AlertDialog.Builder(activity).apply{
            setTitle(R.string.email_saved)
            setMessage(R.string.email_saved_message)
            setPositiveButton(android.R.string.ok){ _, _ ->
                closeFragment()
            }
        }.create().show()
    }

    companion object{
        private const val ENTERED_EMAIL_ADDRESS_TAG = "tag1"
    }
}