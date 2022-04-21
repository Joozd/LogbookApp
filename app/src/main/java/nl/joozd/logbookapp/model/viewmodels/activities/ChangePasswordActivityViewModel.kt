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

package nl.joozd.logbookapp.model.viewmodels.activities

import android.content.ClipData
import android.content.ClipboardManager
import android.content.Context
import android.content.SharedPreferences
import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.viewModelScope
import kotlinx.coroutines.launch
import nl.joozd.logbookapp.App
import nl.joozd.logbookapp.R
import nl.joozd.logbookapp.data.comm.Cloud
import nl.joozd.logbookapp.data.comm.InternetStatus
import nl.joozd.logbookapp.data.comm.UserManagement
import nl.joozd.logbookapp.data.comm.CloudFunctionResults
import nl.joozd.logbookapp.data.sharedPrefs.Prefs
import nl.joozd.logbookapp.model.feedbackEvents.FeedbackEvents.ChangePasswordEvents
import nl.joozd.logbookapp.model.viewmodels.JoozdlogActivityViewModel
import nl.joozd.logbookapp.utils.generatePassword

//TODO remove livedata, move flow collection to activity
class ChangePasswordActivityViewModel: JoozdlogActivityViewModel() {

    /***********************************************************************************************
     * Private parts
     ***********************************************************************************************/

    private val _emailAddress = MutableLiveData(Prefs.emailAddress)

    //TODO collect flows in activity, not here
    init{
        viewModelScope.launch {
            Prefs.emailAddressFlow.collect{
                _emailAddress.value = it
            }
        }
    }

    /***********************************************************************************************
     * Public parts
     ***********************************************************************************************/

    /**
     * Observables
     */
    val online: LiveData<Boolean>
        get() = InternetStatus.internetAvailableLiveData


    val emailAddress: LiveData<String?>
        get() = _emailAddress

    /**
     * Public functions
     */

    /**
     * If submit button is clicked, we will check if internet is available
     * If it is, generate a password and attempt to get server to set that as current password.
     * If success, request a login link email (or schedule it for when email verified)
     * If fail or no internet, feedback to Activity
     */
    fun submitClicked(){
        val password = generatePassword(16)
        when {
            InternetStatus.internetAvailable != true -> feedback(ChangePasswordEvents.NO_INTERNET)
            else -> { // passwords match, are good enough, username not empty and internet looks OK
                feedback(ChangePasswordEvents.WAITING_FOR_SERVER)
                viewModelScope.launch {
                    when (UserManagement.changePassword(password)){
                        CloudFunctionResults.OK -> {
                            sendPasswordLinksToClipboard(UserManagement.generateLoginLink())
                            if (Prefs.emailVerified)
                                Cloud.requestLoginLinkMail()
                            else {
                                Prefs.emailJobsWaiting.sendLoginLink = true
                            }
                            feedback(ChangePasswordEvents.FINISHED)
                        }
                        CloudFunctionResults.NO_LOGIN_DATA -> feedback(ChangePasswordEvents.NOT_LOGGED_IN)
                        CloudFunctionResults.UNKNOWN_USER_OR_PASS -> feedback(ChangePasswordEvents.LOGIN_INCORRECT)
                        CloudFunctionResults.CLIENT_ERROR -> feedback(ChangePasswordEvents.SERVER_NOT_RESPONDING)
                        else -> feedback(ChangePasswordEvents.NOT_IMPLEMENTED)
                    }
                }
            }
        }
    }

    private fun sendPasswordLinksToClipboard(new: String?) {
        new?.let { newLink ->
            with(App.instance) {
                (getSystemService(Context.CLIPBOARD_SERVICE) as ClipboardManager).setPrimaryClip(
                    ClipData.newPlainText(getString(R.string.login_link), newLink)
                )
            }
            feedback(ChangePasswordEvents.LOGIN_LINK_COPIED)
        } ?: feedback(ChangePasswordEvents.NOT_LOGGED_IN)
    }
}