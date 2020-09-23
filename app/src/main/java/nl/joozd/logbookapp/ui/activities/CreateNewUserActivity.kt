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

package nl.joozd.logbookapp.ui.activities

import android.os.Bundle
import android.text.Editable
import android.util.Log
import android.widget.EditText
import androidx.activity.viewModels
import androidx.fragment.app.commit
import androidx.lifecycle.Observer
import nl.joozd.logbookapp.R
import nl.joozd.logbookapp.data.sharedPrefs.Preferences
import nl.joozd.logbookapp.databinding.ActivityCreateNewUserBinding
import nl.joozd.logbookapp.extensions.onTextChanged
import nl.joozd.logbookapp.model.feedbackEvents.FeedbackEvents
import nl.joozd.logbookapp.model.viewmodels.activities.CreateNewUserActivityViewModel
import nl.joozd.logbookapp.ui.utils.customs.JoozdlogAlertDialog
import nl.joozd.logbookapp.ui.utils.toast

class CreateNewUserActivity : JoozdlogActivity() {
    private val viewModel: CreateNewUserActivityViewModel by viewModels()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setTheme(R.style.AppTheme)
        ActivityCreateNewUserBinding.inflate(layoutInflater).apply {

            /**
             * Set layout the way I want to
             */
            signUpButton.joozdLogSetBackgroundColor()


            setSupportActionBarWithReturn(createNewUserActivityToolbar)?.apply {
                setDisplayHomeAsUpEnabled(true)
                title = resources.getString(R.string.newUserActivityHeader)
            }

            // Restore texts from savedInstanceState, eg. on rotate or app switch
            userNameEditText.setTextIfNotNull(viewModel.userNameState)
            passwordEditText.setTextIfNotNull(viewModel.password1State)
            repeatPasswordEditText.setTextIfNotNull(viewModel.password2State)

            /*******************************************************************************************
             * EditText onFocusChanged and onTextChanged
             *******************************************************************************************/


            userNameEditText.setOnFocusChangeListener { _, hasFocus ->
                if (!hasFocus) userNameEditText.setText(
                    userNameEditText.text.toString().toLowerCase(
                        java.util.Locale.ROOT
                    )
                )
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

            /*******************************************************************************************
             * OnClickedListeners
             *******************************************************************************************/

            tcCheckbox.setOnClickListener {
                tcCheckbox.isChecked = false
                if (Preferences.acceptedCloudSyncTerms) Preferences.acceptedCloudSyncTerms = false
                else {
                    supportFragmentManager.commit {
                        add(R.id.createNewUserActivityLayout, nl.joozd.logbookapp.ui.dialogs.CloudSyncTermsDialog())
                        addToBackStack(null)
                    }
                }
            }

            signUpButton.setOnClickListener {
                if (Preferences.acceptedCloudSyncTerms)
                    JoozdlogAlertDialog(activity).show {
                        messageResource = nl.joozd.logbookapp.R.string.cannot_restore_password
                        setPositiveButton(android.R.string.ok){
                            viewModel.signUpClicked(userNameEditText.text.toString(), passwordEditText.text.toString(), repeatPasswordEditText.text.toString())
                        }
                        setNegativeButton(nl.joozd.logbookapp.R.string.already_forgot)
                    }
                else {
                    JoozdlogAlertDialog(activity).show {
                        messageResource = nl.joozd.logbookapp.R.string.must_accept_terms
                        setPositiveButton(android.R.string.ok)
                    }
                }
            }

            passwordRequirementsText.setOnClickListener {
                showPasswordRequirements()
            }


            /*******************************************************************************************
             * Observers:
             *******************************************************************************************/

            viewModel.acceptedTerms.observe(activity, Observer {
                tcCheckbox.isChecked = it
            })

            /*******************************************************************************************
             * Viewmodel feedback
             *******************************************************************************************/

            viewModel.feedbackEvent.observe(activity, Observer {
                Log.d("Event!", "${it.type}")
                when(it.getEvent()){
                    FeedbackEvents.NewUserActivityEvents.NOT_IMPLEMENTED -> { toast("Not implemented!")}
                    FeedbackEvents.NewUserActivityEvents.USER_EXISTS_PASSWORD_INCORRECT -> showUserExistsError(this)
                    FeedbackEvents.NewUserActivityEvents.USER_EXISTS_PASSWORD_CORRECT -> showUserExistsPasswordCorrect(this)
                    FeedbackEvents.NewUserActivityEvents.PASSWORDS_DO_NOT_MATCH -> showPasswordsDoNotMatchError(this)
                    FeedbackEvents.NewUserActivityEvents.PASSWORD_DOES_NOT_MEET_STANDARDS -> showPasswordDoesNotMeetStandardsError(this)
                    FeedbackEvents.NewUserActivityEvents.PASSWORD_TOO_SHORT -> {
                        showPasswordCannotBeEmptyError(this)
                    }
                    FeedbackEvents.NewUserActivityEvents.USERNAME_TOO_SHORT -> showUsernameCannotBeEmptyError(this)
                    FeedbackEvents.NewUserActivityEvents.NO_INTERNET -> showCreateAccountNoInternetError(this)
                    FeedbackEvents.NewUserActivityEvents.WAITING_FOR_SERVER -> this.setWaitingForServerLayout()
                    FeedbackEvents.NewUserActivityEvents.SERVER_NOT_RESPONDING -> {
                        setNotWaitingForServerLayout()
                        showCreateAccountServerError()
                    }
                    FeedbackEvents.NewUserActivityEvents.FINISHED -> finish()
                }
            })


            setContentView(root)
        }
    }


