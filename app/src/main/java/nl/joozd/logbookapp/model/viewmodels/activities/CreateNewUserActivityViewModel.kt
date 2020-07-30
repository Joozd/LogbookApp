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

import android.content.SharedPreferences
import android.text.Editable
import android.util.Log
import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.viewModelScope
import kotlinx.coroutines.launch
import nl.joozd.logbookapp.data.comm.InternetStatus
import nl.joozd.logbookapp.data.comm.UserManagement
import nl.joozd.logbookapp.data.sharedPrefs.Preferences
import nl.joozd.logbookapp.model.feedbackEvents.FeedbackEvents.CreateNewUserActivityEvents
import nl.joozd.logbookapp.model.viewmodels.JoozdlogActivityViewModel
import nl.joozd.logbookapp.utils.checkPasswordSafety

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

    fun signUpClicked(username: String, pass1: String, pass2: String) {
        Log.d("signUpClicked", "user: $username, pass1: $pass1, pass2: $pass2")
        Preferences.useCloud = true
        when {
            InternetStatus.internetAvailable != true -> feedback(CreateNewUserActivityEvents.NO_INTERNET)
            pass1.isBlank() -> feedback(CreateNewUserActivityEvents.PASSWORD_TOO_SHORT)
            username.isBlank() -> feedback(CreateNewUserActivityEvents.USERNAME_TOO_SHORT)
            pass1 != pass2 -> feedback(CreateNewUserActivityEvents.PASSWORDS_DO_NOT_MATCH)
            !checkPasswordSafety(pass1) -> feedback(CreateNewUserActivityEvents.PASSWORD_DOES_NOT_MEET_STANDARDS)
            else -> { // passwords match, are good enough, username not empty and internet looks OK
                feedback(CreateNewUserActivityEvents.WAITING_FOR_SERVER)
                viewModelScope.launch {
                    when (UserManagement.createNewUser(username, pass1)) {
                        true -> feedback(CreateNewUserActivityEvents.FINISHED)
                        null -> feedback(CreateNewUserActivityEvents.SERVER_NOT_RESPONDING)
                        false -> { // username taken, check to see if password correct
                            when (UserManagement.login(username, pass1)) {
                                true -> feedback(CreateNewUserActivityEvents.USER_EXISTS_PASSWORD_CORRECT)
                                false -> feedback(CreateNewUserActivityEvents.USER_EXISTS_PASSWORD_INCORRECT)
                                null -> feedback(CreateNewUserActivityEvents.SERVER_NOT_RESPONDING)
                            }
                        }
                    }
                }
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
    var password1State: Editable? = null
    var password2State: Editable? = null

    /*******************************************************************************************
     * Cleanup on cleared
     *******************************************************************************************/

    override fun onCleared() {
        super.onCleared()
        Preferences.getSharedPreferences().unregisterOnSharedPreferenceChangeListener (onSharedPrefsChangedListener)
    }

}