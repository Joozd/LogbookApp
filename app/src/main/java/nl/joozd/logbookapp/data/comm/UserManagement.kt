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

package nl.joozd.logbookapp.data.comm

import android.content.Intent
import android.util.Log
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import nl.joozd.logbookapp.App
import nl.joozd.logbookapp.R
import nl.joozd.logbookapp.data.sharedPrefs.Prefs
import nl.joozd.logbookapp.data.sharedPrefs.errors.Errors
import nl.joozd.logbookapp.data.sharedPrefs.errors.ScheduledErrors
import nl.joozd.logbookapp.extensions.nullIfBlank
import nl.joozd.logbookapp.utils.generatePassword
import nl.joozd.logbookapp.workmanager.JoozdlogWorkersHub

object UserManagement {
    val signedIn: Boolean
        get() = Prefs.username != null && Errors.LOGIN_DATA_REJECTED_BY_SERVER !in ScheduledErrors.currentErrors

    val username
        get() = Prefs.username

    /**
     * Will create a new user next time flights are synchronized.
     * Doesn't change whether cloud is used (if it is not in use, no user will be created on server)
     * Actual new user creation is triggered by [nl.joozd.logbookapp.workmanager.SyncFlightsWorker]
     */
    fun newUser(){
        Prefs.username = null
    }

    /**
     * Create a new user on server
     * This will definitely invalidate current login data
     *
     * If user successfully created, it will set email if one is entered in [Prefs.emailAddress]
     *
     * @return @see [Cloud.createNewUser]
     */
    private suspend fun createNewUser(username: String, password: String): CloudFunctionResults = withContext (Dispatchers.IO){
        Prefs.username = username
        Prefs.password = password
        val email = Prefs.emailAddress.nullIfBlank()
        return@withContext Cloud.createNewUser(username, Prefs.key!!).also {
            Log.d("CreateNewUser()", "Created new user with key ${Prefs.key?.toList()}")
            if (it == CloudFunctionResults.OK) {
                //if email set, send it to server
                email?.let {
                    Cloud.sendNewEmailAddress()
                    //TODO handle replies from server: OK / NOT_A_VALID_EMAIL_ADDRESS / SERVER_ERROR / UNKNOWN_USER_OR_PASS / NO_LOGIN_DATA
                    Log.d("CreateNewUser()", "Added email: $email")
                }
                Prefs.lastUpdateTime = -1
                Prefs.useCloud = true
                Log.d("CreateNewUser()", "created username: $username, password: $password, email: $email")
                Log.d("CreateNewUser()", "check: ${Prefs.username}, password: ${Prefs.password}")
            } else {
                Log.d("CreateNewUser()", "Cloud.createNewUser returned $it")
                Prefs.username = Prefs.USERNAME_NOT_SET
                Prefs.password = null
            }
        }
    }

    /**
     * Call this when a new Cloud username/password is needed.
     * - Asks a username from server
     * - generates a password
     * - creates a new user on server through [createNewUser]
     * In the very unlikely scenario of username being generated twice the same and other user who had this was faster, retry recursively
     * @return [CloudFunctionResults.NO_INTERNET] if no internet connection, else @see [Cloud.createNewUser]
     */
    suspend fun newLoginDataNeeded(): CloudFunctionResults = withContext (Dispatchers.IO){
        if (InternetStatus.internetAvailable == false) return@withContext CloudFunctionResults.NO_INTERNET
        val newUsername = Cloud.requestUsername() ?: return@withContext CloudFunctionResults.CLIENT_ERROR
        val password = generatePassword(16)
        Log.d("UserManagement", "username: $newUsername")
        Log.d("UserManagement", "password: $password")
        return@withContext createNewUser(newUsername, password).let{
            if (it == CloudFunctionResults.USER_ALREADY_EXISTS) newLoginDataNeeded() else {
                Prefs.emailVerified = false
                if (Prefs.emailAddress.isNotBlank())
                    //We can wait for this, it is already a background process and there is no hurry
                    changeEmailAddress()
                it
            }
        }
    }


