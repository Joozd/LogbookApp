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

import nl.joozd.logbookapp.model.helpers.FeedbackEvents.LoginActivityEvents
import nl.joozd.logbookapp.model.viewmodels.JoozdlogActivityViewModel

class LoginActivityViewModel: JoozdlogActivityViewModel(){
    /*******************************************************************************************
     * Private parts
     *******************************************************************************************/


    /*******************************************************************************************
     * Observables
     *******************************************************************************************/


    /*******************************************************************************************
     * Public functions
     *******************************************************************************************/

    fun signIn(username: String, password: String){
        //check if username and password are not empty
        if (username.isBlank()) {
            feedback(LoginActivityEvents.NAME_EMPTY)
            return
        }
        if (password.isBlank()) {
            feedback(LoginActivityEvents.PASSWORD_EMPTY)
            return
        }
        //TODO check user/pass with server
    }

    /**
     * Checks if server is online. If not, it should set online status accordingly
     */
    fun checkServerOnline(){
        feedback(LoginActivityEvents.NOT_IMPLEMENTED)
        //TODO: Implement this
    }
}