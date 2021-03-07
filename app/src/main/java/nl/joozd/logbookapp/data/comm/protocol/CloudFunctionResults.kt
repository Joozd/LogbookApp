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

package nl.joozd.logbookapp.data.comm.protocol

enum class CloudFunctionResults {
    /**
     * Return codes for Joozdlog Cloud functions.
     * All values are randomly chosen and do not need to be in order. Might as well be an enum class.
     */
    OK,
    NO_INTERNET,
    NOT_A_VALID_EMAIL_ADDRESS,
    SERVER_ERROR,
    CLIENT_ERROR,
    DATA_ERROR,
    USER_ALREADY_EXISTS,
    UNKNOWN_REPLY_FROM_SERVER,
    UNKNOWN_USER_OR_PASS,
    EMAIL_DOES_NOT_MATCH,
    NO_LOGIN_DATA
}