    override fun onSupportNavigateUp(): Boolean {
        onBackPressed()
        return true
    }

    /*******************************************************************************************
     * Functions showing AlertDialogs
     *******************************************************************************************/

    /**
     * Show password requirements. At this moment just a static string. TODO Maybe generate something from the function that checks it? Or is that just too much fuzz?
     */
    private fun showPasswordRequirements() = JoozdlogAlertDialog(activity).show {
        titleResource = R.string.pass_requirements
        messageResource = R.string.password_requirements
        setPositiveButton(android.R.string.ok)
    }


    private fun showCreateAccountServerError() =
        JoozdlogAlertDialog(activity).apply {
            titleResource = R.string.no_internet
            messageResource = R.string.no_server_create_account
            setPositiveButton(R.string._continue) {
                viewModel.dontUseCloud()
                finish()
            }
        }.show()

    private fun showCreateAccountNoInternetError(binding: ActivityCreateNewUserBinding ) =
        JoozdlogAlertDialog(activity).show {
            titleResource = R.string.no_internet
            messageResource = R.string.no_internet_create_account
            setPositiveButton(R.string.skip) {
                viewModel.dontUseCloud()
                finish()
            }
            setNegativeButton(R.string.retry){
                binding.run{
                    viewModel.signUpClicked(userNameEditText.text.toString(), passwordEditText.text.toString(), repeatPasswordEditText.text.toString())
                }
            }
            setNeutralButton(android.R.string.cancel) {}
        }

    private fun showPasswordCannotBeEmptyError(binding: ActivityCreateNewUserBinding) {
        binding.passwordTextInputLayout.error = activity.getString(R.string.password_cannot_be_empty)
        binding.passwordEditText.requestFocus()
    }


    private fun showUsernameCannotBeEmptyError(binding: ActivityCreateNewUserBinding) {
        binding.usernameTextInputLayout.error = activity.getString(R.string.username_cannot_be_empty)
        binding.userNameEditText.requestFocus()
    }

    private fun showPasswordsDoNotMatchError(binding: ActivityCreateNewUserBinding){
        binding.repeatPasswordTextInputLayout.error = activity.getString(R.string.passwords_do_not_match)
        binding.userNameEditText.requestFocus()
    }

    private fun showPasswordDoesNotMeetStandardsError(binding: ActivityCreateNewUserBinding){
        binding.passwordTextInputLayout.error = activity.getString(R.string.password_does_not_meet_standards)
        binding.passwordEditText.requestFocus()
    }

    private fun showUserExistsError(binding: ActivityCreateNewUserBinding) = with (binding) {
        userNameEditText.setText("")
        userNameEditText.requestFocus()
        usernameTextInputLayout.error = getString(R.string.username_already_taken)
    }
        /*
        JoozdlogAlertDialog(activity).show {
            titleResource = R.string.username_already_taken
            setPositiveButton(android.R.string.ok) {
                //TODO flash username box and place cursor in it
            }
        }

         */

    private fun showUserExistsPasswordCorrect(binding: ActivityCreateNewUserBinding) =
        JoozdlogAlertDialog(activity).show {
            titleResource = R.string.user_exists_password_correct
            setPositiveButton(R.string.use_this) {
                finish()
            }
            setNegativeButton(android.R.string.cancel){
                viewModel.signOut()
            }
        }


    /*******************************************************************************************
     * Functions changing layout
     *******************************************************************************************/

    // TODO
    private fun ActivityCreateNewUserBinding.setWaitingForServerLayout(){
        toast("TODO -- waiting for server")
    }

    // TODO
    private fun ActivityCreateNewUserBinding.setNotWaitingForServerLayout(){
        toast("TODO -- No longer waiting for server")
    }

    /*******************************************************************************************
     * Helper functions
     *******************************************************************************************/

    private fun EditText.setTextIfNotNull(t: Editable?){
        t?.let{
            text = it
        }
    }
}