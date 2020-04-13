/*
 * JoozdLog Pilot's Logbook
 * Copyright (C) 2020 Joost Welle
 *
 *     This program is free software: you can redistribute it and/or modify
 *     it under the terms of the GNU Affero General Public License as
 *     published by the Free Software Foundation, either version 3 of the
 *     License, or (at your option) any later version.
 *
 *     This program is distributed in the hope that it will be useful,
 *     but WITHOUT ANY WARRANTY; without even the implied warranty of
 *     MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 *     GNU Affero General Public License for more details.
 *
 *     You should have received a copy of the GNU Affero General Public License
 *     along with this program.  If not, see https://www.gnu.org/licenses
 */

package nl.joozd.joozdlogcommon.comms

/**
 * Keywords to be used for determining request types in JoozdLogComms
 * Protocol: <KEYWORD><PAYLOAD_IF_ANY>
 * Keywords are to be wrapped with _wrap_ from ByteArrayMaker.kt
 * Payload can be anything (raw bytes)
 * Should be entered into a Packet for transmission
 */

object JoozdlogCommsKeywords {
    const val HEADER = "JOOZDLOG"
    const val OK = "OK"
    const val USER_ALREADY_EXISTS = "EXISTING_USER"
    const val UNKNOWN_USER_OR_PASS = "UNKNOWN_USER_PASS_COMBO"
    const val NOT_LOGGED_IN = "NOT_LOGGED_IN"
    const val SERVER_ERROR = "SERVER_ERROR "


    const val LOGIN = "LOGIN"
    const val NEW_ACCOUNT = "NEW_ACCOUNT"
    const val CHANGE_PASSWORD = "CHANGE_PASSWORD" // will delete all saved files on server
    const val UPDATE_PASSWORD = "UPDATE_PASSWORD" // should be done after being logged in as it will recrypt all flights with new password
    const val CHECK_NEW_ID_NUMBERS = "CHECK_IDS"
    const val SENDING_FLIGHTS = "SENDING_FLIGHTS"
    const val SENDING_AIRCRAFT = "SENDING_AIRCRAFT"
    const val NEXT_IS_COMPRESSED = "NEXT_IS_COMPRESSED"
    const val REQUEST_TIMESTAMP = "REQUEST_TIMESTAMP"
    const val ADD_TIMESTAMP = "ADD_TIMESTAMP"
    const val REQUEST_FLIGHTS_SINCE_TIMESTAMP = "REQUEST_FLIGHTS_SINCE_TIMESTAMP" // can be used with timestamp -1 for all flights
    const val REQUEST_AIRPORT_DB_VERSION = "REQUEST_AIRPORT_DB_VERSION"
    const val REQUEST_AIRPORT_DB = "REQUEST_AIRPORT_DB"
    const val REQUEST_AIRCRAFT_CONSENSUS = "REQUEST_CONSENSUS"
    const val END_OF_SESSION = "END_OF_SESSION"
    const val SAVE_CHANGES = "SAVE_CHANGES"

}