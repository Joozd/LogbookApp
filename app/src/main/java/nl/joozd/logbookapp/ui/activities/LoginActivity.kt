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
import androidx.activity.viewModels
import nl.joozd.logbookapp.R
import nl.joozd.logbookapp.data.comm.InternetStatus
import nl.joozd.logbookapp.data.comm.UserManagement
import nl.joozd.logbookapp.data.sharedPrefs.Preferences
import nl.joozd.logbookapp.databinding.ActivityLoginBinding
import nl.joozd.logbookapp.extensions.onTextChanged
import nl.joozd.logbookapp.model.feedbackEvents.FeedbackEvents.LoginActivityEvents
import nl.joozd.logbookapp.model.viewmodels.activities.LoginActivityViewModel
import nl.joozd.logbookapp.ui.utils.customs.JoozdlogAlertDialog
import nl.joozd.logbookapp.ui.utils.toast


class LoginActivity : JoozdlogActivity(){
    val viewModel: LoginActivityViewModel by viewModels()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        if (UserManagement.justCreatedNewUser) finish()
        UserManagement.justCreatedNewUser = false
        setTheme(R.style.AppTheme)
        ActivityLoginBinding.inflate(layoutInflater).apply {
            singInButton.joozdLogSetBackgroundColor()
            Preferences.username?.let{
                usernameEditText.setText(it)
                passwordEditText.setText(R.string.fake_hidden_password)
            }

            /**
             * Every time this is recreated, check if server is online.
             * If not, viewModel will send a feedbackEvent for NO_INTERNET or SERVER_ERROR
             * In both cases, entering login/pass should still be possible, but it won't be checked
             * until next time it gets online.
             * TODO make a graphic feedback about this (ie. a message somewhere that server not reached)
             */

            setSupportActionBarWithReturn(loginActivityToolbar)?.apply {
                setDisplayHomeAsUpEnabled(true)
                title = resources.getString(R.string.title_activity_login)
            }

            /*******************************************************************************************
             * EditText onFocusChanged and onTextChanged
             *******************************************************************************************/


            usernameEditText.setOnFocusChangeListener { _, hasFocus ->
                if (!hasFocus) usernameEditText.setText(
                    usernameEditText.text.toString().toLowerCase(
                        java.util.Locale.ROOT
                    )
                )
            }

            usernameEditText.onTextChanged {
                usernameLayout.error = ""
            }
            passwordEditText.onTextChanged {
                passwordLayout.error = null
            }

            /*******************************************************************************************
             * OnClickedListeners
             *******************************************************************************************/

            singInButton.setOnClickListener {
                viewModel.signIn(
                    usernameEditText.text.toString(),
                    passwordEditText.text.toString()
                )
            }

            forgotPasswordTextView.setOnClickListener {
                showYouAreAnIdiotDialog()
            }

            makeNewAccountTextView.setOnClickListener {
                if (InternetStatus.internetAvailable == true)
                    startActivity(Intent(activity, CreateNewUserActivity::class.java))
                else showNoInternetCreateNewDialog()
            }




            /*******************************************************************************************
             * Observers:
             *******************************************************************************************/


            /**
             * Feedback events:
             */
            viewModel.feedbackEvent.observe(activity){
                when (it.getEvent()) {
                    LoginActivityEvents.PASSWORD_EMPTY -> showPasswordCannotBeEmptyError()
                    LoginActivityEvents.USERNAME_EMPTY -> showUsernameCannotBeEmptyError()
                    LoginActivityEvents.USERNAME_OR_PASSWORD_INCORRECT -> showPasswordIncorrectError()
                    LoginActivityEvents.NOT_IMPLEMENTED -> toast("Not implemented!")
                    LoginActivityEvents.SAVED_WITHOUT_CHECKING_BECAUSE_NO_INTERNET, LoginActivityEvents.SAVED_WITHOUT_CHECKING_BECAUSE_NO_SERVER -> showNoInternetLoginDialog()
                    LoginActivityEvents.FINISHED -> finish()
                    else -> toast("unhandled feedback: ${it.type}")
                }
            }

            //
            setContentView(root)
        }
    }

    override fun onSupportNavigateUp(): Boolean {
        onBackPressed()
        return true
    }

    private fun ActivityLoginBinding.showPasswordCannotBeEmptyError() {
        passwordLayout.error = activity.getString(R.string.password_cannot_be_empty)
        passwordEditText.requestFocus()
    }

    private fun ActivityLoginBinding.showUsernameCannotBeEmptyError() {
        usernameLayout.error = activity.getString(R.string.username_cannot_be_empty)
        usernameEditText.requestFocus()
    }

    private fun ActivityLoginBinding.showPasswordIncorrectError() {
        passwordLayout.error = activity.getString(R.string.wrong_username_password)
        passwordEditText.requestFocus()
    }


    private fun showNoInternetCreateNewDialog(){
        JoozdlogAlertDialog(activity).apply{
            titleResource = R.string.no_internet
            messageResource = R.string.no_internet_create_account
            setPositiveButton(android.R.string.ok)
        }.show()
    }

    private fun showNoInternetLoginDialog(){
        JoozdlogAlertDialog(activity).apply{
            titleResource = R.string.no_internet
            messageResource = R.string.no_internet_login
            setPositiveButton(android.R.string.ok){
                finish()
            }
        }.show()
    }


    private fun showYouAreAnIdiotDialog(){
        JoozdlogAlertDialog(activity).apply{
            title = "DUH"
            messageResource = (R.string.what_to_do_forgot_password)
            setPositiveButton(android.R.string.ok)
        }.show()
    }
}
