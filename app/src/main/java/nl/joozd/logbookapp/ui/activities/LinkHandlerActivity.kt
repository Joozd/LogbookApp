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
import android.content.Intent.ACTION_VIEW
import android.net.Uri
import android.os.Bundle
import androidx.lifecycle.lifecycleScope
import kotlinx.coroutines.launch
import nl.joozd.logbookapp.R
import nl.joozd.logbookapp.data.comm.Cloud
import nl.joozd.logbookapp.data.comm.CloudFunctionResults
import nl.joozd.logbookapp.data.comm.UserManagement
import nl.joozd.logbookapp.data.sharedPrefs.Preferences
import nl.joozd.logbookapp.databinding.ActivityLinkHandlerBinding
import nl.joozd.logbookapp.ui.utils.JoozdlogActivity
import nl.joozd.logbookapp.workmanager.JoozdlogWorkersHub

/**
 * This activity handles people getting sent to app through links.
 * links supported at time of this writing:
 * - login
 * - check email
 */
class LinkHandlerActivity : JoozdlogActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        ActivityLinkHandlerBinding.inflate(layoutInflater).apply{
            handleIntent()
            setContentView(root)
        }
    }


    private fun handleIntent() {
        println("LOOK FOR THIS")
        //if action != ACTION_VIEW we don't want it.
        if (intent?.action != ACTION_VIEW) return

        // If no data in intent we can't use it.
        val data: Uri = intent?.data ?: return
        println("HANDLING $data BOOMSTAM")

        when(data.pathSegments?.firstOrNull()){
            LOGIN_LINK_PATH -> handleLoginLink(data)
            VERIFY_EMAIL_PATH -> handleEmailVerificationLink(data)
            else -> showUnknownLinkMessage()
        }
    }

    private fun handleEmailVerificationLink(data: Uri) {
        println("CheckPoint 1")
        data.lastPathSegment?.replace("-", "/")?.let {
            lifecycleScope.launch {
                if (UserManagement.confirmEmail(it)) {
                    Cloud.requestLoginLinkMail()
                }
            }
        }
    }

    private fun handleLoginLink(data: Uri) {
        data.lastPathSegment?.let { lpString ->
            println("CheckPoint 2: $lpString")
            //TODO needs sanity check
            lifecycleScope.launch {
                val result = UserManagement.loginFromLink(makeLoginPassPair(lpString))
                println("CheckPoint 3: result: $result")
                if (result in CloudFunctionResults.connectionErrors) {
                    Preferences.loginLinkStringWaiting = lpString
                    JoozdlogWorkersHub.scheduleLoginAttempt()
                    showNoConnectionDialog()
                } else {
                    closeAndStartMainActivity() // mainActivity will sync on resume
                }
            }
        }
    }

    private fun makeLoginPassPair(loginPassString: String): Pair<String, String> =
        loginPassString.replace('-', '/').split(":").let { lp ->
            lp.first() to lp.last()
        }


    private fun showUnknownLinkMessage() = AlertDialog.Builder(this).apply{
        setTitle(R.string.unknown_link_title)
        setTitle(R.string.unknown_link_message)
        setPositiveButton(android.R.string.ok){ _, _ ->
            closeAndStartMainActivity()
        }
    }

    private fun showNoConnectionDialog() = AlertDialog.Builder(this).apply{
        setTitle(R.string.server_problem)
        setTitle(R.string.no_internet_login)
        setPositiveButton(android.R.string.ok){ _, _ ->
            closeAndStartMainActivity()
        }
    }

    companion object{
        private const val VERIFY_EMAIL_PATH = "verify-email"
        private const val LOGIN_LINK_PATH ="inject-key"
    }
}