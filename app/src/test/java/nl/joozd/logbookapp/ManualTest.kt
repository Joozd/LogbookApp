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

package nl.joozd.logbookapp

import org.junit.Test
import org.junit.Assert.assertEquals

class ManualTest {
    @Test
    fun manualTest(){
        val ll = listOf("one", "two", "three")
        val ll2 = listOf("one", "four", "three")
        val lhm = LinkedHashMap<Int, String>()
        lhm[1] = ll[0]
        lhm[2] = ll[1]
        lhm[3] = ll[2]
        assertEquals(ll, lhm.values.toList())
        lhm[2] = ll2[1]
        assertEquals(ll2, lhm.values.toList())
    }

}