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
*  toByteArray extensions (and back)
*  Changes primitive types to arrays of bytes with the same values
*  version 2
*  Copyright (c) 2020 Joost Welle
*  You may redistribute/use/copy/modify/etc in whatever way you see fit.
*  This file is "as is".
*  It is not intended for any ose other than my own, and therefore I will not accept any liability in case
*  you decide to use it and it turns out to break all your data.
*  However, if it does, and you find out why, feel free to let me know so we can fix it.
*
*  usage: <Type>.toByteArray() will give a ByteArray with the same amount of bytes as the original type,
*  containing the same bits as the original.
*  the reverse, <Type>FromBytes(bytes:ByteArray) will reconstruct a basic type from bytes.
*
*  You could use it to stitch the bits of 2 Ints into one Long, or whatever
*  You could also use it to restructure data from a long stream of unmarked Bytes, or change structured data into such a stream.
*/
const val eightOnes = 255L
const val LONG: Byte = 1
const val INT: Byte = 2
const val SHORT: Byte = 3
const val CHAR: Byte = 4
const val FLOAT: Byte = 5
const val DOUBLE: Byte = 6
const val BOOLEAN: Byte = 7
const val BYTEARRAY: Byte = 8
const val BYTEARRAY_BASIC_VAL: Byte = 8
// BYTEARRAY will use 8..12 //      = 12

const val LIST: Byte = 13
const val LIST_BASIC_VALUE: Byte = 13
// LIST will use 13..17 //      = 17
const val MAP: Byte = 18
const val MAP_BASIC_VALUE: Byte = 18
// MAP will use 18..22 //      = 22

const val STRING_BASIC_VALUE: Byte = 32
const val STRING: Byte = 32

const val SIZE_WRAPPED_LONG = Long.SIZE_BYTES + 1
const val SIZE_WRAPPED_INT = Int.SIZE_BYTES + 1
const val SIZE_WRAPPED_SHORT = Short.SIZE_BYTES + 1
const val SIZE_WRAPPED_CHAR = Char.SIZE_BYTES + 1
const val SIZE_WRAPPED_FLOAT =
    (32 / 8) + 1 // assuming 32 bits Float as Float doesn't have SIZE_BYTES
const val SIZE_WRAPPED_DOUBLE =
    (64 / 8) + 1 // assuming 64 bits Double as Float doesn't have SIZE_BYTES
const val SIZE_WRAPPED_BOOLEAN = 1 + 1


fun Double.toByteArray(): ByteArray = java.lang.Double.doubleToLongBits(this).toByteArray()
fun doubleFromBytes(bytes: ByteArray): Double = java.lang.Double.longBitsToDouble(
    longFromBytes(
        bytes
    )
)

fun Float.toByteArray(): ByteArray = java.lang.Float.floatToIntBits(this).toByteArray()
fun floatFromBytes(bytes: ByteArray): Float = java.lang.Float.intBitsToFloat(
    intFromBytes(
        bytes
    )
)

/**
 * returns an array of Long.SIZE_BYTES (==8) bytes, containing the same bits
 */
fun Long.toByteArray(): ByteArray {
    val bytes = ByteArray(Long.SIZE_BYTES)
    repeat(Long.SIZE_BYTES) { pos ->
        bytes[pos] = this.shr(8 * pos).and(eightOnes).toByte()
    }
    return bytes.reversedArray()
}

/**
 * returns a Long with the same bits as a Long.SIZE_BYTES byte ByteArray
 */
fun longFromBytes(bytes: ByteArray): Long {
    require(bytes.size == Long.SIZE_BYTES) { "Trying to deserialize a long but got ${bytes.size} bytes" }
    var long = 0L

    // Least significant byte first, shift left by n bytes every time
    bytes.reversed().forEachIndexed { i, byte ->
        val comparator: Long =
            (if (byte >= 0) byte.toLong() else 256 + byte.toLong()).shl(8 * i)
        long = long or comparator
    }
    return long
}

