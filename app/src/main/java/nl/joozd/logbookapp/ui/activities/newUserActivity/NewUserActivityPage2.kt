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
import androidx.fragment.app.activityViewModels
import androidx.fragment.app.commit
import androidx.lifecycle.Observer
import nl.joozd.logbookapp.R
import nl.joozd.logbookapp.data.sharedPrefs.Preferences
import nl.joozd.logbookapp.databinding.ActivityNewUserPage2Binding
import nl.joozd.logbookapp.extensions.getStringWithMakeup
import nl.joozd.logbookapp.extensions.onTextChanged
import nl.joozd.logbookapp.model.feedbackEvents.FeedbackEvents.NewUserActivityEvents
import nl.joozd.logbookapp.model.viewmodels.activities.NewUserActivityViewModel
import nl.joozd.logbookapp.ui.dialogs.CloudSyncTermsDialog
import nl.joozd.logbookapp.ui.fragments.JoozdlogFragment
import nl.joozd.logbookapp.ui.utils.customs.JoozdlogAlertDialog
import nl.joozd.logbookapp.ui.utils.toast
import java.util.*

class NewUserActivityPage2: JoozdlogFragment() {

    val viewModel: NewUserActivityViewModel by activityViewModels()
    private var mBinding: ActivityNewUserPage2Binding? = null

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View? {
        val binding = ActivityNewUserPage2Binding.bind(inflater.inflate(R.layout.activity_new_user_page_2, container, false))
        mBinding = binding


        // Restore texts from savedInstanceState, eg. on rotate or app switch
        binding.apply{
            userNameEditText.setTextIfNotNull(viewModel.userNameState)
            passwordEditText.setTextIfNotNull(viewModel.password1State)
            repeatPasswordEditText.setTextIfNotNull(viewModel.password2State)
        }



        /*******************************************************************************************
         * EditText onFocusChanged and onTextChanged
         *******************************************************************************************/

        with(binding){
            userNameEditText.setOnFocusChangeListener { _, hasFocus ->
                if (!hasFocus) userNameEditText.setText(userNameEditText.text.toString().toLowerCase(Locale.ROOT))
            }

            userNameEditText.onTextChanged {
                usernameTextInputLayout.error = ""
            }
            passwordEditText.onTextChanged {
                passwordTextInputLayout.error = null
            }
            repeatPasswordEditText.onTextChanged {
                repeatPasswordTextInputLayout.error = ""
            }
        }

        /*******************************************************************************************
         * OnClickedListeners
         *******************************************************************************************/
        with(binding) {
            signInTextView.setOnClickListener {
                viewModel.signInClicked()
            }

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
                if (Preferences.acceptedCloudSyncTerms)
                    JoozdlogAlertDialog(requireActivity()).apply {
                        messageResource = R.string.cannot_restore_password
                        setPositiveButton(android.R.string.ok){
                            viewModel.signUpClicked(userNameEditText.text.toString(), passwordEditText.text.toString(), repeatPasswordEditText.text.toString())
                        }
                        setNegativeButton(R.string.already_forgot)
                    }.show()
                else {
                    JoozdlogAlertDialog(requireActivity()).apply {
                        messageResource = R.string.must_accept_terms
                        setPositiveButton(android.R.string.ok)
                    }.show()
                }
            }

            passwordRequirementsText.setOnClickListener {
                showPasswordRequirements()
            }
        }

        /*******************************************************************************************
         * Observers:
         *******************************************************************************************/

        viewModel.username.observe(viewLifecycleOwner, Observer {
            binding.setLoggedInLayout(it)
        })

        viewModel.acceptTerms.observe(viewLifecycleOwner, Observer {
            binding.tcCheckbox.isChecked = it
        })

        /**
         * Event observers:
         */

        // TODO this doesn't work as Activity eats these first, needs a personal feedback livedata
        viewModel.page2Feedback.observe(viewLifecycleOwner, Observer {
            Log.d("Event!", "${it.type}")
            when(it.getEvent()){
                NewUserActivityEvents.NOT_IMPLEMENTED -> { toast("Not implemented!")}
                NewUserActivityEvents.USER_EXISTS_PASSWORD_INCORRECT -> showUserExistsError(binding)
                NewUserActivityEvents.USER_EXISTS_PASSWORD_CORRECT -> showUserExistsPasswordCorrect(binding)
                NewUserActivityEvents.PASSWORDS_DO_NOT_MATCH -> showPasswordsDoNotMatchError(binding)
                NewUserActivityEvents.PASSWORD_DOES_NOT_MEET_STANDARDS -> showPasswordDoesNotMeetStandardsError(binding)
                NewUserActivityEvents.PASSWORD_TOO_SHORT -> showPasswordCannotBeEmptyError(binding)
                NewUserActivityEvents.USERNAME_TOO_SHORT -> showUsernameCannotBeEmptyError(binding)
                NewUserActivityEvents.NO_INTERNET -> showCreateAccountNoInternetError()
                NewUserActivityEvents.WAITING_FOR_SERVER -> binding.setWaitingForServerLayout()
                NewUserActivityEvents.SERVER_NOT_RESPONDING -> {
                    binding.setNotWaitingForServerLayout()
                    showCreateAccountServerError()
                }
                NewUserActivityEvents.LOGGED_IN_AS -> viewModel.nextPage(PAGE_NUMBER)
                NewUserActivityEvents.FINISHED -> viewModel.nextPage(PAGE_NUMBER)
            }
        })

