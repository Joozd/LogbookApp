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
 * TODO replace Fragment with JoozdlogAlertDialog
 */
class LoginActivityViewModel: JoozdlogActivityViewModel(){
    /*******************************************************************************************
     * Private parts
     *******************************************************************************************/

    /*******************************************************************************************
     * Observables
     *******************************************************************************************/

    /*******************************************************************************************
     * Public variables
     *******************************************************************************************/

    /*******************************************************************************************
     * Public functions
     *******************************************************************************************/

    fun signOut(){
        UserManagement.signOut()
        feedback(LoginActivityEvents.FINISHED)
    }
}