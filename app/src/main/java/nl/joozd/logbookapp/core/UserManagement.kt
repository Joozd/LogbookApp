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

package nl.joozd.logbookapp.core

import android.content.Intent
import android.util.Log
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import nl.joozd.logbookapp.R
import nl.joozd.logbookapp.data.comm.*
import nl.joozd.logbookapp.data.sharedPrefs.Prefs
import nl.joozd.logbookapp.data.sharedPrefs.errors.Errors
import nl.joozd.logbookapp.extensions.nullIfBlank
import nl.joozd.logbookapp.utils.generatePassword
import nl.joozd.logbookapp.data.sharedPrefs.EmailPrefs
import nl.joozd.logbookapp.utils.UserMessage

object UserManagement {
    /**
     * To be called from TaskDispatcher
     */
    suspend fun createNewUser(){
        if(requestAndSaveLoginData()){
            /*
             * requestAndSaveLoginData just made username and key so they cannot be null
             */
            createNewUserOnServer(Prefs.username()!!, Prefs.key()!!)
            TaskFlags.createNewUser = false
        }

        /*
         * this will probably end up with a call to this function again.
         * However, Cloud should have taken care of the reasons for failure.
         */
        else TaskFlags.createNewUser = true
    }

    /*
     * This will invalidate current login data.
     * - Asks a username from server
     * - generates a password
     * - saves them to Prefs.
     * @return true if new data received and saved, false if connection or server error.
     */
    private suspend fun requestAndSaveLoginData(): Boolean{
        Prefs.username = Prefs.USERNAME_NOT_SET
        Prefs.password = null
        EmailPrefs.emailVerified = false

        Cloud().requestUsername()?.let{ n ->
            Prefs.username = n
            Prefs.password = generatePassword(16)
            return true
        }
        return false
    }

    /*
     * Create a new user on server
     * Does NOT check or save anything with Prefs.
     *
     * If user successfully created, it will send email if one is entered in [EmailPrefs.emailAddress]
     *
     * If not successful, will set [TaskFlags.createNewUser] to true. Reason for failure will be handled by Cloud.
     */
    private suspend fun createNewUserOnServer(username: String, key: ByteArray) {
        require(username.isNotBlank() && key.isNotEmpty()) { "Username($username) cannot be blank and key ($key) cannot be empty" }

        if (Cloud().createNewUser(username, key)) {
            //if email set, send it to server
            EmailPrefs.emailAddress().takeIf { it.isNotBlank() }?.let {
                Cloud().sendNewEmailAddress(username, key, it)
            }
            Prefs.lastUpdateTime = -1
            Prefs.useCloud = true
        }
        else TaskFlags.createNewUser = true
    }

    /**
     * Change email address. It will confirm with server at the first possible time. Server will send a confirmation mail if needed.
     * If newEmailAddress is null, it will re-confirm stored address with server.
     * If no connection it will schedule sending to server when internet gets available
     */
    suspend fun changeEmailAddress(newEmailAddress: String) {
        EmailPrefs.emailAddress = newEmailAddress
        verifyEmailPrefsAddressWithServer()
    }

    private suspend fun verifyEmailPrefsAddressWithServer(){
        EmailPrefs.emailAddress().takeIf{ it.isNotBlank() }?.let{ address ->
            val loginDataSet = checkIfLoginDataSet()
            if (loginDataSet)
                Cloud().sendNewEmailAddress(Prefs.username()!!, Prefs.key()!!, address)
            else TaskFlags.pushUpdateEmailWithServer(true) // this reschedules it, checkIfLoginDataSet() will have taken care of no login data problem
        }
    }

    /**
     * check username/password with server and store them if OK.
     * If not OK, Cloud will handle any problems.
     */
    suspend fun loginFromLink(loginLinkString: String) {
        val lpPair = makeLoginPassPair(loginLinkString)
        when(Cloud().checkLoginDataWithServer(lpPair.first, lpPair.second)){
            true -> {
                storeNewLoginData(lpPair)
            }
            false -> showBadLoginLinkMessage()
            null -> {
                showLoginLinkPostponedMessage()
                storeNewLoginData(lpPair)
            }
        }
    }

    private suspend fun storeNewLoginData(lpPair: Pair<String, String>) {
        Prefs.username = lpPair.first
        Prefs.setEncodedPassword(lpPair.second)
        Prefs.lastUpdateTime = -1
        Prefs.useCloud = true
        verifyEmailPrefsAddressWithServer()
    }

    /**
     * Change password. First checks if login credentials correct,
     * Then, saves new password, and changes that on server.
     * If anything goes wrong between saving new pass and setting it on server, user should be able to log in with previous loginlink.
     */
    suspend fun changePassword(newPassword: String): ServerFunctionResult = withContext (Dispatchers.IO){
        // Check if username/pass set
        Prefs.username ?: return@withContext ServerFunctionResult.NO_LOGIN_DATA
        Prefs.password ?: return@withContext ServerFunctionResult.NO_LOGIN_DATA

        OldCloud.checkUser().let {
            if (!it.isOK())
                return@withContext it
        }
        Prefs.newPassword = newPassword
        return@withContext OldCloud.changePassword(
            newPassword,
            email = EmailPrefs.emailAddress.nullIfBlank()
        ).also{
            if (it.isOK()) {
                Prefs.password = newPassword
                Prefs.newPassword = ""
                ServerFunctionResult.OK
            }
        }
    }

