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
import kotlinx.coroutines.MainScope
import kotlinx.coroutines.launch
import nl.joozd.logbookapp.R
import nl.joozd.logbookapp.data.comm.*
import nl.joozd.logbookapp.data.sharedPrefs.Prefs
import nl.joozd.logbookapp.utils.generatePassword
import nl.joozd.logbookapp.data.sharedPrefs.EmailPrefs
import nl.joozd.logbookapp.utils.UserMessage

/*
 * UserManagement does NOT take care of setting TaskFlags that need to be (re-)set, other than any other function can.
 *  (can ask for something to be done, cannot mark a task as incomplete or finished)
 * Flags are the business of TaskManager and workers. This only provides functions.
 * It also might send messages to MessageCentre in case user needs to be made aware of something, or needs to make a decision.
 */
//TODO move all functions that get called as the result of a TaskFlag being set to Workers.
// Functions here are to be replaced by just setting TaskFlag. Currently this work would be done in two places, which violates DRY, or leads to opaque back-and-forth-ing.
class UserManagement(private val taskFlags: TaskFlags = TaskFlags) {
    /**
     * Schedule the creation of a new user account.
     */
    fun createNewUser(){
        taskFlags.postCreateNewUser(true)
    }

    fun requestEmailVerificationMail(){
        taskFlags.postRequestVerificationEmail(true)
    }


    /*
    Functions to be called from Workers
     */

    /**
     * Create a new user on server. Will verify email as well if one is set.
     * @return true if user created now, false it not due to any reason. (connection or server refused)
     */
    //TODO if this can only be used in a Worker, it belongs in that worker.


    /**
     * Change email address. It will confirm with server at the first possible time. Server will send a confirmation mail if needed.
     * If newEmailAddress is null, it will re-confirm stored address with server.
     * If no connection it will schedule sending to server when internet gets available.
     */
    fun changeEmailAddress(newEmailAddress: String?) {
        newEmailAddress?.let { EmailPrefs.emailAddress = it }
        requestEmailVerificationMail()
    }




    /**
     * check username/password with server and store them if OK.
     */
    suspend fun loginFromLink(loginLinkString: String): Boolean {
        val lpPair = makeLoginPassPair(loginLinkString)
        return when(cloud.checkLoginDataWithServer(lpPair.first, lpPair.second)){
            CloudFunctionResult.OK -> {
                storeNewLoginData(lpPair)
                TaskFlags.postUseLoginLink(false)
                true
            }
            CloudFunctionResult.SERVER_REFUSED -> {
                showBadLoginLinkMessage()
                TaskFlags.postUseLoginLink(false) // The data in login link was bad. It will be bad next time we try it too, so no use in rescheduling.
                false
            }
            CloudFunctionResult.CONNECTION_ERROR -> {
                showLoginLinkPostponedMessage()
                storeNewLoginData(lpPair)
                TaskFlags.postUseLoginLink(true)
                false
            }
        }
    }


    /*
     * Schedule email verification with server
     */
    suspend fun verifyEmailAddressWithServerIfSet() {
        if (EmailPrefs.emailAddress().isNotBlank())
            TaskFlags.postRequestVerificationEmail(true)
    }

    //login data from login link has base64 encoded key
    private fun storeNewLoginData(lpPair: Pair<String, String>) {
        Prefs.username = lpPair.first
        Prefs.postKeyString(lpPair.second)
        Prefs.lastUpdateTime = -1
        Prefs.useCloud = true
        requestEmailVerificationMail()
    }

    /**
     * Change password. First checks if login credentials correct,
     * Then, saves new password, and changes that on server.
     * If anything goes wrong between saving new pass and setting it on server,
     * user's account will be locked and they will probably get a notification
     * about making a new cloud account on next sync.
     */
    suspend fun changePassword(): Boolean =
        if (checkIfLoginDataSet()) {
            val newPassword = generatePassword(16)
            val result = if (cloud.changePassword(newPassword) == CloudFunctionResult.OK) {
                Prefs.keyString = newPassword
                true
            } else false
            TaskFlags.postChangePassword(!result)
            result // return
        }
        else {
            TaskFlags.postChangePassword(true) // will change password as soon as login data are actually set.
            false
        }

