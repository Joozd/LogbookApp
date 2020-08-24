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

package nl.joozd.logbookapp.ui.dialogs

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.fragment.app.viewModels
import nl.joozd.logbookapp.R
import nl.joozd.logbookapp.databinding.ActivityLoginBinding
import nl.joozd.logbookapp.databinding.DialogLoginBinding
import nl.joozd.logbookapp.extensions.onTextChanged
import nl.joozd.logbookapp.model.feedbackEvents.FeedbackEvents
import nl.joozd.logbookapp.model.viewmodels.activities.LoginActivityViewModel
import nl.joozd.logbookapp.ui.fragments.JoozdlogFragment
import nl.joozd.logbookapp.ui.utils.customs.JoozdlogAlertDialog
import nl.joozd.logbookapp.ui.utils.toast

class LoginDialog: JoozdlogFragment() {
    //reuse viewModel from LoginActivity as that already has all the logic we need here
    private val viewModel: LoginActivityViewModel by viewModels()


    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View? {
        DialogLoginBinding.bind(inflater.inflate(R.layout.dialog_login, container, false)).apply{
            topHalf.joozdLogSetBackgroundColor()

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
             * Buttons & Backgrounds
             *******************************************************************************************/

            singInButton.setOnClickListener {
                //TODO make visual that something is happening
                viewModel.signIn(
                    usernameEditText.text.toString(),
                    passwordEditText.text.toString()
                )
            }

            background.setOnClickListener{} // do nothing, just catch missed clicks


            /*************************************************************************************
             * Observers
             *************************************************************************************/

            viewModel.feedbackEvent.observe(viewLifecycleOwner){
                when (it.getEvent()) {
                    FeedbackEvents.LoginActivityEvents.PASSWORD_EMPTY -> showPasswordCannotBeEmptyError()
                    FeedbackEvents.LoginActivityEvents.USERNAME_EMPTY -> showUsernameCannotBeEmptyError()
                    FeedbackEvents.LoginActivityEvents.USERNAME_OR_PASSWORD_INCORRECT -> showPasswordIncorrectError()
                    FeedbackEvents.LoginActivityEvents.NOT_IMPLEMENTED -> toast("Not implemented!")
                    FeedbackEvents.LoginActivityEvents.SAVED_WITHOUT_CHECKING_BECAUSE_NO_INTERNET, FeedbackEvents.LoginActivityEvents.SAVED_WITHOUT_CHECKING_BECAUSE_NO_SERVER -> showNoInternetLoginDialog()
                    FeedbackEvents.LoginActivityEvents.FINISHED -> finish()
                    else -> toast("unhandled feedback: ${it.type}")
                }
            }

            return root
        }
    }

    private fun DialogLoginBinding.showPasswordCannotBeEmptyError() {
        passwordLayout.error = activity?.getString(R.string.password_cannot_be_empty) ?: "WRONG"
        passwordEditText.requestFocus()
    }

    private fun DialogLoginBinding.showUsernameCannotBeEmptyError() {
        usernameLayout.error = activity?.getString(R.string.username_cannot_be_empty) ?: "WRONG"
        usernameEditText.requestFocus()
    }

    private fun DialogLoginBinding.showPasswordIncorrectError() {
        passwordLayout.error = activity?.getString(R.string.wrong_username_password) ?: "WRONG"
        passwordEditText.requestFocus()
    }


    private fun showNoInternetCreateNewDialog(){
        activity?.let {
            JoozdlogAlertDialog(it).apply{
                titleResource = R.string.no_internet
                messageResource = R.string.no_internet_create_account
                setPositiveButton(android.R.string.ok)
            }.show()
        }
    }

    private fun showNoInternetLoginDialog(){
        activity?.let {
            JoozdlogAlertDialog(it).apply{
                titleResource = R.string.no_internet
                messageResource = R.string.no_internet_login
                setPositiveButton(android.R.string.ok){
                    finish()
                }
            }.show()
        }
    }

    private fun finish() = closeFragment()

}