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

package nl.joozd.joozdlogcommon

import nl.joozd.serializing.*

/**
 * This class matches aircraft registrations to types
 * @param registration: Primary key, aircraft registration (can be any string, but usually something like "PH-EZE"
 * @param type: AircraftTypeData.name<String> to match with AircraftType
 */
data class ForcedTypeData(val registration: String, val type: String): JoozdSerializable {
    override fun serialize(): ByteArray {
        var serialized = ByteArray(0)
        serialized += wrap(component1())
        serialized += wrap(component2())
        return serialized
    }

    companion object: JoozdSerializable.Deserializer<ForcedTypeData> {
        override fun deserialize(source: ByteArray): ForcedTypeData {
            val wraps = ForcedTypeData.serializedToWraps(source)
            return ForcedTypeData(
                unwrap(wraps[0]),
                unwrap(wraps[1])
            )
        }
    }
}

