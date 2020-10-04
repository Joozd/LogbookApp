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

import androidx.lifecycle.LiveData
import androidx.lifecycle.viewModelScope
import kotlinx.coroutines.launch
import nl.joozd.logbookapp.App
import nl.joozd.logbookapp.R
import nl.joozd.logbookapp.data.comm.Cloud
import nl.joozd.logbookapp.data.comm.InternetStatus
import nl.joozd.logbookapp.data.comm.UserManagement
import nl.joozd.logbookapp.data.sharedPrefs.Preferences
import nl.joozd.logbookapp.model.feedbackEvents.FeedbackEvents.LoginActivityEvents
import nl.joozd.logbookapp.model.viewmodels.JoozdlogActivityViewModel

/**
 * ViewModel for login and create new user activity
 */
class LoginActivityViewModel: JoozdlogActivityViewModel(){
    /*******************************************************************************************
     * Private parts
     *******************************************************************************************/

    /*******************************************************************************************
     * Observables
     *******************************************************************************************/

    val internetAvailable: LiveData<Boolean>
        get() = InternetStatus.internetAvailableLiveData

    /*******************************************************************************************
     * Public variables
     *******************************************************************************************/

    /*******************************************************************************************
     * Public functions
     *******************************************************************************************/

    fun signIn(username: String, password: String){
        when {
            //check if username and password are not empty
            password == App.instance.ctx.getString(R.string.fake_hidden_password) && username == Preferences.username -> feedback(LoginActivityEvents.FINISHED) // if nothing changed, do nothing

            username.isBlank() -> feedback(LoginActivityEvents.USERNAME_EMPTY)

            password.isBlank() -> feedback(LoginActivityEvents.PASSWORD_EMPTY)

            // If offline, save username and pass but don't check, and feedback that
            InternetStatus.internetAvailable == false -> {
                Preferences.username = username
                Preferences.password = password // Preferences will take care of password hashing
                feedback(LoginActivityEvents.SAVED_WITHOUT_CHECKING_BECAUSE_NO_INTERNET)
            }

            // name and pass not empty, internet seems online, let's check it!
            else -> {
                viewModelScope.launch {
                    when (Cloud.checkUser(username, password)){
                        true -> {
                            Preferences.username = username
                            Preferences.password = password // Preferences will take care of password hashing
                            feedback(LoginActivityEvents.FINISHED)
                        }
                        false -> {
                            feedback(LoginActivityEvents.USERNAME_OR_PASSWORD_INCORRECT)
                        }
                        else -> {
                            Preferences.username = username
                            Preferences.password = password // Preferences will take care of password hashing
                            feedback(LoginActivityEvents.SAVED_WITHOUT_CHECKING_BECAUSE_NO_SERVER)
                        }
                    }
                }
            }
        }
    }

    fun signOut(){
        UserManagement.signOut()
        feedback(LoginActivityEvents.FINISHED)
    }
}