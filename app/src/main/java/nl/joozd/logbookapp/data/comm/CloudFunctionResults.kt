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

package nl.joozd.logbookapp.data.comm

enum class CloudFunctionResults {
    /**
     * Return codes for Joozdlog Cloud functions.
     * All values are randomly chosen and do not need to be in order. Might as well be an enum class.
     */
    OK,
    NOT_A_VALID_EMAIL_ADDRESS,
    USER_ALREADY_EXISTS,
    UNKNOWN_REPLY_FROM_SERVER,
    UNKNOWN_USER_OR_PASS,
    EMAIL_DOES_NOT_MATCH,
    NO_CHANGES,
    NO_LOGIN_DATA,              // No login data stored in Preferences
    NOT_LOGGED_IN,              // Server expected user to be logged in before doing this, but user was not logged in

    NO_INTERNET,                // No internet connection present
    DATA_ERROR,                 // bad data received from server
    CLIENT_NOT_ALIVE,           // tried to use a client with 'alive' flag set to false
    SERVER_ERROR,               // server reported encountering an error
    CLIENT_ERROR,               // client encountered an error (eg. connection dropping halfway)
    SOCKET_IS_NULL,             // No socket created for this Client
    UNKNOWN_HOST,               // Client could not find host
    IO_ERROR,                   // Generic IO error
    CONNECTION_REFUSED,         // ConnectException occured. Typically, the connection was refused remotely (e.g., no process is listening on the remote address/port).
    SOCKET_ERROR;               // there is an error creating or accessing a Socket.

    fun isOK(): Boolean = this == OK
    companion object{
        val connectionErrors = listOf(NO_INTERNET, DATA_ERROR, CLIENT_NOT_ALIVE, SERVER_ERROR, CLIENT_ERROR, SOCKET_IS_NULL, UNKNOWN_HOST, IO_ERROR, CONNECTION_REFUSED, SOCKET_ERROR)
    }

}