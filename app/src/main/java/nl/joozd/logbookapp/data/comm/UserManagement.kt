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

package nl.joozd.logbookapp.data.comm

import nl.joozd.logbookapp.data.sharedPrefs.Preferences

object UserManagement {
    val signedIn: Boolean
        get() = Preferences.username != null

    // This is used to check if loginActivity got returned to after a new user was just
    // successfully created and it can close right away
    var justCreatedNewUser: Boolean = false
    /**
     * Create a new user on server
     * @return true if success
     * @return false if username taken
     * @return null if server or connection error
     */
    suspend fun createNewUser(username: String, password: String): Boolean?{
        return(Cloud.createNewUser(username, password)).also{
            if(it == true){
                Preferences.username = username
                Preferences.password = password
                Preferences.lastUpdateTime = -1
            }
        }
    }

    /**
     * check username/password with server and store them if OK
     * @return true if success
     * @return false if username taken
     * @return null if server or connection error
     */
    suspend fun login(username: String, password: String): Boolean?{
        return Cloud.checkUser(username, password).also{
            if(it == true){
                Preferences.username = username
                Preferences.password = password
                Preferences.lastUpdateTime = -1
            }
        }
    }

    fun signOut(){
        Preferences.username = null
        Preferences.password = null
        Preferences.lastUpdateTime = -1
    }
}