        return binding.root
    }

    override fun onStop() {
        super.onStop()
        mBinding?.apply {
            viewModel.userNameState = userNameEditText.text
            viewModel.password1State = passwordEditText.text
            viewModel.password2State = repeatPasswordEditText.text
        }
    }




    /*******************************************************************************************
     * Functions showing AlertDialogs
     *******************************************************************************************/

    /**
     * Feedback dialogs for when bad data is entered when creating account
     */
    private fun showCreateAccountServerError() =
        JoozdlogAlertDialog(requireActivity()).apply {
            titleResource = R.string.no_internet
            messageResource = R.string.no_server_create_account
            setPositiveButton(R.string._continue) {
                viewModel.dontUseCloud()
                viewModel.nextPage(PAGE_NUMBER)
            }
        }.show()

    private fun showCreateAccountNoInternetError() =
        JoozdlogAlertDialog(requireActivity()).apply {
            titleResource = R.string.no_internet
            messageResource = R.string.no_internet_create_account
            setPositiveButton(R.string.skip) {
                viewModel.dontUseCloud()
                viewModel.nextPage(PAGE_NUMBER)
            }
            setNegativeButton(R.string.retry){
                mBinding?.run{
                    viewModel.signUpClicked(userNameEditText.text.toString(), passwordEditText.text.toString(), repeatPasswordEditText.text.toString())
                }
            }
            setNeutralButton(android.R.string.cancel) {}
        }.show()

    private fun showPasswordCannotBeEmptyError(binding: ActivityNewUserPage2Binding) {
        binding.passwordTextInputLayout.error = requireActivity().getString(R.string.password_cannot_be_empty)
        binding.passwordEditText.requestFocus()
    }


    private fun showUsernameCannotBeEmptyError(binding: ActivityNewUserPage2Binding) {
        binding.usernameTextInputLayout.error = requireActivity().getString(R.string.username_cannot_be_empty)
        binding.userNameEditText.requestFocus()
    }

    private fun showPasswordsDoNotMatchError(binding: ActivityNewUserPage2Binding){
        binding.repeatPasswordTextInputLayout.error = requireActivity().getString(R.string.passwords_do_not_match)
        binding.userNameEditText.requestFocus()
    }

    private fun showPasswordDoesNotMeetStandardsError(binding: ActivityNewUserPage2Binding){
        binding.passwordTextInputLayout.error = requireActivity().getString(R.string.password_does_not_meet_standards)
        binding.passwordEditText.requestFocus()
    }

    private fun showUserExistsError(binding: ActivityNewUserPage2Binding) =
        JoozdlogAlertDialog(requireActivity()).apply {
            titleResource = R.string.username_already_taken
            setPositiveButton(android.R.string.ok) {
                //TODO flash username box and place cursor in it
            }
        }

    private fun showUserExistsPasswordCorrect(binding: ActivityNewUserPage2Binding) =
        JoozdlogAlertDialog(requireActivity()).apply {
            titleResource = R.string.user_exists_password_correct
            setPositiveButton(android.R.string.ok) {
                binding.setLoggedInLayout(Preferences.username)
            }
        }


    /**
     * Other dialogs
     */

    private fun showPasswordRequirements() = JoozdlogAlertDialog(requireActivity()).apply {
        titleResource = R.string.pass_requirements
        setPositiveButton(android.R.string.ok)
    }

    /*******************************************************************************************
     * Functions for changing layout
     *******************************************************************************************/

    /**
     *  Sets layout as "logged in already" if username is provided, as "create new account" if not
     */
    private fun ActivityNewUserPage2Binding.setLoggedInLayout(username: String? = null){
        val enabled = username != null

        userNameEditText.makeEnabled(!enabled)
        passwordEditText.makeEnabled(!enabled)
        repeatPasswordEditText.makeEnabled(!enabled)
        signUpButton.makeEnabled(!enabled)
        tcCheckbox.makeEnabled(!enabled)

        alreadyHaveAnAccountFirstPartTextView.visibility = if (enabled) View.GONE else View.VISIBLE
        signInTextView.visibility =  if (enabled) View.GONE else View.VISIBLE
        dontWantCloudSyncTextView.visibility = if (enabled) View.GONE else View.VISIBLE
        skipThisStepTextView.visibility = if (enabled) View.GONE else View.VISIBLE
        passwordRequirementsText.visibility = if (enabled) View.GONE else View.VISIBLE

        youAreSignedInAsTextView.visibility = if (enabled) View.VISIBLE else View.GONE
        signOutTextView.visibility = if (enabled) View.VISIBLE else View.GONE
        continueTextView.visibility = if (enabled) View.VISIBLE else View.GONE

        youAreSignedInAsTextView.text = requireActivity().getStringWithMakeup(R.string.you_are_signed_in_as, username)
    }

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
        private const val PASS1_BUNDLE_KEY = "PASS1"
        private const val PASS2_BUNDLE_KEY = "PASS2"
    }
}