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

package nl.joozd.logbookapp.core.usermanagement

import kotlinx.coroutines.withContext
import nl.joozd.logbookapp.R
import nl.joozd.logbookapp.core.App
import nl.joozd.logbookapp.core.Constants
import nl.joozd.logbookapp.core.TaskFlags
import nl.joozd.logbookapp.comm.*
import nl.joozd.logbookapp.core.messages.MessagesWaiting
import nl.joozd.logbookapp.data.sharedPrefs.Prefs
import nl.joozd.logbookapp.data.sharedPrefs.ServerPrefs
import nl.joozd.logbookapp.data.sharedPrefs.TaskPayloads
import nl.joozd.logbookapp.utils.DispatcherProvider
import nl.joozd.logbookapp.utils.generateKey

/*
 * UserManagement does NOT take care of setting TaskFlags that need to be (re-)set, other than any other function can.
 *  (can ask for something to be done, cannot mark a task as incomplete or finished)
 * Flags are the business of TaskManager and workers. This only provides entries for the rest of the program to set something in motion.
 * It is allowed send messages to MessageCentre in case user needs to be made aware of something, or needs to make a decision for a real-time function.
 */
//TODO move all functions that get called as the result of a TaskFlag being set to Workers.
// Functions here are to be replaced by just setting TaskFlag. Currently this work would be done in two places, which violates DRY, or leads to opaque back-and-forth-ing.
class UserManagement(private val taskFlags: TaskFlags = TaskFlags) {
    /**
     * Schedule the creation of a new user account.
     */
    fun createNewUser(){
        taskFlags.createNewUser(true)
    }

    //blocking IO in DispatcherProvider.io()
    suspend fun storeNewLoginData(username: String, key: ByteArray) = withContext(DispatcherProvider.io()){
        Prefs.username = username
        Prefs.key = key
        Prefs.lastUpdateTime = -1
    }

    //blocking IO in DispatcherProvider.io()
    suspend fun storeNewLoginData(username: String, keyString: String) = withContext(DispatcherProvider.io()){
        Prefs.username = username
        Prefs.keyString = keyString
        Prefs.lastUpdateTime = -1
    }

    //requesting a verification email is done by just re-submitting current email address.
    fun requestEmailVerificationMail(){
        taskFlags.updateEmailWithServer(true)
    }

    fun requestLoginLink(){
        taskFlags.sendLoginLink(true)
    }

    /**
     * Change email address. It will confirm with server at the first possible time. Server will send a confirmation mail if needed.
     * If newEmailAddress is null, it will re-confirm stored address with server.
     * If no connection it will schedule sending to server when internet gets available.
     * @param newEmailAddress - the email address to store. This is NOT checked to see if it is a valid email address.
     */
    fun changeEmailAddress(newEmailAddress: String) {
        ServerPrefs.emailAddress = newEmailAddress
        requestEmailVerificationMail()
    }

    /**
     * Change password. First checks if login credentials correct,
     * This function is executed right away, as a scheduled password change might lead to unwanted and/or unforeseen consequences.
     * @return true if password changed; new password will be stored.
     * @return false if password not changed due to server refusal (eg bad login data, which is handled by cloud) or no connection.
     * Calling function gets to handle message to user about success or failure. Consider coercing user to save new login link in that message.
     */
    suspend fun changeLoginKey(cloud: Cloud = Cloud()): Boolean =
        UsernameWithKey.fromPrefs()?.let{ lp ->
            val newKey = generateKey()
            val result = cloud.changeLoginKey(lp.username, lp.key, newKey)
            if (result == CloudFunctionResult.OK)
                withContext(DispatcherProvider.io()) { Prefs.key = newKey }
            return result == CloudFunctionResult.OK
        } ?: false


    fun logOut() {
        Prefs.useCloud(false)
        Prefs.username = null
        Prefs.keyString = null
        Prefs.lastUpdateTime = -1

    }

    fun generateLoginLink(): String? = Prefs.username?.let { username ->
        "${Constants.JOOZDLOG_LOGIN_LINK_PREFIX}$username:${Prefs.keyString?.replace('/', '-')}"
    }

    fun generateLoginLinkMessage(): String? = generateLoginLink()?.let { link ->
         App.instance.resources.openRawResource(R.raw.joozdlog_login_link_email)
             .reader()
             .readText()
             .replace(EMAIL_LINK_PLACEHOLDER, link)
        }

    /**
     * Confirm email confirmation string with server, or schedule that to happen as soon as server is online
     * @param confirmationString    the confirmation string as should have been received in confirmation email.
     *                              This will be sent to the server to verify.
     */
    suspend fun confirmEmail(confirmationString: String) {
        if (checkConfirmationString(confirmationString))
            withContext(DispatcherProvider.io()){
                TaskPayloads.emailConfirmationStringWaiting(confirmationString)
                TaskFlags.verifyEmailCode(true)
            }
        else MessagesWaiting.postBadVerificationCodeClicked(true)
    }

    fun invalidateEmail(){
        ServerPrefs.postEmailAddress("")
        ServerPrefs.postEmailVerified(false)
        Prefs.postBackupFromCloud(false)
    }

    /**
     * Use this for functions that need to be logged in.
     * If you just want to know, and not have any consequences from not being logged in, use [isLoggedIn]
     * @return true if logged in, false if not.
     * False will also cause messages to user to be generated.
     */
    suspend fun checkIfLoginDataSet(): Boolean =
        isLoggedIn().also {
            if (!it)
                notifyNoUserDataSet()
        }

    /**
     * True if logged in, false if not. For purposes that need to be logged in or else handle that, use [checkIfLoginDataSet]
     */
    suspend fun isLoggedIn() = Prefs.username() != null && Prefs.key() != null

    private fun makeLoginPassPair(loginPassString: String): Pair<String, String> =
        loginPassString.replace('-', '/').split(":").let { lp ->
            lp.first() to lp.last()
        }

    private fun notifyNoUserDataSet(){
        TODO("Notify user no login data is set")
    }

    companion object {
        private const val EMAIL_LINK_PLACEHOLDER = "[INSERT_LINK_HERE]"
    }
}