    /**
     * Change password. First checks if login credentials correct,
     * Then, saves new password, and changes that on server.
     * If anything goes wrong between saving new pass and setting it on server, user should be able to log in with previous loginlink.
     */
    suspend fun changePassword(newPassword: String): CloudFunctionResults = withContext (Dispatchers.IO){
        // Check if username/pass set
        Prefs.username ?: return@withContext CloudFunctionResults.NO_LOGIN_DATA
        Prefs.password ?: return@withContext CloudFunctionResults.NO_LOGIN_DATA

        Cloud.checkUser().let {
            if (!it.isOK())
                return@withContext it
        }
        Prefs.newPassword = newPassword
        return@withContext Cloud.changePassword(newPassword, email = Prefs.emailAddress.nullIfBlank()).also{
            if (it.isOK()) {
                Prefs.password = newPassword
                Prefs.newPassword = ""
                CloudFunctionResults.OK
            }
        }
    }


    /**
     * Change email address. It will confirm with server at the first possible time. Server will send a confirmation mail if needed.
     * @return true if sent to server, false if failed due to bad data, or no connection.
     * If no connection it will schedule sending to server when internet gets available
     */
    suspend fun changeEmailAddress(newEmailAddress: String = Prefs.emailAddress): Boolean = withContext (Dispatchers.IO){
        Prefs.emailAddress = newEmailAddress
        //don't do anything if no data entered and return false
        return@withContext if (newEmailAddress.isBlank()) false
        else when (Cloud.sendNewEmailAddress()) {
            // If it works right away, good for us!
            CloudFunctionResults.OK -> {
                true
            }
            // No connection, schedule it
            CloudFunctionResults.CLIENT_ERROR -> {
                JoozdlogWorkersHub.scheduleSetEmail()
                false
            }
            CloudFunctionResults.UNKNOWN_USER_OR_PASS -> {
                ScheduledErrors.addError(Errors.LOGIN_DATA_REJECTED_BY_SERVER)
                false
            }
            CloudFunctionResults.NOT_A_VALID_EMAIL_ADDRESS -> {
                ScheduledErrors.addError(Errors.BAD_EMAIL_SAVED)
                false
            }
            CloudFunctionResults.UNKNOWN_REPLY_FROM_SERVER -> {
                ScheduledErrors.addError(Errors.SERVER_ERROR)
                false
            }
            else -> {
                Log.w("confirmEmail", "Received unhandled response")
                false
            }
        }
    }

    /**
     * Try to fix a login if app crashed during password change (detected by Preferences.newPassword not being empty)
     */
    suspend fun tryToFixLogin(): Boolean? = withContext (Dispatchers.IO) {
        Prefs.newPassword.nullIfBlank()?.let {
            Prefs.password?.let { pw ->
                when (Cloud.checkUser(Prefs.username ?: return@withContext false, pw)) {
                    CloudFunctionResults.OK -> {
                        Log.w("tryToFixLogin", "Login was already correct")
                        return@withContext true
                    }
                    in CloudFunctionResults.connectionErrors -> return@withContext null // other problem, don't do anything
                    else -> Unit
                }
            }
            //If we get here, either Preferences.password == null or it is incorrect for login, and newPassword is not blank

            when (Cloud.checkUser(Prefs.username ?: return@withContext false, Prefs.newPassword)) {
                CloudFunctionResults.OK -> { // yay this fixed it
                    Prefs.password = Prefs.newPassword
                    Prefs.newPassword = ""
                    true
                }
                in CloudFunctionResults.connectionErrors -> null // Server didn't respond well. Don't do anything.
                CloudFunctionResults.UNKNOWN_USER_OR_PASS -> { // Didn't fix it, just wrong login credentials (which is wierd as newPassword wasn't empty)
                    Prefs.newPassword = ""
                    Prefs.username = null  //this will start the creation of a new account on the next sync
                    false
                }
                else -> null // something else went wrong. Doing nothing for now, maybe force a new creation here as well?
            }
        } ?: false.also { Log.w("tryToFixLogin", "Don't run this when Preferences.newPassword is blank") }
    }

