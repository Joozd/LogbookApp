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

package nl.joozd.joozdlogcommon

import nl.joozd.joozdlogcommon.serializing.JoozdlogSerializable
import nl.joozd.joozdlogcommon.serializing.unwrap
import nl.joozd.joozdlogcommon.serializing.wrap

data class FeedbackData(val feedback: String, val contactInfo: String): JoozdlogSerializable {
    override fun serialize(): ByteArray {
        var serialized = ByteArray(0)
        serialized += wrap(component1())
        serialized += wrap(component2())

        return serialized
    }

    companion object : JoozdlogSerializable.Creator {
        override fun deserialize(source: ByteArray): FeedbackData {
            val wraps = serializedToWraps(source)
            return FeedbackData(
                unwrap(wraps[0]),
                unwrap(wraps[1])
            )
        }
    }
}