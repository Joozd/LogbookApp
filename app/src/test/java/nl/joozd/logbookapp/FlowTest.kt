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

import kotlinx.coroutines.flow.flow
import org.junit.Test
import kotlin.random.Random

class FlowTest {
    @Test
    fun testFlatMap(){

    }

    fun makeFlow(i: Int, s: String) = flow {
        emit("1. Doing something without using $i and $s")
        Array(10000){ Random.nextDouble() }.sorted()
        emit("2. Doing something without $i and $s")
        Array(10000){ Random.nextDouble() }.sorted()
        emit("3. Doing something without $i and $s")
    }
}