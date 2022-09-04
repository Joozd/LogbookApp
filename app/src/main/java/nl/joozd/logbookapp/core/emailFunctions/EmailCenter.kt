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

/* TODO The switch from cloud to email forwarding backups needs some thinking and designing.
 * As I want intermediate versions to work, I am keeping some Cloud parts up for now. They get to be deleted when I know how to do stuff
 * Proposition:
 *  - Server keeps list of IDs with email hashes. New email also generates new ID.
 *      - This makes sure you can't get spammed by somebody else if your email is registered (your ID + email address is only known locally, your ID + email hash on server so they can be matched)
 */
package nl.joozd.logbookapp.core.emailFunctions

import kotlinx.coroutines.withContext
import nl.joozd.logbookapp.R
import nl.joozd.logbookapp.core.App
import nl.joozd.logbookapp.core.Constants
import nl.joozd.logbookapp.core.TaskFlags
import nl.joozd.logbookapp.comm.*
import nl.joozd.logbookapp.core.messages.MessagesWaiting
import nl.joozd.logbookapp.data.sharedPrefs.Prefs
import nl.joozd.logbookapp.data.sharedPrefs.TaskPayloads
import nl.joozd.logbookapp.data.sharedPrefs.toggle
import nl.joozd.logbookapp.ui.utils.base64Encode
import nl.joozd.logbookapp.utils.DispatcherProvider
import nl.joozd.logbookapp.utils.generateKey

/*
 * UserManagement does NOT take care of setting TaskFlags that need to be (re-)set, other than any other function can.
 *  (can ask for something to be done, cannot mark a task as incomplete or finished)
 * Flags are the business of TaskManager and workers. This only provides entries for the rest of the program to set something in motion.
 * It is allowed send messages to MessageCentre in case user needs to be made aware of something, or needs to make a decision for a real-time function.
 */
class EmailCenter(private val taskFlags: TaskFlags = TaskFlags) {
    suspend fun toggleCloudOrCreateNewUser(){
            if(Prefs.useCloud() || isLoggedIn())
                Prefs.useCloud.toggle()
            else TaskFlags.createNewUserAndEnableCloud.toggle()
    }

    suspend fun setCloudOrCreateNewUser(value: Boolean){
        if(isLoggedIn())
            Prefs.useCloud(value)
        else TaskFlags.createNewUserAndEnableCloud(value)
    }

    fun storeNewLoginData(username: String, keyString: String) {
        Prefs.username(username)
        Prefs.keyString(keyString)
    }

    fun storeNewLoginData(username: String, key: ByteArray) {
        Prefs.username(username)
        Prefs.key(key)
    }

    //requesting a verification email is done by just re-submitting current email address.
    fun requestEmailVerificationMail(){
        taskFlags.updateEmailWithServer(true)
    }

    /**
     * Change email address. It will confirm with server at the first possible time. Server will send a confirmation mail if needed.
     * If newEmailAddress is null, it will re-confirm stored address with server.
     * If no connection it will schedule sending to server when internet gets available.
     * @param newEmailAddress - the email address to store. This is NOT checked to see if it is a valid email address.
     */
    suspend fun changeEmailAddress(newEmailAddress: String) {
        ServerPrefs.emailVerified(ServerPrefs.emailVerified() && newEmailAddress.equals(ServerPrefs.emailAddress(), ignoreCase = true))
        ServerPrefs.emailAddress(newEmailAddress)
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
        getUsernameWithKey()?.let{ lp ->
            val newKey = generateKey()
            val result = cloud.changeLoginKey(lp.username, lp.key, newKey)
            if (result == CloudFunctionResult.OK)
                Prefs.key(newKey)
            return result == CloudFunctionResult.OK
        } ?: false


    fun logOut() {
        Prefs.useCloud(false)
        Prefs.username(null)
        Prefs.keyString(null)
    }

    suspend fun generateLoginLink(): String? =
        getUsernameWithKey()?.let { uwk ->
            val username = uwk.username
            val keyString = keyToLinkableBase64String(uwk.key)

            Constants.JOOZDLOG_LOGIN_LINK_PREFIX + "$username:$keyString"
        }

    suspend fun generateLoginLinkMessage(): String? = generateLoginLink()?.let { link ->
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
        else MessagesWaiting.badVerificationCodeClicked(true)
    }

    fun invalidateEmail(){
        ServerPrefs.emailAddress("")
        ServerPrefs.emailVerified(false)
    }

    suspend fun getUsernameWithKey(): UsernameWithKey? {
        val n = Prefs.username()
        val k = Prefs.key()
        if (n == null || k == null) {
            Prefs.useCloud(false)
            notifyNoUserDataSet()
            return null
        }
        return UsernameWithKey(n, k)
    }

    suspend fun getUsername(): String? =
        Prefs.username()

    /**
     * Use this for functions that need to be logged in.
     * If you just want to know, and not have any consequences from not being logged in, use [isLoggedIn]
     * @return true if logged in, false if not.
     * False will also cause messages to user to be generated.
     */
    suspend fun checkIfLoginDataSet(): Boolean =
        getUsernameWithKey() != null


    /**
     * True if logged in, false if not. For purposes that need to be logged in or else handle that, use [checkIfLoginDataSet]
     */
    suspend fun isLoggedIn() = Prefs.username() != null && Prefs.key() != null

    private fun notifyNoUserDataSet(){
        MessagesWaiting.noLoginDataSaved(true)
    }


    private fun keyToLinkableBase64String(key: ByteArray) = base64Encode(key).replace('/', '-')

    companion object {
        private const val EMAIL_LINK_PLACEHOLDER = "[INSERT_LINK_HERE]"
    }
}