fun longFromBytes(bytes: List<Byte>): Long =
    longFromBytes(bytes.toByteArray())


/**
 * returns an array of Int.SIZE_BYTES (==4) bytes, containing the same bits
 */
fun Int.toByteArray(): ByteArray {
    val bytes = ByteArray(Int.SIZE_BYTES)
    val eightOnes = 255
    repeat(Int.SIZE_BYTES) { pos ->
        bytes[pos] = this.shr(8 * pos).and(eightOnes).toByte()
    }
    return bytes.reversedArray()
}

/**
 * Makes an Int from an Int.SIZE_BYTES byte ByteArray
 */
fun intFromBytes(bytes: ByteArray): Int {
    require(bytes.size == Int.SIZE_BYTES) { "Trying to deserialize an Int but got ${bytes.size} bytes" }
    var int = 0

    // Least significant byte first, shift left by n bytes every time
    bytes.reversed().forEachIndexed { i, byte ->
        val comparator: Int = (if (byte >= 0) byte.toInt() else 256 + byte.toInt()).shl(8 * i)
        int = int or comparator
    }
    return int
}

fun intFromBytes(bytes: List<Byte>): Int =
    intFromBytes(bytes.toByteArray())

/**
 * returns an array of Char.SIZE_BYTES (==8) bytes, containing the same bits
 */
fun Char.toByteArray(): ByteArray {
    return this.toInt().toByteArray().takeLast(Char.SIZE_BYTES).toByteArray()
}

/**
 * Makes an Int from an Int.SIZE_BYTES byte ByteArray
 */
fun charFromBytes(bytes: ByteArray): Char {
    require(bytes.size == Char.SIZE_BYTES)
    return intFromBytes(ByteArray(Int.SIZE_BYTES - Char.SIZE_BYTES) + bytes)
        .toChar()
}

/**
 * returns an array of Int.SIZE_BYTES (==8) bytes, containing the same bits
 */
fun Short.toByteArray(): ByteArray {
    return this.toInt().toByteArray().takeLast(Short.SIZE_BYTES).toByteArray()
}

/**
 * Makes an Int from an Int.SIZE_BYTES byte ByteArray
 */
fun shortFromBytes(bytes: ByteArray): Short {
    require(bytes.size == Short.SIZE_BYTES)
    return intFromBytes(ByteArray(Int.SIZE_BYTES - Short.SIZE_BYTES) + bytes)
        .toShort()
}

/**
 * Makes aByteArray from a Boolean (so an 1 or a 0)
 */
fun Boolean.toByteArray(): ByteArray {
    return if (this) listOf(1.toByte()).toByteArray() else listOf(0.toByte()).toByteArray()
}

/**
 * Will turn any non-zero value into True
 */
fun booleanFromBytes(bytes: ByteArray): Boolean {
    require(bytes.size == 1)
    return bytes[0] > 0
}

fun <T, R> Pair<T, R>.toByteArray(): ByteArray {
    val first = when (this.first) {
        is Long -> (this.first as Long).toByteArray()
        is Int -> (this.first as Int).toByteArray()
        is Char -> (this.first as Char).toByteArray()
        is Double -> (this.first as Double).toByteArray()
        is Float -> (this.first as Float).toByteArray()
        else -> error("Unsupported type for ${this.first}")
    }
    val second = when (this.second) {
        is Long -> (this.second as Long).toByteArray()
        is Int -> (this.second as Int).toByteArray()
        is Char -> (this.second as Char).toByteArray()
        is Double -> (this.second as Double).toByteArray()
        is Float -> (this.second as Float).toByteArray()
        else -> error("Unsupported type for ${this.second}")
    }
    return first + second
}

//probably not gonna make this for every possibility
fun pairLongLongFromBytes(bytes: ByteArray): Pair<Long, Long> {
    require(bytes.size == Long.SIZE_BYTES + Long.SIZE_BYTES)
    return longFromBytes(
        bytes.slice(0 until Long.SIZE_BYTES).toByteArray()
    ) to longFromBytes(
        bytes.slice(Long.SIZE_BYTES until bytes.size).toByteArray()
    )
}

