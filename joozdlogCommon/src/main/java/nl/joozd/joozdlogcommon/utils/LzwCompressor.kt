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


package nl.joozd.joozdlogcommon.utils

import nl.joozd.joozdlogcommon.serializing.shortFromBytes
import nl.joozd.joozdlogcommon.serializing.toByteArray
import java.io.IOException
import java.io.InputStream
import java.io.OutputStream

object LzwCompressor {
    const val TABLE_SIZE = Short.MAX_VALUE

    fun compress(input: ByteArray): ByteArray {
        var position: Int = 0
        val dataOut = mutableListOf<Short>()

        //populate map with (0..255)
        //map: Key = string to search, value is value to place in output (list of Byte)
        val table = (0..255).map{listOf(it.toByte()) to it.toShort()}.toMap().toMutableMap()

        compressionLoop@while (position in input.indices ){
            var l = 1
            //keep expanding list untill it is no longer found
            while (input.slice(position until position+l) in table.keys){
                l++
                if (position+l >=input.size){
                    // In this case, remaining inputData is known and it's index can be written to output, encoding is done
                    dataOut.add(table[input.slice(position until position+l)] ?: error("LZW compression error 2"))
                    break@compressionLoop
                }
            }
            //we now have the length of the longest list in dictionary + 1
            // if there is room in the table, add list+1 to it
            if (table.size < Short.MAX_VALUE)
                table[input.slice(position until position+l)] = table.size.toShort()

            dataOut.add(table[input.slice(position until position+l)]?: error("LZW compression error 1"))

            //remove l+1 items from beginning of input
            position += l
        }


        //throwing a lot of formats around but what this does:
        // dataOut is a list of Shorts. These shorts are broken into two bytes, and all those bytes are put into a ByteArray.
        return dataOut.map{it.toByteArray().toList()}.flatten().toByteArray()
    }

    /**
     * compressToBufferedOutputStreamStream will compress a ByteArray
     * and write it's result incrementally to an OutputStream
     */
    fun compressToOutputStreamStream(output: OutputStream, input: ByteArray){
        var position: Int = 0
        val table = (0..255).map{listOf(it.toByte()) to it.toShort()}.toMap().toMutableMap()
        compressionLoop@while (position in input.indices ){
            var l = 1
            //keep expanding list untill it is no longer found
            while (input.slice(position until position+l) in table.keys){
                l++
                if (position+l >=input.size){
                    // In this case, remaining inputData is known and it's index can be written to output, encoding is done
                    output.write(table[input.slice(position until position+l)]?.toByteArray() ?: error("LZW compression error 3"))
                    break@compressionLoop
                }
            }
            //we now have the length of the longest list in dictionary + 1
            // if there is room in the table, add list+1 to it
            if (table.size < Short.MAX_VALUE)
                table[input.slice(position until position+l)] = table.size.toShort()

            output.write(table[input.slice(position until position+l)]?.toByteArray()?: error("LZW compression error 1"))

            position += l
        }
    }

    fun decompress(input: ByteArray): ByteArray {
        //change _input_ into list of shorts
        val dataIn = input.toList().chunked(2).map{
            shortFromBytes(
                it.toByteArray()
            )
        }
        val dataOut = mutableListOf<Byte>()

        //populate map with (0..255)
        //map: Key = value is Short found, value = List of Bytes to put in output
        val table = (0..255).map{it.toShort() to listOf(it.toByte())}.toMap().toMutableMap()

        //put found index into output
        dataIn.forEachIndexed { index, descriptor ->
            println("Descriptor: $descriptor\n")
            dataOut.addAll(table[descriptor] ?: error("LZW Decompression error 1: Bad index"))
            println(table[descriptor])
            println("\n")

            //add next value to table
            if (index < dataIn.size -1) {
                // both values cannot be null as they are checked earlier
                table[table.size.toShort()] =
                    if (dataIn[index+1] in table.keys) table[descriptor]!!+table[dataIn[index+1]]!![0]
                    else table[descriptor]!!+table[descriptor]!![0]
                println("Table size now ${table.size}")
            }
        }
        return dataOut.toByteArray()
    }

    /**
     * Decompress a stream as it is coming in, so that it can be returned immediately as a ByteArray upon completion
     */
    fun decompressFromInputStream(inputStream: InputStream, size: Int, bufferSize: Int = 8192): ByteArray{
        val buffer = ByteArray(bufferSize)
        var processedBytes = 0

        val compresedMessage = mutableListOf<Byte>()
        val uncompresedMessage = mutableListOf<Byte>()

        val table = (0..255).map{it.toShort() to listOf(it.toByte())}.toMap().toMutableMap()

        //read and unpack buffers until correct amount of bytes reached or fail trying
        while (processedBytes < size){
            val b = inputStream.read(buffer)
            if (b<0) throw IOException("Stream too short: expect $size, got ${processedBytes+b}")
            compresedMessage.addAll(buffer.take(b))
            //make a list of shorts from as many pairs of Bytes as you can find (leave last one if odd amount is read)
            val shorts = (if (compresedMessage.size.isEven()) compresedMessage.toList() else compresedMessage.dropLast(1)).chunked(2).map{
                shortFromBytes(it.toByteArray())
            }

            //clear out buffer, leave last byte if it's an odd one
            if (compresedMessage.size.isEven()) compresedMessage.clear()
            else {
                val lastByte = compresedMessage.last()
                compresedMessage.clear()
                compresedMessage.add(lastByte)
            }
            //now we have a list of Shorts that we can add to output:
            shorts.forEachIndexed { index, descriptor ->
                println("Descriptor: $descriptor\n")
                uncompresedMessage.addAll(table[descriptor] ?: error("LZW Decompression error 1: Bad index"))
                println(table[descriptor])
                println("\n")

                //add next value to table
                if (index < shorts.size -1) {
                    // both values cannot be null as they are checked earlier
                    table[table.size.toShort()] =
                        if (shorts[index+1] in table.keys) table[descriptor]!!+table[shorts[index+1]]!![0]
                        else table[descriptor]!!+table[descriptor]!![0]
                }
            }


            processedBytes += b
        }
        return uncompresedMessage.toByteArray()

    }

    private fun <K, V> Map<K, V>.reversedMap() = HashMap<V, K>().also { newMap ->
        entries.forEach { newMap[it.value] = it.key }
    }

    private fun Int.isEven() = this%2 == 0

}