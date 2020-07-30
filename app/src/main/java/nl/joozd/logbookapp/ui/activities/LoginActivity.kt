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
import androidx.lifecycle.Observer
import nl.joozd.logbookapp.R
import nl.joozd.logbookapp.data.comm.InternetStatus
import nl.joozd.logbookapp.data.comm.UserManagement
import nl.joozd.logbookapp.databinding.ActivityLoginBinding
import nl.joozd.logbookapp.databinding.ActivityNewUserPage2Binding
import nl.joozd.logbookapp.extensions.onTextChanged
import nl.joozd.logbookapp.model.feedbackEvents.FeedbackEvents
import nl.joozd.logbookapp.model.feedbackEvents.FeedbackEvents.LoginActivityEvents
import nl.joozd.logbookapp.model.viewmodels.activities.LoginActivityViewModel
import nl.joozd.logbookapp.ui.utils.customs.JoozdlogAlertDialog
import nl.joozd.logbookapp.ui.utils.toast


class LoginActivity : JoozdlogActivity(){
    private val activity = this
    val viewModel: LoginActivityViewModel by viewModels()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        if (UserManagement.justCreatedNewUser) finish()
        UserManagement.justCreatedNewUser = false
        setTheme(R.style.AppTheme)
        val binding = ActivityLoginBinding.inflate(layoutInflater).apply {

            singInButton.joozdLogSetBackgroundColor()

            /**
             * Every time this is recreated, check if server is online.
             * If not, viewModel will send a feedbackEvent for NO_INTERNET or SERVER_ERROR
             * In both cases, entering login/pass should still be possible, but it won't be checked
             * until next time it gets online.
             * TODO make a graphic feedback about this (ie. a message somewhere that server not reached)
             *
             * TODO MainActivity should get functionality to deal with server login errors,
             * TODO giving people the option to go to this screen and fix things.
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
            viewModel.feedbackEvent.observe(activity, Observer {
                when (it.getEvent()) {
                    LoginActivityEvents.PASSWORD_EMPTY -> showPasswordCannotBeEmptyError()
                    LoginActivityEvents.USERNAME_EMPTY -> showUsernameCannotBeEmptyError()
                    LoginActivityEvents.USERNAME_OR_PASSWORD_INCORRECT -> showPasswordIncorrectError()
                    LoginActivityEvents.NOT_IMPLEMENTED -> toast("Not implemented!")
                    LoginActivityEvents.SAVED_WITHOUT_CHECKING_BECAUSE_NO_INTERNET, LoginActivityEvents.SAVED_WITHOUT_CHECKING_BECAUSE_NO_SERVER -> showNoInternetLoginDialog()
                    LoginActivityEvents.FINISHED -> finish()
                    else -> toast("unhandled feedback: ${it.type}")
                }
            })

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
            message = "Well, now you will have to make a new account, and I will be stuck with a dead account taking resources on my server :(\nPlease don't forget the password again."
            setPositiveButton(android.R.string.ok)
        }.show()
    }
}