fun pairIntLongFromBytes(bytes: ByteArray): Pair<Int, Long> {
    require(bytes.size == Int.SIZE_BYTES + Long.SIZE_BYTES)
    return intFromBytes(
        bytes.slice(0 until Int.SIZE_BYTES).toByteArray()
    ) to longFromBytes(
        bytes.slice(Int.SIZE_BYTES until bytes.size).toByteArray()
    )
}

/**
 * Does not suport a list of Lists as the inner list type cannot be reflected (be me right now)
 */

inline fun <reified T> List<T>.toByteArray(): ByteArray {
    val supportedSingleValues = setOf(
        Long::class,
        Int::class,
        Short::class,
        Char::class,
        Float::class,
        Double::class,
        Boolean::class,
        String::class
    )
    // val supportedArrays = setOf(String::class, ByteArray::class)

    return when {
        T::class in supportedSingleValues -> packList(this.map { it.castedToByteArray() })
        T::class == ByteArray::class -> packList(this.map { it as ByteArray })
        T::class == Byte::class -> {
            val size = this.size
            ByteArray(this.size) {this[it] as Byte}
        }
        else -> error("${T::class} not supported")
    }
}

inline fun <reified T> listFromBytes(bytes: ByteArray): List<T> {
    return when (T::class) {
        String::class -> unpackList(bytes).map{it.toString(Charsets.UTF_8) as T}
        Long::class -> unpackList(bytes).map{ longFromBytes(it) as T}
        Int::class -> unpackList(bytes).map{ intFromBytes(it) as T}
        Short::class -> unpackList(bytes).map{ shortFromBytes(it) as T}
        Char::class -> unpackList(bytes).map{ charFromBytes(it) as T}
        Float::class -> unpackList(bytes).map{ floatFromBytes(it) as T}
        Double::class -> unpackList(bytes).map{ doubleFromBytes(it) as T}
        Boolean::class -> unpackList(bytes).map{ booleanFromBytes(it) as T}
        ByteArray::class -> unpackList(bytes).map{it as T}
        else -> error("not supported")
    }
}

/**
 * Takes a map and serializes it into a Bytearray
 * Structure:
 * listOf(
 * keys as List<K>.toByteArray()
 * values as List<V>.toByteArray()
 * ).toByteArray())
 *
 * Will throw error if list sizes don't match
 */
inline fun <reified K, reified V> Map<K, V>.toByteArray(): ByteArray = (listOf(
    this.keys.toList().toByteArray(),
    this.values.toList().toByteArray()))
    .toByteArray()

inline fun <reified K, reified V> mapFromBytes(bytes: ByteArray): Map<K,V>{
    val kvList = unpackList(bytes) // now have a list of two serialized lists
    val keys = listFromBytes<K>(kvList[0])
    val values = listFromBytes<V>(kvList[1])
    require (keys.size == values.size)

    //return if (kvList[0].isEmpty()) emptyMap<K,V>()
    //else
    return   keys.zip(values).toMap()
}

        /**
 * Use this function when you need to cast a generic type and chnage that to a bytearray
 */
inline fun <reified T> T.castedToByteArray(): ByteArray {
    return when (T::class) {
        String::class -> (this as String).toByteArray()
        Long::class -> (this as Long).toByteArray()
        Int::class -> (this as Int).toByteArray()
        Short::class -> (this as Short).toByteArray()
        Char::class -> (this as Char).toByteArray()
        Float::class -> (this as Float).toByteArray()
        Double::class -> (this as Double).toByteArray()
        Boolean::class -> (this as Boolean).toByteArray()
        ByteArray::class -> this as ByteArray
        else -> error("not supported")
    }
}