    /**
     * Try to fix a login if app crashed during password change (detected by Preferences.newPassword not being empty)
     */
    suspend fun tryToFixLogin(): Boolean? = withContext (Dispatchers.IO) {
        Prefs.newPassword.nullIfBlank()?.let {
            Prefs.password?.let { pw ->
                when (OldCloud.checkUser(Prefs.username ?: return@withContext false, pw)) {
                    ServerFunctionResult.OK -> {
                        Log.w("tryToFixLogin", "Login was already correct")
                        return@withContext true
                    }
                    in ServerFunctionResult.connectionErrors -> return@withContext null // other problem, don't do anything
                    else -> Unit
                }
            }
            //If we get here, either Preferences.password == null or it is incorrect for login, and newPassword is not blank

            when (OldCloud.checkUser(Prefs.username ?: return@withContext false, Prefs.newPassword)) {
                ServerFunctionResult.OK -> { // yay this fixed it
                    Prefs.password = Prefs.newPassword
                    Prefs.newPassword = ""
                    true
                }
                in ServerFunctionResult.connectionErrors -> null // Server didn't respond well. Don't do anything.
                ServerFunctionResult.UNKNOWN_USER_OR_PASS -> { // Didn't fix it, just wrong login credentials (which is wierd as newPassword wasn't empty)
                    Prefs.newPassword = ""
                    Prefs.username = null  //this will start the creation of a new account on the next sync
                    false
                }
                else -> null // something else went wrong. Doing nothing for now, maybe force a new creation here as well?
            }
        } ?: false.also { Log.w("tryToFixLogin", "Don't run this when Preferences.newPassword is blank") }
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
            val text = App.instance.resources.openRawResource(R.raw.joozdlog_login_link_email).reader().readText().replace(
                EMAIL_LINK_PLACEHOLDER, link)

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
        when (withContext (Dispatchers.IO) { OldCloud.confirmEmail(confirmationString) }){
            ServerFunctionResult.OK -> {
                EmailPrefs.emailVerified = true // TaskDispatcher will monitor this and take care of the rest
                true
            }
            ServerFunctionResult.CLIENT_ERROR -> {
                TaskFlags.verifyEmail = true
                EmailPrefs.emailConfirmationStringWaiting = confirmationString
                false
            }
            ServerFunctionResult.UNKNOWN_USER_OR_PASS -> {
                ScheduledErrors.addError(Errors.LOGIN_DATA_REJECTED_BY_SERVER)
                false
            }
            ServerFunctionResult.EMAIL_DOES_NOT_MATCH -> {
                ScheduledErrors.addError(Errors.EMAIL_CONFIRMATION_FAILED)
                false
            }
            ServerFunctionResult.UNKNOWN_REPLY_FROM_SERVER -> {
                ScheduledErrors.addError(Errors.SERVER_ERROR)
                false
            }
            else -> {
                Log.w("confirmEmail", "Received unhandled response")
                false
            }
        }

    private fun showBadLoginLinkMessage(){
        val message = UserMessage.Builder().apply{
            titleResource = R.string.login_error
            descriptionResource = R.string.wrong_username_password
            setPositiveButton(android.R.string.ok){

            }
        }.build()
        MessageCenter.pushMessage(message)
    }

    private fun showLoginLinkPostponedMessage(){
        val message = UserMessage.Builder().apply{
            titleResource = R.string.login_link
            descriptionResource = R.string.no_internet_login
            setPositiveButton(android.R.string.ok){

            }
        }.build()
        MessageCenter.pushMessage(message)
    }

    private suspend fun signedIn(): Boolean =
        Prefs.username() != null && Prefs.key() != null

    private fun handleBadEmailAddressSent(){

    }

    fun invalidateEmail(){
        EmailPrefs.postEmailAdress("")
        EmailPrefs.postEmailVerified(false)
        Prefs.postBackupFromCloud(false)
    }

    private suspend fun checkIfLoginDataSet(): Boolean =
        signedIn().also {
            if (!it)
                notifyNoUserDataSet()
        }

    private fun makeLoginPassPair(loginPassString: String): Pair<String, String> =
        loginPassString.replace('-', '/').split(":").let { lp ->
            lp.first() to lp.last()
        }



    private fun notifyNoUserDataSet(){
        TODO("Notify user no login data is set")
    }

    private const val EMAIL_LINK_PLACEHOLDER = "[INSERT_LINK_HERE]"
    private const val EMAIL_SUBJECT = "JoozdLog Login Link"
}


