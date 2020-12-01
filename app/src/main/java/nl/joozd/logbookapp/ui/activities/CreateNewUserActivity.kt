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

import android.content.Intent
import android.os.Bundle
import android.text.Editable
import android.util.Log
import android.widget.EditText
import androidx.activity.viewModels
import androidx.fragment.app.commit
import nl.joozd.logbookapp.R
import nl.joozd.logbookapp.data.comm.UserManagement
import nl.joozd.logbookapp.data.sharedPrefs.Preferences
import nl.joozd.logbookapp.databinding.ActivityCreateNewUserBinding
import nl.joozd.logbookapp.extensions.onTextChanged
import nl.joozd.logbookapp.model.feedbackEvents.FeedbackEvents.CreateNewUserActivityEvents
import nl.joozd.logbookapp.model.viewmodels.activities.CreateNewUserActivityViewModel
import nl.joozd.logbookapp.ui.utils.customs.JoozdlogAlertDialogV1
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
                    viewModel.signUpClicked(userNameEditText.text.toString())
                else {
                    JoozdlogAlertDialogV1(activity).show {
                        messageResource = R.string.must_accept_terms
                        setPositiveButton(android.R.string.ok)
                    }
                }
            }

            /*******************************************************************************************
             * Observers:
             *******************************************************************************************/

            viewModel.acceptedTerms.observe(activity) {
                tcCheckbox.isChecked = it
            }

            /*******************************************************************************************
             * Viewmodel feedback
             *******************************************************************************************/

            viewModel.feedbackEvent.observe(activity) {
                Log.d("Event!", "${it.type}")
                when(it.getEvent()){
                    CreateNewUserActivityEvents.USER_EXISTS -> showUserExistsError()
                    CreateNewUserActivityEvents.USERNAME_TOO_SHORT -> showUsernameCannotBeEmptyError()
                    CreateNewUserActivityEvents.NO_INTERNET -> showCreateAccountNoInternetError()
                    CreateNewUserActivityEvents.WAITING_FOR_SERVER -> this.setWaitingForServerLayout()
                    CreateNewUserActivityEvents.SERVER_NOT_RESPONDING -> {
                        setNotWaitingForServerLayout()
                        showCreateAccountServerError()
                    }
                    CreateNewUserActivityEvents.FINISHED -> exportLoginLink()
                }
            }


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

    private fun showCreateAccountServerError() =
        JoozdlogAlertDialogV1(activity).apply {
            titleResource = R.string.no_internet
            messageResource = R.string.no_server_create_account
            setPositiveButton(R.string._continue) {
                viewModel.dontUseCloud()
                finish()
            }
        }.show()

    private fun ActivityCreateNewUserBinding.showCreateAccountNoInternetError() =
        JoozdlogAlertDialogV1(activity).show {
            titleResource = R.string.no_internet
            messageResource = R.string.no_internet_create_account
            setPositiveButton(R.string.skip) {
                viewModel.dontUseCloud()
                finish()
            }
            setNegativeButton(R.string.retry){
                run{
                    viewModel.signUpClicked(userNameEditText.text.toString())
                }
            }
            setNeutralButton(android.R.string.cancel) {}
        }


    private fun ActivityCreateNewUserBinding.showUsernameCannotBeEmptyError() {
        usernameTextInputLayout.error = activity.getString(R.string.username_cannot_be_empty)
        userNameEditText.requestFocus()
    }

    private fun ActivityCreateNewUserBinding.showUserExistsError(){
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

    private fun exportLoginLink() {
        UserManagement.generateLoginLinkIntent()?.let {
            if (intent.resolveActivity(packageManager) != null) {
                startActivity(Intent.createChooser(it, getString(R.string.send_using)))
            }
        } ?: toast("NewUserActivity error 1: No login stored")
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