/**
 * wrap gives a function to wrap primitive types into a bytearray
 * that can be unwrapped by it's respective unWrap function
 *  * Functions are provided to tag every value with a descriptor:
 * Format for non-string: <DescriptorByte><Appropriate amount of bytes for that data>
 * Format for string:
 *  - Descriptor Byte is STRING_BASIC_VALUE + Bytes needed to define length (usualy 1 or 2, unless its a HUGE string
 *  - so n = Descriptor Byte - STRING_BASIC_VALUE
 *  - Next n bytes say how many bytes in the string (lets call that m)
 *  - next m Bytes are the string, encoded in UTF-8
 *  - example: "Hallon!" will be "33-6-H-a-l-l-o-n" (where letters are encoded UTF-8)
 * Format for ByteArray:
 *  - Descriptor Byte is BYTEARRAY + Bytes needed to define length (usualy 1 or 2, unless its a HUGE ByteArray
 *  - so n = Descriptor Byte - BYTEARRAY
 *  - Next n bytes say how many bytes in the ByteArray (lets call that m)
 *  - next m Bytes are the ByteArray
 *  - example: "Hallon!".toBytearray() will be "9-6-H-a-l-l-o-n" (where letters are encoded UTF-8)

 *
 */
fun wrap(string: String): ByteArray {
    // (listOf((STRING_BASIC_VALUE + bytesNeeded(string.length)).toByte()).toByteArray() + string.length.toByteArray()
    //     .takeLast(bytesNeeded(string.length)) + string.toByteArray(Charsets.UTF_8))
    val stringAsBytes = string.toByteArray(Charsets.UTF_8)
    return (listOf(
        (STRING_BASIC_VALUE + bytesNeeded(
            stringAsBytes.size
        )).toByte()
    ).toByteArray() + stringAsBytes.size.toByteArray()
        .takeLast(bytesNeeded(stringAsBytes.size)) + stringAsBytes)
}

fun wrap(byteArray: ByteArray): ByteArray {
    val bytesNeeded =  bytesNeeded(byteArray.size)
    return (listOf((BYTEARRAY + bytesNeeded).toByte()).toByteArray()
            + byteArray.size.toByteArray().takeLast(bytesNeeded)
            + byteArray)
}

/**
 * Unwrapping list needs to be done specifically with
 * @link unwrapList() as Type cannot be determined with generic
 */
inline fun <reified T>wrap(list: List<T>): ByteArray{
    val bytes = wrap(list.toByteArray())
    val bytesNeeded = bytesNeeded(bytes.size)
    return (listOf(LIST + bytesNeeded.toByte()).toByteArray()
            + bytes.size.toByteArray().takeLast(bytesNeeded)
            + bytes)
}

/**
 * Unwrapping Map needs to be done specifically with
 * @link unwrapList() as Type cannot be determined with generic
 */
inline fun <reified K, reified V>wrap(map: Map<K, V>): ByteArray{
    val bytes = wrap(map.toByteArray())
    val bytesNeeded = bytesNeeded(bytes.size)
    return (listOf(MAP + bytesNeeded.toByte()).toByteArray()
            + bytes.size.toByteArray().takeLast(bytesNeeded)
            + bytes)
}


fun wrap(v: Int): ByteArray = listOf(INT).toByteArray() + v.toByteArray()
fun wrap(v: Short): ByteArray = listOf(SHORT).toByteArray() + v.toByteArray()
fun wrap(v: Long): ByteArray = listOf(LONG).toByteArray() + v.toByteArray()
fun wrap(v: Float): ByteArray = listOf(FLOAT).toByteArray() + v.toByteArray()
fun wrap(v: Double): ByteArray = listOf(DOUBLE).toByteArray() + v.toByteArray()
fun wrap(v: Char): ByteArray = listOf(CHAR).toByteArray() + v.toByteArray()
fun wrap(v: Boolean): ByteArray = listOf(BOOLEAN).toByteArray() + v.toByteArray()

/**
 * Unwrapping functions:
 * @param bytes: ByteArray containing exactly one wrapped value
 */
