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

package nl.joozd.logbookapp

import androidx.annotation.UiThread
import nl.joozd.joozdlogcommon.serializing.listFromBytes
import nl.joozd.joozdlogcommon.serializing.mapFromBytes
import nl.joozd.joozdlogcommon.serializing.packList
import nl.joozd.joozdlogcommon.serializing.toByteArray

import org.junit.Assert.*
import org.junit.Test

/**
 * Example local unit test, which will execute on the development machine (host).
 *
 * See [testing documentation](http://d.android.com/tools/testing).
 */
class ExampleUnitTest {
    /*
    @Test
    fun addition_isCorrect() {
        assertEquals(4, 2 + 2)
    }
    */

//    @Test
    fun stringSerialize(){
        val string = ""
        val bytes = string.toByteArray(Charsets.UTF_8)
        bytes.forEach { print("$it, ") }
        println("${bytes.size}")
        val deserialized = bytes.toString(Charsets.UTF_8)
        assertEquals(string, deserialized)
    }

 //   @Test
    fun packListTest(){
        val bytes = packList(listOf(ByteArray(0)))
        print("bytes size: ${bytes.size}\n")
        bytes.forEach { print("$it, ")}
        true
    }

 //   @Test
    fun serializeListOfStringsAndBack(){
        val testList = listOf ("hoi", "fiets", "banaan", "123", "", "\\n", "\n")
        val bytes = testList.toByteArray()
        val deserialized: List<String> = listFromBytes(bytes)
        println(deserialized)
        assertEquals(testList, deserialized)
    }

 //   @Test
    fun testMapSerializing(){
        val map = listOf(1,2,3,4).zip(listOf("boom", "roos", "vis", "vuur")).toMap()
        val bytes = map.toByteArray()
        val deserialized = mapFromBytes<Int, String>(bytes)
        println(deserialized)
        assertEquals(deserialized, map)
    }

    @Test
    fun testMediator() {
        val test = MyChild()
        var value: String? = null
        test.myObservable.observeForever {
            value = it
        }
        assertEquals("3", value)
    }

}
