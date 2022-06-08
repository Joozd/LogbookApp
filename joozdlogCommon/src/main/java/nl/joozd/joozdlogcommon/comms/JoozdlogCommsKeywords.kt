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

package nl.joozd.joozdlogcommon.comms

/**
 * Keywords to be used for determining request types in JoozdLogComms
 * Protocol: <KEYWORD><PAYLOAD_IF_ANY>
 * Keywords are to be wrapped with _wrap_ from ByteArrayMaker.kt
 * Payload can be anything (raw bytes)
 * Should be entered into a Packet for transmission
 */

object JoozdlogCommsKeywords {
    //protocol keywords
    const val HEADER = "JOOZDLOG"
    const val OK = "OK"
    const val SERVER_ERROR = "SERVER_ERROR "
    const val BAD_DATA_RECEIVED = "BAD_DATA_RECEIVED"
    const val NEXT_IS_COMPRESSED = "NEXT_IS_COMPRESSED"
    const val HELLO = "HELLO" // to be done before any communications with PROTOCOL_VERSION. Do this right after creating SSL connection.
    const val END_OF_SESSION = "END_OF_SESSION"

    //general keywords
    const val REQUEST_TIMESTAMP = "REQUEST_TIMESTAMP"
    const val REQUEST_BACKUP_MAIL = "REQUEST_BACKUP_MAIL"
    const val SENDING_FEEDBACK = "SENDING_FEEDBACK"

    //email keywords
    const val SET_EMAIL = "SET_EMAIL"               // Sets email hash on server, can expect to receive an email on given address with a confirmation link
    const val CONFIRM_EMAIL = "CONFIRM_EMAIL"       // confirm email hash - send wrapped confirmation string ("user:BASE64HASH") as extraData
    const val REQUEST_LOGIN_LINK_MAIL = "REQUEST_LOGIN_LINK_MAIL"
    const val EMAIL_NOT_KNOWN_OR_VERIFIED = "EMAIL_NOT_KNOWN_OR_VERIFIED"
    const val NOT_A_VALID_EMAIL_ADDRESS = "NOT_A_VALID_EMAIL_ADDRESS"


    //account keywords
    const val LOGIN = "LOGIN"
    const val REQUEST_NEW_USERNAME = "REQUEST_NEW_USERNAME"
    const val NEW_ACCOUNT = "NEW_ACCOUNT"
    const val UPDATE_PASSWORD = "UPDATE_PASSWORD" // should be done after being logged in as it will recrypt all flights with new password
    const val USER_ALREADY_EXISTS = "EXISTING_USER"
    const val UNKNOWN_USER_OR_PASS = "UNKNOWN_USER_PASS_COMBO"
    const val NOT_LOGGED_IN = "NOT_LOGGED_IN"

    //flights keywords
    const val REQUEST_FLIGHTS_LIST_CHECKSUM = "REQUEST_FLIGHTS_LIST_CHECKSUM" // Client can expect a serialized FlightsListChecksum
    const val REQUEST_ID_WITH_TIMESTAMPS_LIST = "REQUEST_ID_WITH_TIMESTAMPS_LIST" // Client can expect a packed List<serialized IDWithTimeStamp>
    const val REQUEST_FLIGHTS = "REQUEST_FLIGHTS" // expects in extraData a list of Ints, representing a list of FlightIDs to send. Client can expect a list of all found flights as List<serialized BasicFlight>; nonexistent IDs will be ignored.
    const val SENDING_NEWER_FLIGHTS = "SENDING_NEWER_FLIGHTS" // server will expect a packed List<serialized BasicFlight> as extraData

    //deprecated, but here for backwards compatibility
    const val SENDING_FLIGHTS = "SENDING_FLIGHTS"
    const val REQUEST_FLIGHTS_SINCE_TIMESTAMP = "REQUEST_FLIGHTS_SINCE_TIMESTAMP" // can be used with timestamp -1 for all flights


    //aircraft keywords
    const val SENDING_AIRCRAFT_CONSENSUS = "SENDING_AIRCRAFT_CONSENSUS" // adds a packSerialized list of <AircraftConsensus>
    const val REQUEST_AIRCRAFT_CONSENSUS = "REQUEST_AIRCRAFT_CONSENSUS"
    const val REQUEST_AIRCRAFT_TYPES = "REQUEST_AIRCRAFT_TYPES"
    const val REQUEST_AIRCRAFT_TYPES_VERSION = "REQUEST_AIRCRAFT_TYPES_VERSION"
    const val REQUEST_FORCED_TYPES = "REQUEST_FORCED_TYPES"
    const val REQUEST_FORCED_TYPES_VERSION = "REQUEST_FORCED_TYPES_VERSION"

    //airport functions
    const val REQUEST_AIRPORT_DB_VERSION = "REQUEST_AIRPORT_DB_VERSION"
    const val REQUEST_AIRPORT_DB = "REQUEST_AIRPORT_DB"

    //control keywords
    const val ADD_TIMESTAMP = "ADD_TIMESTAMP"
    const val SAVE_CHANGES = "SAVE_CHANGES"

    //debug keywords
    const val DEBUG_SEND_TEST_MAIL = "DEBUG_SEND_TEST_MAIL"


}