fun unwrapString(bytes: ByteArray): String {
    val size =
        getWrappedStringLength(bytes) // this also performs [require] checks
    val bytesNeeded = bytes[0] - STRING_BASIC_VALUE
    require(bytes.size == size) { "unwrapString(): ByteArray length doesn't match declared length - ${bytes.size} != ${1 + bytesNeeded + size}" } //  descriptor+lengthBytes+String
    require(checkType(bytes) == STRING) { "unwrapString(): Descriptor Byte doesn't say STRING($STRING): ${bytes[0]}" }
    return bytes.drop(1 + bytesNeeded).toByteArray().toString(Charsets.UTF_8)
}

fun unwrapLong(bytes: ByteArray): Long {
    require(checkType(bytes) == LONG) { "unwrapLong(): Descriptor Byte doesn't say LONG($LONG): ${bytes[0]}" }
    return longFromBytes(
        bytes.drop(1).toByteArray()
    )
}

fun unwrapInt(bytes: ByteArray): Int {
    require(checkType(bytes) == INT) { "unwrapInt(): Descriptor Byte doesn't say INT($INT): ${bytes[0]}" }
    return intFromBytes(
        bytes.drop(1).toByteArray()
    )
}

fun unwrapShort(bytes: ByteArray): Short {
    require(checkType(bytes) == SHORT) { "unwrapShort(): Descriptor Byte doesn't say SHORT($SHORT): ${bytes[0]}" }
    return shortFromBytes(
        bytes.drop(1).toByteArray()
    )
}

fun unwrapChar(bytes: ByteArray): Char {
    require(checkType(bytes) == CHAR) { "unwrapChar(): Descriptor Byte doesn't say CHAR($CHAR): ${bytes[0]}" }
    return charFromBytes(
        bytes.drop(1).toByteArray()
    )
}

fun unwrapFloat(bytes: ByteArray): Float {
    require(checkType(bytes) == FLOAT) { "unwrapFloat(): Descriptor Byte doesn't say FLOAT($FLOAT): ${bytes[0]}" }
    return floatFromBytes(
        bytes.drop(1).toByteArray()
    )
}

fun unwrapDouble(bytes: ByteArray): Double {
    require(checkType(bytes) == DOUBLE) { "unwrapDouble(): Descriptor Byte doesn't say DOUBLE($DOUBLE): ${bytes[0]}" }
    return doubleFromBytes(
        bytes.drop(1).toByteArray()
    )
}

fun unwrapBoolean(bytes: ByteArray): Boolean {
    require(checkType(bytes) == BOOLEAN) { "unwrapBoolean(): Descriptor Byte doesn't say BOOLEAN($BOOLEAN): ${bytes[0]}" }
    return booleanFromBytes(
        bytes.drop(1).toByteArray()
    )
}

fun unwrapByteArray(bytes: ByteArray): ByteArray {
    val size =
        getWrappedByteArrayLength(bytes)
    require(checkType(bytes) == BYTEARRAY) { "unwrapByteArray(): Descriptor Byte doesn't say BYTEARRAY($BYTEARRAY): ${bytes[0]}" }
    val bytesNeeded = bytes[0] - BYTEARRAY_BASIC_VAL
    require(bytes.size == size) { "unwrapString(): ByteArray length doesn't match declared length - ${bytes.size} != ${1 + bytesNeeded + size}" } //  descriptor+lengthBytes+String
    return bytes.drop(1 + bytesNeeded).toByteArray()
}

inline fun <reified T>unwrapList(bytes:ByteArray): List<T>{
    require(checkType(bytes) == LIST) { "unwrapBoolean(): Descriptor Byte doesn't say LIST($LIST): ${bytes[0]}" }
    val size = getWrappedListLength(bytes)
    val bytesNeeded = bytes[0] - LIST_BASIC_VALUE
    require(bytes.size == size) { "unwrapList(): ByteArray length doesn't match declared length - ${bytes.size} != ${1 + bytesNeeded + size}" } //  descriptor+lengthBytes+String
    return listFromBytes(bytes.drop(1 + bytesNeeded).toByteArray())
}

