/*
 *  JoozdLog Pilot's Logbook
 *  Copyright (c) 2020-2022 Joost Welle
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

import android.app.AlertDialog
import android.os.Bundle
import android.view.View
import androidx.lifecycle.lifecycleScope
import kotlinx.coroutines.launch
import nl.joozd.logbookapp.R
import nl.joozd.logbookapp.core.emailFunctions.EmailCenter
import nl.joozd.logbookapp.databinding.ActivityChangePasswordBinding
import nl.joozd.logbookapp.ui.utils.JoozdlogActivity
import nl.joozd.logbookapp.ui.utils.longToast


/**
 * Activity to change a users password. If a password is still stored, you do not need to confirm old password.
 * This way you can use this to change password in case of forgotten pass (ie. if you have a recovery link)
 */
class ChangePasswordActivity : JoozdlogActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setTheme(R.style.AppTheme)
        with (ActivityChangePasswordBinding.inflate(layoutInflater)) {
            setSupportActionBarWithReturn(changePasswordActivityToolbar)?.apply{
                setDisplayShowHomeEnabled(true)
                setDisplayHomeAsUpEnabled(true)
                title = getString(R.string.change_password)
            }

            resetSubmitButton()

            /****************************************************************************************
             * Observers
             ***************************************************************************************/

            setContentView(root)
        }
    }

    private fun ActivityChangePasswordBinding.resetSubmitButton() {
        submitButtonLoadingSpinner.visibility = View.GONE
        with(submitButton){
            setText(R.string.change_password)
            isEnabled = true
            setOnClickListener {
                changePasswordAndUpdateButton()
            }
        }
    }

    private fun ActivityChangePasswordBinding.makeSubmitButtonShowLoading(){
        submitButtonLoadingSpinner.visibility = View.VISIBLE
        with(submitButton) {
            text = ""
            isEnabled = false
        }
    }

    private fun ActivityChangePasswordBinding.makeSubmitButtonRecentlyChanged(){
        submitButtonLoadingSpinner.visibility = View.GONE
        with(submitButton) {
            setText(R.string.change_password)
            isEnabled = true
            setOnClickListener {
                showRecentlyChangedDialog()
            }
        }
    }

    private fun ActivityChangePasswordBinding.changePasswordAndUpdateButton() {
        makeSubmitButtonShowLoading()
        lifecycleScope.launch {
            if (EmailCenter().changeLoginKey()) {
                showLoginLinkChangedDialog()
                makeSubmitButtonRecentlyChanged()
            }
            else resetSubmitButton()
        }
    }



    private fun ActivityChangePasswordBinding.showRecentlyChangedDialog() {
        AlertDialog.Builder(activity).apply {
            setTitle(R.string.key_recently_changed)
            setMessage(R.string.key_recently_changed_message)
            setPositiveButton(android.R.string.ok) { _, _ ->
                changePasswordAndUpdateButton()
            }
            setNegativeButton(android.R.string.cancel) { _, _ -> }
        }.create().show()
    }

    private fun showLoginLinkChangedDialog(){
        AlertDialog.Builder(activity).apply{
            setTitle(R.string.changed_login_link)
            setMessage(R.string.changed_login_link_message)
            setPositiveButton(android.R.string.ok) { _, _ ->
                lifecycleScope.launch {
                    EmailCenter().generateLoginLinkMessage()?.let {
                        sendMessageToOtherApp(it, getString(R.string.login_link_title))
                    } ?: longToast(R.string.change_pass_error_1)
                }
            }
        }
    }
}