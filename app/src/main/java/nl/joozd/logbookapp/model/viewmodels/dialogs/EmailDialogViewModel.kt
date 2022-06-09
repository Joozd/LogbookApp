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

package nl.joozd.logbookapp.model.viewmodels.dialogs

import androidx.lifecycle.viewModelScope
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.launch
import nl.joozd.logbookapp.R
import nl.joozd.logbookapp.core.usermanagement.UserManagement
import nl.joozd.logbookapp.data.sharedPrefs.ServerPrefs
import nl.joozd.logbookapp.model.viewmodels.JoozdlogDialogViewModel
import nl.joozd.logbookapp.model.viewmodels.status.EmailDialogStatus
import nl.joozd.logbookapp.model.viewmodels.status.EmailDialogStatus.EmailDialogStatusError.*
import nl.joozd.logbookapp.utils.CastFlowToMutableFlowShortcut
import java.util.*

class EmailDialogViewModel: JoozdlogDialogViewModel() {
    //intentionally public
    var onComplete: () -> Unit = {}

    val statusFlow: StateFlow<EmailDialogStatus?> = MutableStateFlow(null)
    private var status by CastFlowToMutableFlowShortcut(statusFlow)

    val email1Flow: StateFlow<String> = MutableStateFlow("").apply{ viewModelScope.launch { value = ServerPrefs.emailAddress() } }
    val email2Flow: StateFlow<String> = MutableStateFlow("").apply{ viewModelScope.launch { value = ServerPrefs.emailAddress() } }

    var email1 by CastFlowToMutableFlowShortcut(email1Flow)
    private set
    private var email2 by CastFlowToMutableFlowShortcut(email2Flow)

    val canBeAcceptedFlow = combine(email1Flow, email2Flow) { e1, e2 ->
        android.util.Patterns.EMAIL_ADDRESS.matcher(e1).matches()
            && e1.equals(e2,ignoreCase = true)
    }

    /*
     * Text to be shown as "OK" button in email dialog
     * Will switch to "VERIFY" if clicking it will lead to a verification mail being sent
     * Initially set to OK if email verified or empty, or VERIFY otherwise
     */
    val okOrVerifyFlow = combine(email1Flow, ServerPrefs.emailAddress.flow, ServerPrefs.emailVerified.flow){
        email1, knownEmail, verified ->
            if (email1 == knownEmail && verified || email1.isEmpty())
                android.R.string.ok
            else R.string.verify
    }

    fun resetStatus(){
        status = null
    }

    /**
     * When OK clicked:
     * - This checks for correct data (should always be the case but never hurts to doublecheck)
     * - If email has been changed, will set Preferences.emailVerified to false
     * - If Preferences.emailVerified was false or has just been set to that, call UserManagement.changeEmailAddress()
     * - feeds back DONE to fragment if email changed, OK if nothing happened, so it will close itself
     */
    fun okClicked() = viewModelScope.launch{
        if (email1 != email2) {
            status = EmailDialogStatus.Error(EMAILS_DO_NOT_MATCH)
            return@launch
        }

        if(!android.util.Patterns.EMAIL_ADDRESS.matcher(email1).matches()
        && email1.isNotBlank()) {
            status = EmailDialogStatus.Error(INVALID_EMAIL_ADDRESS)
            return@launch
        }

        if (ServerPrefs.emailAddress().lowercase(Locale.ROOT) != email1)
            ServerPrefs.emailVerified(false)

        status = if (!ServerPrefs.emailVerified()) {
            if (email1 != ServerPrefs.emailAddress().lowercase(Locale.ROOT))
                ServerPrefs.emailAddress(email1)

            if (email1.isNotBlank()) {
                viewModelScope.launch {
                    // UserManagement takes care of any errors that may arise
                    UserManagement().changeEmailAddress(email1)
                }
            }
            EmailDialogStatus.Done
        }
        else EmailDialogStatus.DoneNoChanges



    }

    fun updateEmail1(e: String){
        email1 = e.trim()
    }

    fun completedEmail1(){
        if(!android.util.Patterns.EMAIL_ADDRESS.matcher(email1).matches())
            status = EmailDialogStatus.Error(INVALID_EMAIL_ADDRESS_1)
    }

    fun updateEmail2(e: String){
        email2 = e.trim()
    }

    fun completedEmail2(){
        if(!android.util.Patterns.EMAIL_ADDRESS.matcher(email2).matches())
            status = EmailDialogStatus.Error(INVALID_EMAIL_ADDRESS_2)
        if(!email1.equals(email2, ignoreCase = true))
            status = EmailDialogStatus.Error(ENTRIES_DO_NOT_MATCH)
    }

}