inline fun <reified K, reified V>unwrapMap(bytes:ByteArray): Map<K, V>{
    require(checkType(bytes) == MAP) { "unwrapBoolean(): Descriptor Byte doesn't say LIST($LIST): ${bytes[0]}" }
    val size = getWrappedListLength(bytes)
    val bytesNeeded = bytes[0] - MAP_BASIC_VALUE
    require(bytes.size == size) { "unwrapMap(): ByteArray length doesn't match declared length - ${bytes.size} != ${1 + bytesNeeded + size}" } //  descriptor+lengthBytes+String
    return mapFromBytes<K, V>(bytes.drop(1 + bytesNeeded).toByteArray())
}

inline fun <reified T> unwrap(bytes: ByteArray): T {
    return when (T::class) {
        String::class -> unwrapString(bytes) as T
        Long::class -> unwrapLong(bytes) as T
        Int::class -> unwrapInt(bytes) as T
        Short::class -> unwrapShort(bytes) as T
        Char::class -> unwrapChar(bytes) as T
        Float::class -> unwrapFloat(bytes) as T
        Double::class -> unwrapDouble(bytes) as T
        Boolean::class -> unwrapBoolean(bytes) as T
        ByteArray::class -> unwrapByteArray(bytes) as T
        else -> error("not supported")
    }
}


/**
 * Check sanity of a Bytearray to be unwrapped
 */
fun checkType(bytes: ByteArray): Byte {
    require(bytes.isNotEmpty()) { "checkType(): Empty ByteArray" }
    val type = bytes[0]
    return when {
        type == LONG -> {
            require(bytes.size == SIZE_WRAPPED_LONG) { "checkType(): Descriptor says LONG but size doesn't match" }
            type
        }
        type == INT -> {
            require(bytes.size == SIZE_WRAPPED_INT) { "checkType(): Descriptor says INT but size doesn't match" }
            type
        }
        type == SHORT -> {
            require(bytes.size == SIZE_WRAPPED_SHORT) { "checkType(): Descriptor says SHORT but size doesn't match" }
            type
        }
        type == CHAR -> {
            require(bytes.size == SIZE_WRAPPED_CHAR) { "checkType(): Descriptor says CHAR but size doesn't match" }
            type
        }
        type == FLOAT -> {
            require(bytes.size == SIZE_WRAPPED_FLOAT) { "checkType(): Descriptor says FLOAT but size doesn't match" }
            type
        }
        type == DOUBLE -> {
            require(bytes.size == SIZE_WRAPPED_DOUBLE) { "checkType(): Descriptor says DOUBLE but size doesn't match" }
            type
        }
        type == BOOLEAN -> {
            require(bytes.size == SIZE_WRAPPED_BOOLEAN) { "checkType(): Descriptor says BOOLEAN but size doesn't match" }
            type
        }
        type in (MAP..MAP+4) -> {
            require(bytes.size > 5) { "checkType(): Type says MAP but not enough Bytes for that" } // Needs a descriptor and a size byte (can be empty string)
            MAP
        }
        type in (STRING..STRING+4) -> {
            require(bytes.size > 1) { "checkType(): Type says STRING but not enough Bytes for that" } // Needs a descriptor and a size byte (can be empty string)
            STRING
        }
        type in (LIST..LIST+4) -> {
            require(bytes.size > 1) { "checkType(): Type says LIST but not enough Bytes for that" } // Needs a descriptor and a size byte (can be empty string)
            LIST
        }
        type in (BYTEARRAY..BYTEARRAY + 4) -> {
            require(bytes.size > 1) { "checkType(): Type says BYTEARRAY but not enough Bytes for that" } // Needs a descriptor and a size byte (can be empty string)
            //require (checkWrapLength(bytes))                   { "checkType(): Descriptor says ${checkWrapLength(bytes)} bytes but actual size is ${bytes.size}"}
            BYTEARRAY
        }

        else -> error("invalid descriptor Byte: $type")
    }
}

