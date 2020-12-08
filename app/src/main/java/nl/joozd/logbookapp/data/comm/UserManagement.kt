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

package nl.joozd.logbookapp.data.comm

import android.content.Intent
import android.util.Log
import nl.joozd.logbookapp.App
import nl.joozd.logbookapp.R
import nl.joozd.logbookapp.data.comm.protocol.CloudFunctionResults
import nl.joozd.logbookapp.data.sharedPrefs.Preferences
import nl.joozd.logbookapp.extensions.nullIfBlank

object UserManagement {
    val signedIn: Boolean
        get() = Preferences.username != null

    val username
        get() = Preferences.username

    // This is used to check if loginActivity got returned to after a new user was just
    // successfully created and it can close right away
    var justCreatedNewUser: Boolean = false

    /**
     * Create a new user on server
     * @return true if success
     * @return false if username taken
     * @return null if server or connection error
     */
    suspend fun createNewUser(username: String, password: String, email: String? = null): CloudFunctionResults {
        return (if (email == null) Cloud.createNewUser(username.toLowerCase(), password) else Cloud.createNewUser(username, password, email)).also {
            if (it == CloudFunctionResults.OK) {
                Preferences.username = username
                Preferences.password = password
                Preferences.lastUpdateTime = -1
                Preferences.useCloud = true
                Log.d("CreateNewUser()", "created username: $username, password: $password, email: $email")
                Log.d("CreateNewUser()", "check: ${Preferences.username}, password: ${Preferences.password}")
            } else Log.d("CreateNewUser()", "Cloud.createNewUser returned $it")
        }
    }


    /**
     * Change password. First checks if login credentials correct,
     * Then, saves new password, and changes that on server.
     * If anything goes wrong between saving new pass and setting it on server, user should be able to log in with previous loginlink.
     */
    suspend fun changePassword(newPassword: String, email: String? = null): Int {
        // Check if username/pass set
        Preferences.username ?: return ReturnCodes.NO_USERNAME
        Preferences.password ?: return ReturnCodes.NO_PASSWORD

        if (Cloud.checkUser().also {Log.d("XXXXXX", "checkUser gave $it")}
            != true
        ) return ReturnCodes.WRONG_CREDENTIALS


        Preferences.newPassword = newPassword
        return when (Cloud.changePassword(newPassword, email).also { Log.d("changePassword()", "returned $it") }) {
            true -> {
                Preferences.password = newPassword
                Preferences.newPassword = ""
                ReturnCodes.SUCCESS
            }
            false -> ReturnCodes.WRONG_CREDENTIALS
            null -> ReturnCodes.CONNECTION_ERROR
        }
    }

    /**
     * Try to fix a login if app crashed during password change (detected by Preferences.newPassword not being empty)
     */
    suspend fun tryToFixLogin(): Boolean? =
        Preferences.newPassword.nullIfBlank()?.let{
            Preferences.password?.let { pw ->
                when (Cloud.checkUser(Preferences.username ?: return false, pw)) {
                    true -> {
                        Log.w("tryToFixLogin", "Login was already correct")
                        return true
                    }
                    null -> return null // other problem, don't do anything
                    false -> Unit
                }
            }
            //If we get here, either Preferences.password == null or it is incorrect for login, and newPassword is not blank

            when (Cloud.checkUser( Preferences.username ?: return false, Preferences.newPassword)){
                true -> { // yay this fixed it
                    Preferences.password = Preferences.newPassword
                    Preferences.newPassword = ""
                    true
                }
                false -> { // Didn't fix it, just wrong login credentials (which is wierd as newPassword wasn't empty)
                    Preferences.newPassword = ""
                    false
                }
                null -> null // Server didn't respond well. Don't do anything.
            }
        } ?: false.also { Log.w("tryToFixLogin", "Don't run this when Preferences.newPassword is blank") }

    /**
     * check username/password with server and store them if OK
     * @return true if success
     * @return false if username taken
     * @return null if server or connection error
     */
    suspend fun login(username: String, password: String): Boolean? {
        return Cloud.checkUser(username, password).also {
            if (it == true) {
                Preferences.username = username
                Preferences.password = password
                Preferences.useCloud = true
                Preferences.lastUpdateTime = -1
            }
        }
    }

    suspend fun loginFromLink(loginPassPair: Pair<String, String>): Boolean? {
        return Cloud.checkUserFromLink(loginPassPair.first, loginPassPair.second).also {
            if (it == true) {
                Preferences.username = loginPassPair.first
                Preferences.forcePassword(loginPassPair.second)
                Preferences.lastUpdateTime = -1
                Preferences.useCloud = true
            }
        }
    }

    fun signOut() {
        Preferences.username = null
        Preferences.password = null
        Preferences.lastUpdateTime = -1
        Preferences.useCloud = false
    }

    fun generateLoginLink(): String? = Preferences.username?.let {
        "https://joozdlog.joozd.nl/inject-key/$it:${Preferences.password}"
    }

    fun generateLoginLinkIntent(): Intent? = generateLoginLink()?.let { link ->
        Intent(Intent.ACTION_SEND).apply {
            val text = App.instance.resources.openRawResource(R.raw.joozdlog_login_link_email).reader().readText().replace(EMAIL_LINK_PLACEHOLDER, link)

            type = "text/plain"
            putExtra(Intent.EXTRA_SUBJECT, EMAIL_SUBJECT)
            putExtra(Intent.EXTRA_TEXT, text)
        }
    }

    object ReturnCodes {
        const val SUCCESS = 0
        const val CONNECTION_ERROR = -999
        const val WRONG_CREDENTIALS = 1
        const val NO_USERNAME = 2
        const val NO_PASSWORD = 3
    }

    private const val EMAIL_LINK_PLACEHOLDER = "[INSERT_LINK_HERE]"
    private const val EMAIL_SUBJECT = "JoozdLog Login Link"
}


