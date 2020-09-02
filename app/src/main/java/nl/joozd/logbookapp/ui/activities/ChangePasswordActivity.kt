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
import android.util.Log
import androidx.activity.viewModels
import androidx.fragment.app.commit
import nl.joozd.logbookapp.R
import nl.joozd.logbookapp.data.comm.UserManagement
import nl.joozd.logbookapp.data.sharedPrefs.Preferences
import nl.joozd.logbookapp.databinding.ActivityChangePasswordBinding
import nl.joozd.logbookapp.extensions.getStringWithMakeup
import nl.joozd.logbookapp.extensions.onTextChanged
import nl.joozd.logbookapp.model.feedbackEvents.FeedbackEvents.ChangePasswordEvents
import nl.joozd.logbookapp.model.viewmodels.activities.ChangePasswordActivityViewModel
import nl.joozd.logbookapp.ui.dialogs.LoginDialog
import nl.joozd.logbookapp.ui.dialogs.WaitingForSomethingDialog
import nl.joozd.logbookapp.ui.utils.customs.JoozdlogAlertDialog
import nl.joozd.logbookapp.ui.utils.longToast
import nl.joozd.logbookapp.ui.utils.toast


/**
 * Activity to change a users password. If a password is still stored, you do not need to confirm old password.
 * This way you can use this to change password in case of forgotten pass (ie. if you have a recovery link)
 */
