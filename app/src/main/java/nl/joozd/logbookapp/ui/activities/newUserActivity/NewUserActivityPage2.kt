/*
 *  JoozdLog Pilot's Logbook
 *  Copyright (c) 2020 Joost Welle
 *
 *      This program is free software: you can redistribute it and/or modify
 *      it under the terms of the GNU Affero General Public License as
 *      published by the Free Software Foundation, either version 3 of the
 *      License, or (at your option) any later version.
 *
 *      This program is distributed in the hope that it will be useful,
 *      but WITHOUT ANY WARRANTY; without even the implied warranty of
 *      MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 *      GNU Affero General Public License for more details.
 *
 *      You should have received a copy of the GNU Affero General Public License
 *      along with this program.  If not, see https://www.gnu.org/licenses
 *
 */

package nl.joozd.logbookapp.ui.activities.newUserActivity

import android.os.Bundle
import android.text.Editable
import android.util.Log
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.EditText
import androidx.core.util.PatternsCompat.EMAIL_ADDRESS
import androidx.fragment.app.activityViewModels
import androidx.fragment.app.commit
import nl.joozd.logbookapp.App
import nl.joozd.logbookapp.R
import nl.joozd.logbookapp.data.comm.UserManagement
import nl.joozd.logbookapp.data.sharedPrefs.Preferences
import nl.joozd.logbookapp.databinding.ActivityNewUserPage2Binding
import nl.joozd.logbookapp.extensions.getStringWithMakeup
import nl.joozd.logbookapp.extensions.onTextChanged
import nl.joozd.logbookapp.model.feedbackEvents.FeedbackEvents.NewUserActivityEvents
import nl.joozd.logbookapp.model.viewmodels.activities.NewUserActivityViewModel
import nl.joozd.logbookapp.ui.dialogs.CloudSyncTermsDialog
import nl.joozd.logbookapp.ui.fragments.JoozdlogFragment
import nl.joozd.logbookapp.ui.utils.customs.JoozdlogAlertDialogV1
import nl.joozd.logbookapp.ui.utils.toast
import java.util.*

/**
 * Create new user!
 */
class NewUserActivityPage2: JoozdlogFragment() {


    val viewModel: NewUserActivityViewModel by activityViewModels()
    private var mBinding: ActivityNewUserPage2Binding? = null

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View? =
        ActivityNewUserPage2Binding.bind(inflater.inflate(R.layout.activity_new_user_page_2, container, false)).apply {

            // Restore texts from savedInstanceState, eg. on rotate or app switch
            userNameEditText.setTextIfNotNull(viewModel.userNameState)
            /*******************************************************************************************
             * EditText onFocusChanged and onTextChanged
             *******************************************************************************************/

            userNameEditText.setOnFocusChangeListener { _, hasFocus ->
                if (!hasFocus) userNameEditText.setText(userNameEditText.text.toString().toLowerCase(Locale.ROOT))
            }

            userNameEditText.onTextChanged {
                usernameTextInputLayout.error = ""
            }

            /*******************************************************************************************
             * OnClickedListeners
             *******************************************************************************************/

            signOutTextView.setOnClickListener {
                viewModel.signOutClicked()
            }

            skipThisStepTextView.setOnClickListener {
                viewModel.dontUseCloud()
                viewModel.nextPage(PAGE_NUMBER)
            }

            continueTextView.setOnClickListener {
                viewModel.nextPage(PAGE_NUMBER)
            }

            tcCheckbox.setOnClickListener {
                tcCheckbox.isChecked = false
                if (Preferences.acceptedCloudSyncTerms) Preferences.acceptedCloudSyncTerms = false
                else {
                    supportFragmentManager.commit {
                        add(R.id.newUserActivityLayout, CloudSyncTermsDialog())
                        addToBackStack(null)
                    }
                }
            }

            signUpButton.setOnClickListener {
                if (!EMAIL_ADDRESS.matcher(emailEditText.text.toString()).matches()) {
                    noEmailDialog{
                        if (Preferences.acceptedCloudSyncTerms)
                            viewModel.signUpClicked(userNameEditText.text)
                        else {
                            JoozdlogAlertDialogV1(requireActivity()).show {
                                messageResource = R.string.must_accept_terms
                                setPositiveButton(android.R.string.ok)
                            }
                        }
                    }
                }
                else {
                    if (Preferences.acceptedCloudSyncTerms)
                        viewModel.signUpClicked(userNameEditText.text, emailEditText.text)
                    else {
                        JoozdlogAlertDialogV1(requireActivity()).show {
                            messageResource = R.string.must_accept_terms
                            setPositiveButton(android.R.string.ok)
                        }
                    }
                }
            }


            /*******************************************************************************************
             * Observers:
             *******************************************************************************************/

            viewModel.username.observe(viewLifecycleOwner) {
                setLoggedInLayout(it)
            }

            viewModel.emailAddress.observe(viewLifecycleOwner){
                emailEditText.setText(it)
            }

            viewModel.acceptTerms.observe(viewLifecycleOwner) {
                tcCheckbox.isChecked = it
            }

            /**
             * Event observers:
             */

            // TODO this doesn't work as Activity eats these first, needs a personal feedback livedata
            viewModel.page2Feedback.observe(viewLifecycleOwner) {
                Log.d("Event!", "${it.type}, already consumed: ${it.consumed}")
                when (it.getEvent()) {
                    NewUserActivityEvents.NOT_IMPLEMENTED -> {
                        toast("Not implemented!")
                    }
                    NewUserActivityEvents.USER_EXISTS -> showUserExistsError()

                    NewUserActivityEvents.USERNAME_TOO_SHORT -> showUsernameCannotBeEmptyError()
                    NewUserActivityEvents.NO_INTERNET -> showCreateAccountNoInternetError()
                    NewUserActivityEvents.WAITING_FOR_SERVER -> setWaitingForServerLayout()
                    NewUserActivityEvents.SERVER_NOT_RESPONDING -> {
                        setNotWaitingForServerLayout()
                        showCreateAccountServerError()
                    }
                    NewUserActivityEvents.LOGGED_IN_AS -> showPasswordLinkDialog()
                    NewUserActivityEvents.FINISHED -> viewModel.nextPage(PAGE_NUMBER)
                }
            }

        }.root


