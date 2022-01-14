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

package nl.joozd.logbookapp.data.sharedPrefs.errors

enum class Errors(val flag: Long) {
    /**
     * User tried to confirm an email address with server, but server rejected confirmation data
     */
    EMAIL_CONFIRMATION_FAILED(1),

    /**
     * User tried to log in to server but server doesn't know this combination of login/pass
     */
    LOGIN_DATA_REJECTED_BY_SERVER(2),

    /**
     * Server gave an unexpected response
     */
    SERVER_ERROR(4),

    /**
     * Server complained we tried to feed it a bad email address
     */
    BAD_EMAIL_SAVED(8)
}
