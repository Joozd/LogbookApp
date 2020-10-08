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
import nl.joozd.joozdlogcommon.serializing.unwrapByteArray
import nl.joozd.joozdlogcommon.serializing.wrap

/**
 * Add or remove an opinion to/from consensus
 * @param registration: Registration of aircraft this applies to
 * @param serializedType: AircraftType. Can be non-serialized through secondary constructor
 * @param subtract: If true, this needs to be subtracted from consensus
 */

@Suppress("ArrayInDataClass")
data class ConsensusData(val registration: String,
                         val serializedType: ByteArray, // serialized AircraftType
                         val subtract: Boolean = false
                        ) : JoozdlogSerializable {
    constructor(registration: String, aircraftType: AircraftType, subtract: Boolean = false): this(registration, aircraftType.serialize(), subtract)

    override fun serialize(): ByteArray {
        var serialized = ByteArray(0)

        serialized += wrap(component1())
        serialized += wrap(component2())
        serialized += wrap(component3())

        return serialized
    }
    val aircraftType: AircraftType                          // TODO This shouldn't be here
        get() = AircraftType.deserialize(serializedType)    // TODO check if used on server


    companion object: JoozdlogSerializable.Creator {
        override fun deserialize(source: ByteArray): ConsensusData {
            val wraps = ConsensusData.serializedToWraps(source)
            return ConsensusData(
                unwrap(wraps[0]),
                unwrapByteArray(wraps[1]), // needs to be forced bytearray to prevent ambiguity with secondary constructor
                unwrap(wraps[2])
            )
        }
    }
}
