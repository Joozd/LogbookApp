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

import nl.joozd.comms.CommsKeywords

/**
 * Keywords to be used for determining request types in JoozdLogComms
 * Protocol: <KEYWORD><PAYLOAD_IF_ANY>
 * Keywords are to be wrapped with _wrap_ from ByteArrayMaker.kt
 * Payload can be anything (raw bytes)
 * Should be entered into a Packet for transmission
 */

enum class JoozdlogCommsRequests(val keyword: String){
    HELLO(CommsKeywords.HELLO),
    END_OF_SESSION(CommsKeywords.END_OF_SESSION),

    //requests
    SENDING_FEEDBACK("SENDING_FEEDBACK"),
    SET_EMAIL("SET_EMAIL"),
    CONFIRM_EMAIL("CONFIRM_EMAIL"),
    SENDING_BACKUP_EMAIL_DATA("SENDING_BACKUP_EMAIL_DATA"),
    MIGRATE_EMAIL_DATA("MIGRATE_EMAIL_DATA"),
    SENDING_P2P_DATA("SENDING_P2P_DATA"),
    REQUEST_P2P_DATA("REQUEST_P2P_DATA"),

    UNKNOWN_KEYWORD("UNKNOWN_KEYWORD");





    companion object {
        private val keyWords by lazy { values().associateBy { it.keyword } }
        fun toKeyword(s: String): JoozdlogCommsRequests = keyWords[s] ?: UNKNOWN_KEYWORD
    }




}