private fun checkType(bytes: List<Byte>) =
    checkType(bytes.toByteArray())


/**
 * Will return the type that is at the front of a serialized ByteArray
 * eg. LONG or STRING etc
 * @param bb: a stream of wraps
 * @return the type of wrap at the start of the stream
 */
fun nextType(bytes: ByteArray): Byte = when {
    bytes[0] in (LONG..BOOLEAN) -> bytes[0]
    bytes[0] > STRING -> STRING
    else -> error("nextType(): invalid descriptor Byte: ${bytes[0]}")
}

/**
 * Will return the first wrapped value (including wrap) from a stream of Bytes
 * @param bytes: A stream of bytes, starting with a wrapped value
 * @return the wrapped value as ByteArray
 */
fun nextWrap(bytes: ByteArray): ByteArray {
    return when (bytes[0]) {
        LONG -> bytes.slice(0 until SIZE_WRAPPED_LONG).toByteArray()
        INT -> bytes.slice(0 until SIZE_WRAPPED_INT).toByteArray()
        SHORT -> bytes.slice(0 until SIZE_WRAPPED_SHORT).toByteArray()
        CHAR -> bytes.slice(0 until SIZE_WRAPPED_CHAR).toByteArray()
        FLOAT -> bytes.slice(0 until SIZE_WRAPPED_FLOAT).toByteArray()
        DOUBLE -> bytes.slice(0 until SIZE_WRAPPED_DOUBLE).toByteArray()
        BOOLEAN -> bytes.slice(0 until SIZE_WRAPPED_BOOLEAN).toByteArray()
        in (BYTEARRAY..(BYTEARRAY + 4)) -> bytes.slice(
            0 until getWrappedByteArrayLength(
                bytes
            )
        ).toByteArray()
        in (STRING..Int.MAX_VALUE) -> bytes.slice(
            0 until getWrappedStringLength(
                bytes
            )
        ).toByteArray()
        else -> error("nextWrap(): WIERD ERROR should have failed eralier - type is ${bytes[0]}")
    }
}

fun nextWrap(bytes: List<Byte>): ByteArray {
    return when (bytes[0]) {
        LONG -> bytes.slice(0 until SIZE_WRAPPED_LONG).toByteArray()
        INT -> bytes.slice(0 until SIZE_WRAPPED_INT).toByteArray()
        SHORT -> bytes.slice(0 until SIZE_WRAPPED_SHORT).toByteArray()
        CHAR -> bytes.slice(0 until SIZE_WRAPPED_CHAR).toByteArray()
        FLOAT -> bytes.slice(0 until SIZE_WRAPPED_FLOAT).toByteArray()
        DOUBLE -> bytes.slice(0 until SIZE_WRAPPED_DOUBLE).toByteArray()
        BOOLEAN -> bytes.slice(0 until SIZE_WRAPPED_BOOLEAN).toByteArray()
        in (BYTEARRAY..(BYTEARRAY + 4)) -> bytes.slice(
            0 until getWrappedByteArrayLength(
                bytes
            )
        ).toByteArray()
        in (STRING..Int.MAX_VALUE) -> bytes.slice(
            0 until getWrappedStringLength(
                bytes
            )
        ).toByteArray()
        else -> error("nextWrap(): WIERD ERROR should have failed eralier - type is ${bytes[0]}")
    }
}


/**
 * Gets length of a wrapped String in Bytes (including wrapper)
 * this is the amount of Bytes to pass to unwrapString
 * @param bytes: a stream of Bytes starting with a wrapped string
 * @return the length of the string according to the descriptor bytes
 */
private fun getWrappedStringLength(bytes: ByteArray): Int {
    require(checkType(bytes) == STRING)
    val bytesNeeded = bytes[0] - STRING_BASIC_VALUE
    return intFromBytes(
        ByteArray(4 - bytesNeeded) + bytes.slice(
            1..bytesNeeded
        ).toByteArray()
    ) + 1 + bytesNeeded // can use .. range because starting at 1
}