    fun signOut() {
        Prefs.username = null
        Prefs.keyString = null
        Prefs.lastUpdateTime = -1
        Prefs.useCloud = false
    }

    fun generateLoginLink(): String? = Prefs.username?.let {
        "https://joozdlog.joozd.nl/inject-key/$it:${Prefs.keyString?.replace('/', '-')}"
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
     * @param confirmationString    the confirmation string as should have been received in confirmation email
     *                              This will be sent to the server to verify.
     * @param fromActivity: If true, this will send a message to MessageCenter in case of postponement of confirmation, so user will get feedback of the result of clicking his "confirm email" link.
     * @return true if success, false if fail.
     */
    suspend fun confirmEmail(confirmationString: String, fromActivity: Boolean = true) {
        if (":" !in confirmationString) {
            showBadEmailConfirmationStringMessage()
            return
        }
        when(cloud.confirmEmail(confirmationString)){
            CloudFunctionResult.OK -> {
                TaskFlags.postVerifyEmailCode(false)
                EmailPrefs.postEmailConfirmationStringWaiting("")
                showEmailConfirmedMessage()
            }
            CloudFunctionResult.SERVER_REFUSED -> {
                // This doesn't remove flag, as doing that will prevent the message from being shown on next activity creation.
                showEmailConfirmationRejectedMessage()
            }
            CloudFunctionResult.CONNECTION_ERROR -> if (fromActivity){
                EmailPrefs.postEmailConfirmationStringWaiting(confirmationString)
                TaskFlags.postVerifyEmailCode(true)
                showEmailConfirmationPostponedMessage()
            }
        }
    }



    private fun showBadLoginLinkMessage(){
        val message = UserMessage.Builder().apply{
            titleResource = R.string.login_error
            descriptionResource = R.string.wrong_username_password
            setPositiveButton(android.R.string.ok){ }
        }.build()
        MessageCenter.pushMessage(message)
    }

    private fun showLoginLinkPostponedMessage(){
        val message = UserMessage.Builder().apply{
            titleResource = R.string.login_link
            descriptionResource = R.string.no_internet_login
            setPositiveButton(android.R.string.ok){ }
        }.build()
        MessageCenter.pushMessage(message)
    }

    private fun showBadEmailConfirmationStringMessage(){
        val message = UserMessage.Builder().apply{
            titleResource = R.string.verification_mail
            descriptionResource = R.string.email_verification_invalid_data
            setPositiveButton(android.R.string.ok){ }
        }.build()
        MessageCenter.pushMessage(message)
    }

    private fun showEmailConfirmedMessage(){
        val message = UserMessage.Builder().apply{
            titleResource = R.string.verification_mail
            descriptionResource = R.string.email_verified
            setPositiveButton(android.R.string.ok){ }
        }.build()
        MessageCenter.pushMessage(message)
    }

    private fun showEmailConfirmationRejectedMessage(){
        MessageCenter.MessageFragmentBuilder().commit{
            messageResource = R.string.email_address_rejected
            setPositiveButton(R.string.verify){
                cancelEmailCodeVerification()
                MainScope().launch { verifyEmailAddressWithServerIfSet() }
            }
            setNegativeButton(R.string.delete){
                cancelEmailCodeVerification()
                invalidateEmail()
            }
        }
    }

    private fun showEmailConfirmationPostponedMessage(){
        val message = UserMessage.Builder().apply{
            titleResource = R.string.verification_mail
            descriptionResource = R.string.email_verification_scheduled
            setPositiveButton(android.R.string.ok){ }
        }.build()
        MessageCenter.pushMessage(message)
    }

    private fun cancelEmailCodeVerification() {
        TaskFlags.postVerifyEmailCode(false)
        EmailPrefs.postEmailConfirmationStringWaiting("")
    }

    fun invalidateEmail(){
        EmailPrefs.postEmailAddress("")
        EmailPrefs.postEmailVerified(false)
        Prefs.postBackupFromCloud(false)
    }

    suspend fun checkIfLoginDataSet(): Boolean =
        (Prefs.username() != null && Prefs.key() != null).also {
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


