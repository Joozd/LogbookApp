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

package nl.joozd.joozdlogcommon.serializing

/**
 * a JoozdlogSerializable can pack itself into a ByteArray, or be created from such a ByteArray
 * Suggest to use this with nl.joozd.joozdlogcommon.utils.byteArrayMaker functions
 */
interface JoozdlogSerializable{
    fun serialize(): ByteArray

    // interface should be implemented if you want to de-serialize
    interface Creator{
        /**
         * Create a new instance of the JoozdlogSerializable class, instantiating it
         * from the given Parcel whose data had previously been written by
         * {@link serialize}
         *
         * @param source The Parcel to read the object's data from.
         * @return Returns a new instance of the JoozdlogSerializable class.
         */
        fun deserialize(source: ByteArray): JoozdlogSerializable

        fun serializedToWraps(bytes: ByteArray): List<ByteArray>{
            val bb = bytes.toList().toMutableList()
            val wraps = mutableListOf<ByteArray>()
            while (bb.isNotEmpty()){
                wraps.add(nextWrap(bb.toByteArray()))
                repeat(wraps.last().size){ bb.removeAt(0)}
            }
            return wraps
        }
    }
}