private fun getWrappedStringLength(bytes: List<Byte>): Int {
    require(checkType(bytes) == STRING)
    val bytesNeeded = bytes[0] - STRING_BASIC_VALUE
    return intFromBytes(
        ByteArray(4 - bytesNeeded) + bytes.slice(
            1..bytesNeeded
        ).toByteArray()
    ) + 1 + bytesNeeded // can use .. range because starting at 1
}

private fun getWrappedByteArrayLength(bytes: ByteArray): Int {
    require(checkType(bytes) == BYTEARRAY)
    val bytesNeeded = bytes[0] - BYTEARRAY_BASIC_VAL
    return intFromBytes(
        ByteArray(4 - bytesNeeded) + bytes.slice(
            1..bytesNeeded
        ).toByteArray()
    ) + 1 + bytesNeeded // can use .. range because starting at 1
}

private fun getWrappedByteArrayLength(bytes: List<Byte>): Int {
    require(checkType(bytes) == BYTEARRAY)
    val bytesNeeded = bytes[0] - BYTEARRAY_BASIC_VAL
    return intFromBytes(
        ByteArray(4 - bytesNeeded) + bytes.slice(
            1..bytesNeeded
        ).toByteArray()
    ) + 1 + bytesNeeded // can use .. range because starting at 1
}
fun getWrappedListLength(bytes: ByteArray): Int {
    require(checkType(bytes) == LIST)
    val bytesNeeded = bytes[0] - LIST_BASIC_VALUE
    return intFromBytes(
        ByteArray(4 - bytesNeeded) + bytes.slice(
            1..bytesNeeded
        ).toByteArray()
    ) + 1 + bytesNeeded // can use .. range because starting at 1
}

fun getWrappedListLength(bytes: List<Byte>): Int {
    require(checkType(bytes) == BYTEARRAY)
    val bytesNeeded = bytes[0] - LIST_BASIC_VALUE
    return intFromBytes(
        ByteArray(4 - bytesNeeded) + bytes.slice(
            1..bytesNeeded
        ).toByteArray()
    ) + 1 + bytesNeeded // can use .. range because starting at 1
}

/**
 * Check wrap length for array types (String or ByteArray)
 */
private fun checkWrapLength(bytes: ByteArray): Boolean {
    val type = bytes[0]
    return when {
        type > STRING -> {
            val stringLength =
                getWrappedStringLength(bytes)
            val bytesNeeded = type - STRING_BASIC_VALUE
            bytes.size == stringLength + bytesNeeded + 1
        }
        type in (BYTEARRAY..BYTEARRAY + 4) -> {
            val byteArrayLength =
                getWrappedByteArrayLength(
                    bytes
                )
            val bytesNeeded = type - BYTEARRAY_BASIC_VAL
            bytes.size == byteArrayLength + bytesNeeded + 1
        }
        else -> error("Unsupported. checkWrapLength() only works for wrapped String and ByteArray")
    }
}


/**
 * Helper function for wrapping strings
 */
fun bytesNeeded(value: Int) = when (value) {
    in (0..255) -> 1
    in (255..65565) -> 2
    in (65535..16777216) -> 3
    in (16777216..Int.MAX_VALUE) -> 4
    else -> error("$value is not a valid length for a String")
}

/**
 * Helper function for packing Lists
 */

fun packList(series: List<ByteArray>): ByteArray =
    series.map { it.size.toByteArray().toList() + it.toList() }.flatten().toByteArray()

fun unpackList(packed: ByteArray): List<ByteArray> {
    val list = mutableListOf<ByteArray>()
    var index = 0
    while (packed.size > index + 3) {
        val size =
            intFromBytes(packed.slice(index until index + 4))
        //if (size == 0) break // catch padding zeroes
        list.add(packed.slice(index + 4 until (index + size + 4)).toByteArray())
        index += size + 4
    }
    return list
}

