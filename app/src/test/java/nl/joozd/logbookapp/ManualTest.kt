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
        assertEquals("KL1235", increaseFlightnumberByOne("KL1234"))
        assertEquals("HB902D", increaseFlightnumberByOne("HB901D"))
        assertEquals("HV1000", increaseFlightnumberByOne("HV999"))
    }

    // KL1234 becomes KL1235, HB901D becomes HB902D and HV999 becomes HV1000
    private fun increaseFlightnumberByOne(fn: String): String {
        val regex = """\d+""".toRegex()
        val lastHit = regex.findAll(fn).lastOrNull()?.value ?: return fn
        return fn.replace(lastHit, (lastHit.toInt() + 1).toString())
    }
}