class ChangePasswordActivity : JoozdlogActivity() {
    private val viewModel: ChangePasswordActivityViewModel by viewModels()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setTheme(R.style.AppTheme)
        with (ActivityChangePasswordBinding.inflate(layoutInflater)) {
            setSupportActionBarWithReturn(changePasswordActivityToolbar)?.apply{
                setDisplayShowHomeEnabled(true)
                setDisplayHomeAsUpEnabled(true)
                title = getString(R.string.change_password)
            }


            //launch login activity if not signed in
            signInIfNeeded()


            // do some visual things (colors, texts etc)
            youAreSignedInAsTextView.text = Preferences.username?.let { getStringWithMakeup(R.string.you_are_signed_in_as, it) } ?: getString(R.string.you_are_not_signed_in)
            submitButton.joozdLogSetBackgroundColor()


            /****************************************************************************************
             * Buttons, onFocusChangeds, etc
             ***************************************************************************************/

            passwordEditText.onTextChanged {
                passwordTextInputLayout.error = null
            }
            repeatPasswordEditText.onTextChanged {
                repeatPasswordTextInputLayout.error = null
            }

            submitButton.setOnClickListener {
                viewModel.submitClicked(passwordEditText.text.toString(), repeatPasswordEditText.text.toString())
            }

            passwordRequirementsText.setOnClickListener {
                showPasswordRequirements()
            }

            /****************************************************************************************
             * Observers
             ***************************************************************************************/

            //Only change password when online
            viewModel.online.observe(activity){
                checkInternet(it)
            }

            viewModel.feedbackEvent.observe(activity){
                when (it.getEvent()){
                    ChangePasswordEvents.FINISHED -> {
                        toast("Done!")
                        closeWaitingDialog()
                        finish()
                    }

                    ChangePasswordEvents.WAITING_FOR_SERVER -> showWaitingForServerLayout()

                    ChangePasswordEvents.NO_INTERNET -> showNoInternetError()
                    ChangePasswordEvents.PASSWORD_TOO_SHORT -> showPasswordCannotBeEmptyError()
                    ChangePasswordEvents.PASSWORDS_DO_NOT_MATCH -> showPasswordsDoNotMatchError()
                    ChangePasswordEvents.PASSWORD_DOES_NOT_MEET_STANDARDS -> showPasswordDoesNotMeetStandardsError()
                    ChangePasswordEvents.NOT_LOGGED_IN, ChangePasswordEvents.LOGIN_INCORRECT -> {
                        Log.d("Debug 3", "plekje 3")
                        closeWaitingDialog()
                        showIncorrectLoginError()
                    }
                    ChangePasswordEvents.SERVER_NOT_RESPONDING -> {
                        closeWaitingDialog()
                        showServerDownError()
                    }



                    else -> longToast("Unhandled event: ${it.type}")
                }
            }




            setContentView(root)
        }
    }


    /***********************************************************************************************
     * Private functions with output to UI (start dialogs, etc)
     ***********************************************************************************************/

    /**
     * Check if internet available, if not show dialog that closes activity
     */
    private fun checkInternet(internetAvailable: Boolean){
        if (!internetAvailable){
            JoozdlogAlertDialog(this).show(tag = NO_INTERNET_DIALOG_TAG){
                titleResource = R.string.no_internet
                messageResource = R.string.need_internet
                setPositiveButton(android.R.string.ok){
                    finish()
                }
            }
        }
        else{
            (supportFragmentManager.findFragmentByTag(NO_INTERNET_DIALOG_TAG) as JoozdlogAlertDialog?)?.dismiss()
        }
    }

    /**
     * Check if signed in, if not, show dialog that redirects to login, or closes activity
     */
    private fun signInIfNeeded(){
        if (!UserManagement.signedIn){
            JoozdlogAlertDialog(this).show{
                titleResource = R.string.you_are_not_signed_in
                messageResource = R.string.you_need_to_be_signed_in
                setPositiveButton(R.string.signIn){
                    supportFragmentManager.commit {
                        add(R.id.mainActivityLayout, LoginDialog())
                        addToBackStack(null)
                    }
                }
                setNegativeButton(android.R.string.cancel){
                    finish()
                }
            }
        }
    }

    /**
     * Show "waiting for server" dialog.
     * TODO make this cancelable
     */
    private fun showWaitingForServerLayout(){
        supportFragmentManager.commit {
            add(R.id.changePasswordBackgroundLayout, WaitingForSomethingDialog().apply{
                setDescription(R.string.waiting_for_server)
            }, WAITING_DIALOG_TAG)
        }
    }

    private fun closeWaitingDialog(){
        supportFragmentManager.findFragmentByTag(WAITING_DIALOG_TAG).let{
            (it as WaitingForSomethingDialog).done()
        }
    }

    /**
     * Show password requirements. At this moment just a static string. TODO Maybe generate something from the function that checks it? Or is that just too much fuzz?
     */
    private fun showPasswordRequirements() = JoozdlogAlertDialog(activity).show {
        titleResource = R.string.pass_requirements
        messageResource = R.string.password_requirements
        setPositiveButton(android.R.string.ok)
    }

    private fun ActivityChangePasswordBinding.showPasswordCannotBeEmptyError() {
        passwordTextInputLayout.error = activity.getString(R.string.password_cannot_be_empty)
        passwordEditText.requestFocus()
    }
    private fun ActivityChangePasswordBinding.showPasswordsDoNotMatchError(){
        repeatPasswordEditText.setText("")
        repeatPasswordTextInputLayout.error = activity.getString(R.string.passwords_do_not_match)
        repeatPasswordEditText.requestFocus()
    }

    private fun ActivityChangePasswordBinding.showPasswordDoesNotMeetStandardsError(){
        passwordEditText.setText("")
        repeatPasswordEditText.setText("")
        passwordTextInputLayout.error = activity.getString(R.string.password_does_not_meet_standards)
        passwordEditText.requestFocus()
    }

    /**
     * This should not happen as another dialog is already looking at that
     */
    private fun showNoInternetError() = JoozdlogAlertDialog(this).show{
        titleResource = R.string.no_internet
        messageResource = R.string.need_internet
        setPositiveButton(android.R.string.cancel){
            // do nothing
        }
    }

    private fun showServerDownError() = JoozdlogAlertDialog(this).show{
        titleResource = R.string.server_problem
        messageResource = R.string.server_problem_message
        setPositiveButton(android.R.string.cancel){
            // do nothing
        }
    }

    private fun showIncorrectLoginError() = JoozdlogAlertDialog(activity).show{
        titleResource = R.string.login_error
        messageResource = R.string.login_error_message
        setPositiveButton(R.string.login){
            supportFragmentManager.commit {
                add(R.id.changePasswordBackgroundLayout, LoginDialog())
                addToBackStack(null)
            }
        }
        setNegativeButton(android.R.string.cancel){
            finish()
        }

    }


    companion object{
        const val NO_INTERNET_DIALOG_TAG = "NO_INTERNET_DIALOG_TAG"
        const val WAITING_DIALOG_TAG = "WAITING_DIALOG_TAG"
    }
}