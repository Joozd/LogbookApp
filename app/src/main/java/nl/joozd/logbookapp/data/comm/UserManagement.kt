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

import android.util.Log
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
                Preferences.useCloud = true
                Log.d("CreateNewUser()", "created username: $username, password: $password")
                Log.d("CreateNewUser()", "check: ${Preferences.username}, password: ${Preferences.password}")
            }
            else Log.d("CreateNewUser()", "Cloud.createNewUser returned $it")
        }
    }

    suspend fun changePassword(newPassword: String): Int{
        val username = Preferences.username ?: return ReturnCodes.NO_USERNAME
        val password = Preferences.password ?: return ReturnCodes.NO_PASSWORD
        return when (Cloud.changePassword(newPassword).also{ Log.d("changePassword()", "returned $it")}){
            true -> {
                Preferences.password = newPassword
                ReturnCodes.SUCCESS
            }
            false -> ReturnCodes.WRONG_CREDENTIALS
            null -> ReturnCodes.CONNECTION_ERROR
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
                Preferences.useCloud = true
                Preferences.lastUpdateTime = -1
            }
        }
    }

    suspend fun loginFromLink(loginPassPair: Pair<String, String>): Boolean?{
        return Cloud.checkUserFromLink(loginPassPair.first, loginPassPair.second).also{
            if(it == true){
                Preferences.username = loginPassPair.first
                Preferences.forcePassword(loginPassPair.second)
                Preferences.lastUpdateTime = -1
                Preferences.useCloud = true
            }
        }
    }

    fun signOut(){
        Preferences.username = null
        Preferences.password = null
        Preferences.lastUpdateTime = -1
        Preferences.useCloud = false
    }

    fun gerenateLoginLink(): String? = Preferences.username?.let {
        "https://joozdlog.joozd.nl/inject-key/$it:${Preferences.password}"
    }




    object ReturnCodes {
        const val SUCCESS = 0
        const val CONNECTION_ERROR = -999
        const val WRONG_CREDENTIALS = 1
        const val NO_USERNAME = 2
        const val NO_PASSWORD = 3
    }

}