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

package nl.joozd.logbookapp.model.viewmodels.activities

import android.content.ClipData
import android.content.ClipboardManager
import android.content.Context
import android.content.SharedPreferences
import android.text.Editable
import android.util.Log
import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.viewModelScope
import kotlinx.coroutines.launch
import nl.joozd.logbookapp.App
import nl.joozd.logbookapp.R
import nl.joozd.logbookapp.data.comm.InternetStatus
import nl.joozd.logbookapp.data.comm.ServerFunctions
import nl.joozd.logbookapp.data.comm.UserManagement
import nl.joozd.logbookapp.data.comm.protocol.CloudFunctionResults
import nl.joozd.logbookapp.data.repository.flightRepository.FlightRepository
import nl.joozd.logbookapp.data.sharedPrefs.Preferences
import nl.joozd.logbookapp.model.feedbackEvents.FeedbackEvents.NewUserActivityEvents
import nl.joozd.logbookapp.model.viewmodels.JoozdlogActivityViewModel
import nl.joozd.logbookapp.utils.checkPasswordSafety
import nl.joozd.logbookapp.utils.generatePassword

class CreateNewUserActivityViewModel: JoozdlogActivityViewModel() {

    /*******************************************************************************************
     * Private parts
     *******************************************************************************************/

    private val _acceptedTerms = MutableLiveData(Preferences.acceptedCloudSyncTerms)

    /**
     * onSharedPrefsListener
     */
    private val onSharedPrefsChangedListener =  SharedPreferences.OnSharedPreferenceChangeListener { _, key ->
        // Log.d("AirportRepository", "key = $key")
        if (key == Preferences::acceptedCloudSyncTerms.name) _acceptedTerms.value = Preferences.acceptedCloudSyncTerms
    }
    init{
        Preferences.getSharedPreferences().registerOnSharedPreferenceChangeListener (onSharedPrefsChangedListener)
    }


    /*******************************************************************************************
     * Public functions
     *******************************************************************************************/

    fun signUpClicked(username: String) {
        Log.d("signUpClicked", "user: $username")
        Preferences.useCloud = true
        when {
            InternetStatus.internetAvailable != true -> feedback(NewUserActivityEvents.NO_INTERNET)
            username.isBlank() -> feedback(NewUserActivityEvents.USERNAME_TOO_SHORT)
            else -> { // passwords match, are good enough, username not empty and internet looks OK
                feedback(NewUserActivityEvents.WAITING_FOR_SERVER)
                viewModelScope.launch {
                    val email = Preferences.emailAddress.takeIf { it.isNotEmpty() }
                    when (UserManagement.createNewUser(username, generatePassword(16), email)) { // UserManagement will call the correct Cloud function
                        CloudFunctionResults.OK -> {
                            feedback(NewUserActivityEvents.LOGGED_IN_AS)
                            Preferences.emailJobsWaiting.sendLoginLink = true
                        }
                        CloudFunctionResults.CLIENT_ERROR -> feedback(NewUserActivityEvents.SERVER_NOT_RESPONDING)
                        CloudFunctionResults.SERVER_ERROR -> feedback(NewUserActivityEvents.SERVER_NOT_RESPONDING)
                        CloudFunctionResults.NOT_A_VALID_EMAIL_ADDRESS -> feedback(NewUserActivityEvents.BAD_EMAIL)
                        CloudFunctionResults.UNKNOWN_REPLY_FROM_SERVER -> feedback(NewUserActivityEvents.SERVER_NOT_RESPONDING)
                        CloudFunctionResults.USER_ALREADY_EXISTS -> feedback(NewUserActivityEvents.USER_EXISTS)
                        else -> feedback(NewUserActivityEvents.UNKNOWN_ERROR)
                    }
                }
            }
        }
    }

    /**
     * Copy a login link to clipboard (assuming logged in, else do nothing)
     */
    fun copyLoginLinkToClipboard(){
        UserManagement.generateLoginLink()?.let { loginLink ->
            with(App.instance) {
                (getSystemService(Context.CLIPBOARD_SERVICE) as ClipboardManager).setPrimaryClip(
                    ClipData.newPlainText(getString(R.string.login_link), loginLink)
                )
            }
        }
    }

    fun dontUseCloud(){
        Preferences.useCloud = false
    }

    fun signOut(){
        UserManagement.signOut()
    }

    /*******************************************************************************************
     * Observables
     *******************************************************************************************/

    val acceptedTerms: LiveData<Boolean>
        get() = _acceptedTerms

    /*******************************************************************************************
     * saved instance state variables
     *******************************************************************************************/

    var userNameState: Editable? = null

    /*******************************************************************************************
     * Cleanup on cleared
     *******************************************************************************************/

    override fun onCleared() {
        super.onCleared()
        Preferences.getSharedPreferences().unregisterOnSharedPreferenceChangeListener (onSharedPrefsChangedListener)
    }

}