    override fun onStop() {
        super.onStop()
        mBinding?.apply {
            viewModel.userNameState = userNameEditText.text
        }
    }




    /*******************************************************************************************
     * Functions showing AlertDialogs
     *******************************************************************************************/

    private fun showPasswordLinkDialog() =  JoozdlogAlertDialogV1(requireActivity()).show {

        title = App.instance.getString(R.string.created_account, UserManagement.username)
        messageResource = R.string.create_login_link_hint
        setPositiveButton(android.R.string.ok) {
            viewModel.copyLoginLinkToClipboard()
            viewModel.nextPage(PAGE_NUMBER)
        }

    }

    /**
     * Feedback dialogs for when bad data is entered when creating account
     */
    private fun showCreateAccountServerError() =
        JoozdlogAlertDialogV1(requireActivity()).apply {
            titleResource = R.string.no_internet
            messageResource = R.string.no_server_create_account
            setPositiveButton(R.string._continue) {
                viewModel.dontUseCloud()
                viewModel.nextPage(PAGE_NUMBER)
            }
        }.show()

    private fun showCreateAccountNoInternetError() =
        JoozdlogAlertDialogV1(requireActivity()).apply {
            titleResource = R.string.no_internet
            messageResource = R.string.no_internet_create_account
            setPositiveButton(R.string.skip) {
                viewModel.dontUseCloud()
                viewModel.nextPage(PAGE_NUMBER)
            }
            setNegativeButton(R.string.retry){
                mBinding?.run{
                    viewModel.signUpClicked(userNameEditText.text)
                }
            }
            setNeutralButton(android.R.string.cancel) {}
        }.show()


    private fun ActivityNewUserPage2Binding.showUsernameCannotBeEmptyError() {
        usernameTextInputLayout.error = requireActivity().getString(R.string.username_cannot_be_empty)
        userNameEditText.requestFocus()
    }

    private fun showUserExistsError() =
        JoozdlogAlertDialogV1(requireActivity()).show {
            Log.d("XOXOXOXOXOXO", "LALALALALALALALALALALALAHIHIHIHIHIHIJOOOOO")
            titleResource = R.string.username_already_taken
            setPositiveButton(android.R.string.ok)
        }

    /**
     * Shows a dialog complaining no email was entered. Positive button will execute function provided as [f].
     * Negative button will close the dialog.
     */
    private fun noEmailDialog(f: () -> Unit) =
        JoozdlogAlertDialogV1(requireActivity()).show {
            titleResource = R.string.username_already_taken
            messageResource = R.string.no_email_text
            setPositiveButton(R.string.i_dont_care){
                f()
            }
            setNegativeButton(android.R.string.cancel)
        }



    /**
     * Other dialogs
     */

    /*******************************************************************************************
     * Functions for changing layout
     *******************************************************************************************/

    /**
     *  Sets layout as "logged in already" if username is provided, as "create new account" if not
     */
    private fun ActivityNewUserPage2Binding.setLoggedInLayout(username: String? = null){
        val enabled = username != null
        val ifLoggedOut = if (enabled) View.GONE else View.VISIBLE // show if not logged in
        val ifLoggedIn = if (enabled) View.VISIBLE else View.GONE // show if logged in

        usernameTextInputLayout.visibility = ifLoggedOut
        emailTextInputLayout.visibility = ifLoggedOut
        emailReasonTextView.visibility = ifLoggedOut
        signUpButton.visibility = ifLoggedOut
        tcCheckbox.visibility = ifLoggedOut

        dontWantCloudSyncTextView.visibility = ifLoggedOut
        skipThisStepTextView.visibility = ifLoggedOut

        youAreSignedInAsTextView.visibility = ifLoggedIn
        signOutTextView.visibility = ifLoggedIn
        continueTextView.visibility = ifLoggedIn

        youAreSignedInAsTextView.text = requireActivity().getStringWithMakeup(R.string.you_are_signed_in_as, username)
    }

    //TODO make this
    private fun ActivityNewUserPage2Binding.setWaitingForServerLayout(){
        toast("waiting for server")
    }

    private fun ActivityNewUserPage2Binding.setNotWaitingForServerLayout(){
        toast("No longer waiting for server")
    }


    /*******************************************************************************************
     * Helper functions
     *******************************************************************************************/

    private fun View.makeEnabled(enabled: Boolean){
        isEnabled = enabled
        isFocusable = enabled
    }

    private fun EditText.setTextIfNotNull(t: Editable?){
        t?.let{
            text = it
        }
    }



    companion object{
        private const val PAGE_NUMBER = 2
        private const val USERNAME_BUNDLE_KEY = "USERNAME"
    }
}