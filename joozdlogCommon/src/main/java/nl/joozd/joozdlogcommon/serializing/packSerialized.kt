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
 * Takes a list of ByteArrays and returns it as a single ByteArray
 * wrapped with an Int.toBytearray() that describes its length
 */
fun packSerialized(series: List<ByteArray>): ByteArray = series.map{it.size.toByteArray().toList() + it.toList()}.flatten().toByteArray()

fun packSerializable(series: List<JoozdlogSerializable>): ByteArray = packSerialized(series.map{it.serialize()})

fun unpackSerialized(packed: ByteArray): List<ByteArray>{
    val list = mutableListOf<ByteArray>()
    var index = 0
    while (packed.size > index+3){
        val size =
            intFromBytes(packed.slice(index until index + 4))
        if (size == 0) break // catch padding zeroes
        list.add(packed.slice(index+4 until (index+size+4)).toByteArray())
        index += size+4
    }
    return list
}