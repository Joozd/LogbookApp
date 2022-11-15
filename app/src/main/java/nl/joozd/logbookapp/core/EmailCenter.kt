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
 *  - Server keeps database of IDs with email hashes / salts / lastUsed / isVerified. New email also generates new ID.
 *      - This makes sure you can't get spammed by somebody else if your email is registered
 *          (your ID + email address is only known locally, your ID + email hash+salt on server so they can be matched)
 */
package nl.joozd.logbookapp.core

import androidx.core.text.isDigitsOnly
import kotlinx.coroutines.MainScope
import kotlinx.coroutines.launch
import nl.joozd.joozdlogcommon.EmailData
import nl.joozd.logbookapp.comm.migrateEmail
import nl.joozd.logbookapp.data.sharedPrefs.EmailPrefs
import nl.joozd.logbookapp.data.sharedPrefs.Prefs
import nl.joozd.logbookapp.data.sharedPrefs.TaskPayloads
import nl.joozd.logbookapp.ui.utils.base64Decode

/*
 * UserManagement does NOT take care of setting TaskFlags that need to be (re-)set, other than any other function can.
 *  (can ask for something to be done, cannot mark a task as incomplete or finished)
 * Flags are the business of TaskManager and workers. This only provides entries for the rest of the program to set something in motion.
 * It is allowed send messages to MessageCentre in case user needs to be made aware of something, or needs to make a decision for a real-time function.
 */
class EmailCenter(private val taskFlags: TaskFlags = TaskFlags, private val prefs: Prefs = Prefs, private val emailPrefs: EmailPrefs = EmailPrefs) {

    fun scheduleBackupEmail(){

        taskFlags.sendBackupEmail(true)
        TODO("""
            Check if email entered and verified, if not, schedule message through MessageCenter to take care of that (two options, not entered or not verified)            
            """)
    }

    //requesting a verification email is done by just re-submitting current email address.
    fun requestEmailVerificationMail(){
        taskFlags.updateEmailWithServer(true)
    }

    /**
     * Change email address. It will confirm with server at the first possible time. Server will send a confirmation mail if needed.
     * If no connection it will schedule sending to server when internet gets available.
     * @param newEmailAddress - the email address to store. This is NOT checked to see if it is a valid email address.
     */
    fun changeEmailAddress(newEmailAddress: String) {
        MainScope().launch {
            //sets are blocking to prevent race conditions with requestEmailVerificationMail()
            emailPrefs.emailVerified.setValue(false)
            emailPrefs.emailAddress.setValue(newEmailAddress)
            requestEmailVerificationMail()
        }
    }

    /**
     * Confirm email confirmation string with server, or schedule that to happen as soon as server is online
     * @param confirmationString    the confirmation string as should have been received in confirmation email.
     *                              This will be sent to the server to verify.
     */
    suspend fun confirmEmail(confirmationString: String): Boolean =
        verifyConfirmationString(confirmationString).also{
            if(it){
                MainScope().launch {
                    TaskPayloads.emailConfirmationStringWaiting.setValue(confirmationString) // Writing it this way will wait until it is saved before emailcode is verified
                    TaskFlags.verifyEmailCode(true)
                }
            }
        }


    fun invalidateEmail(){
        emailPrefs.emailAddress("")
        emailPrefs.emailVerified(false)
    }

    fun setEmailUnverified(){
        emailPrefs.emailVerified(false)
    }

    suspend fun migrateEmailDataIfNeeded(){
        val userName = prefs.username()
        val emailAddress = emailPrefs.emailAddress()
        val migrationNeeded =
            userName != null
                    && emailAddress.isNotBlank()
                    && emailPrefs.emailID() == EmailData.EMAIL_ID_NOT_SET
        if (migrationNeeded){
            migrateEmail(userName!!, emailAddress)?.let{
                Prefs.username(null)
                emailPrefs.emailID(it)
            }
        }
    }

    private fun verifyConfirmationString(confirmationString: String): Boolean =
        confirmationString.split(":").let{
            it.size == 2
                    && it.first().isDigitsOnly()
                    && it.last().isValidBase64Hash()
        }

    private fun String.isValidBase64Hash(): Boolean =
        try{
            val decoded = base64Decode(this.replace('-', '/')) // I replace '/' with  '-' for Base64 that ends up in a link.
            decoded.size == 32 // SHA256 hash is 32 bytes
        } catch(e: Exception){
            false
        }
}