    /**
     * check username/password with server and store them if OK
     * @return [CloudFunctionResults]:
     *  [CloudFunctionResults.OK] if logged in OK
     *  [CloudFunctionResults.UNKNOWN_USER_OR_PASS] if server rejected login data. In this case, an error to be shown to user will be scheduled through [ScheduledErrors.addError]
     *  [CloudFunctionResults.CLIENT_ERROR] if Client got an error (eg. died while receiving data)
     *  [CloudFunctionResults.CLIENT_NOT_ALIVE] if Client died
     *  [CloudFunctionResults.UNKNOWN_REPLY_FROM_SERVER] if server sent an unknown reply
     */
    suspend fun loginFromLink(loginPassPair: Pair<String, String>): CloudFunctionResults = withContext (Dispatchers.IO) {
        return@withContext Cloud.checkUserFromLink(loginPassPair.first, loginPassPair.second).also {
            if (it.isOK()) {
                Prefs.username = loginPassPair.first
                Prefs.forcePassword(loginPassPair.second)
                Prefs.lastUpdateTime = -1
                Prefs.useCloud = true
            }
        }
    }

    fun signOut() {
        Prefs.username = null
        Prefs.password = null
        Prefs.lastUpdateTime = -1
        Prefs.useCloud = false
    }

    fun generateLoginLink(): String? = Prefs.username?.let {
        "https://joozdlog.joozd.nl/inject-key/$it:${Prefs.password?.replace('/', '-')}"
    }

    fun generateLoginLinkIntent(): Intent? = generateLoginLink()?.let { link ->
        Intent(Intent.ACTION_SEND).apply {
            val text = App.instance.resources.openRawResource(R.raw.joozdlog_login_link_email).reader().readText().replace(EMAIL_LINK_PLACEHOLDER, link)

            type = "text/plain"
            putExtra(Intent.EXTRA_SUBJECT, EMAIL_SUBJECT)
            putExtra(Intent.EXTRA_TEXT, text)
        }
    }

    /**
     * Confirm email confirmation string with server, or schedule that to happen as soon as server is online
     * In case of failure, it will set failure flags in [ScheduledErrors]
     * If the failure is that the server could not be reached, it will not set any errors but start a worker through [JoozdlogWorkersHub] which will recall this
     *      function when a network connection is available
     * @param confirmationString    the confirmation string as should have been received in confirmation email
     *                              This will be sent to the server to verify
     * @return true if success, false if fail.
     */
    suspend fun confirmEmail(confirmationString: String): Boolean =
        when (withContext (Dispatchers.IO) { Cloud.confirmEmail(confirmationString) }){
            CloudFunctionResults.OK -> {
                Prefs.emailVerified = true
                Cloud.sendPendingEmailJobs()
                true
            }
            CloudFunctionResults.CLIENT_ERROR -> {
                JoozdlogWorkersHub.scheduleEmailConfirmation()
                false
            }
            CloudFunctionResults.UNKNOWN_USER_OR_PASS -> {
                ScheduledErrors.addError(Errors.LOGIN_DATA_REJECTED_BY_SERVER)
                false
            }
            CloudFunctionResults.EMAIL_DOES_NOT_MATCH -> {
                ScheduledErrors.addError(Errors.EMAIL_CONFIRMATION_FAILED)
                false
            }
            CloudFunctionResults.UNKNOWN_REPLY_FROM_SERVER -> {
                ScheduledErrors.addError(Errors.SERVER_ERROR)
                false
            }
            else -> {
                Log.w("confirmEmail", "Received unhandled response")
                false
            }
        }


    private const val EMAIL_LINK_PLACEHOLDER = "[INSERT_LINK_HERE]"
    private const val EMAIL_SUBJECT = "JoozdLog Login Link"
}


