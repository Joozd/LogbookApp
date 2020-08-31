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

import android.util.Log
import androidx.lifecycle.LiveData
import androidx.lifecycle.viewModelScope
import kotlinx.coroutines.launch
import nl.joozd.logbookapp.data.comm.InternetStatus
import nl.joozd.logbookapp.data.comm.UserManagement
import nl.joozd.logbookapp.data.sharedPrefs.Preferences
import nl.joozd.logbookapp.model.feedbackEvents.FeedbackEvents.ChangePasswordEvents
import nl.joozd.logbookapp.model.viewmodels.JoozdlogActivityViewModel
import nl.joozd.logbookapp.utils.checkPasswordSafety

class ChangePasswordActivityViewModel: JoozdlogActivityViewModel() {

    /***********************************************************************************************
     * Private parts
     ***********************************************************************************************/



    /***********************************************************************************************
     * Public parts
     ***********************************************************************************************/

    /**
     * Observables
     */
    val online: LiveData<Boolean>
        get() = InternetStatus.internetAvailableLiveData

    /**
     * Public functions
     */

    fun submitClicked(password: String, repeatPassword: String){
        Log.d("submitClicked()", "pass1: $password, pass2: $repeatPassword")
        when {
            InternetStatus.internetAvailable != true -> feedback(ChangePasswordEvents.NO_INTERNET)
            password.isBlank() -> feedback(ChangePasswordEvents.PASSWORD_TOO_SHORT)
            password != repeatPassword -> feedback(ChangePasswordEvents.PASSWORDS_DO_NOT_MATCH)
            !checkPasswordSafety(password) -> feedback(ChangePasswordEvents.PASSWORD_DOES_NOT_MEET_STANDARDS)
            else -> { // passwords match, are good enough, username not empty and internet looks OK
                feedback(ChangePasswordEvents.WAITING_FOR_SERVER)
                viewModelScope.launch {
                    when (UserManagement.changePassword(password)){
                        UserManagement.ReturnCodes.SUCCESS -> feedback(ChangePasswordEvents.FINISHED)
                        UserManagement.ReturnCodes.NO_PASSWORD, UserManagement.ReturnCodes.NO_USERNAME -> feedback(ChangePasswordEvents.NOT_LOGGED_IN)
                        UserManagement.ReturnCodes.WRONG_CREDENTIALS -> feedback(ChangePasswordEvents.LOGIN_INCORRECT)

                        UserManagement.ReturnCodes.CONNECTION_ERROR -> feedback(ChangePasswordEvents.SERVER_NOT_RESPONDING)
                    }
                }
            }
        }
    }
}